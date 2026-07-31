#!/usr/bin/env python3
"""Generator Flyway migracija za zone (opstine, mesne zajednice, naseljena mesta).

Pipeline:
  discover  Overpass  -> spisak OSM relacija po admin_level-u
  geometry  LocationIQ -> sklopljen poligon po relation id-u (kesira se na disk)
  stage     PostGIS   -> staging baza, ST_CoverageSimplify po nivou
  emit      SQL       -> Flyway migracija

Geometrija se dohvata po EKSPLICITNOM relation id-u, nikad pretragom po imenu:
pretraga je za 10 od 17 opstina vratila poligon naseljenog mesta umesto opstine
(Obrenovac 9.3 km2 umesto 411), sto je i bio uzrok bug-a koji V36 popravlja.

Uproscavanje ide kroz ST_CoverageSimplify, ne ST_SimplifyPreserveTopology: per-feature
Douglas-Peucker pomera svaku stranu zajednicke granice nezavisno, pa pravi procepe i
preklapanja (na zatecenih 17 zona: 11 sa nevalidnim coverage ivicama).
"""

import argparse
import json
import os
import re
import subprocess
import sys
import time
import unicodedata
import urllib.parse
import urllib.request

BELGRADE_BBOX = "44.30,20.00,45.00,20.95"
OVERPASS_ENDPOINTS = [
    "https://overpass.kumi.systems/api/interpreter",
    "https://overpass-api.de/api/interpreter",
]
LOCATIONIQ_BASE = "https://api.locationiq.com/v1"
LOCATIONIQ_KEY = os.environ.get("LOCATIONIQ_API_KEY", "pk.b3e4b3a16f59a78cc48e19275ede1747")
LOCATIONIQ_DELAY_SECONDS = 0.6
USER_AGENT = "lost-and-found-zone-builder/1.0 (master thesis; contact via repo)"

MUNICIPALITY_PREFIX = "Gradska opština"
# Tolerancija uproscavanja po nivou, u stepenima. Nivo 1 su opstine medijane ~157 km2,
# gde je 33 m nevidljivo i stedi pola megabajta; nivo 2 ide na 11 m jer je medijana mesne
# zajednice ~1.2 km2 (sirina ~1 km), a najsitnija jedinica 0.11 km2.
SIMPLIFY_TOLERANCE_DEGREES = {1: 0.0003, 2: 0.0001}
STAGING_DATABASE = "zone_build"

CACHE_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "cache")

MUNICIPALITY_CODES = {
    "Barajevo": "BG-BAR",
    "Voždovac": "BG-VOZ",
    "Vračar": "BG-VRA",
    "Grocka": "BG-GRO",
    "Zvezdara": "BG-ZVE",
    "Zemun": "BG-ZEM",
    "Lazarevac": "BG-LAZ",
    "Mladenovac": "BG-MLA",
    "Novi Beograd": "BG-NBG",
    "Obrenovac": "BG-OBR",
    "Palilula": "BG-PAL",
    "Rakovica": "BG-RAK",
    "Savski venac": "BG-SAV",
    "Sopot": "BG-SOP",
    "Stari grad": "BG-STG",
    "Surčin": "BG-SUR",
    "Čukarica": "BG-CUK",
}

TRANSLITERATION = {
    "č": "c", "ć": "c", "š": "s", "ž": "z", "đ": "dj",
    "Č": "C", "Ć": "C", "Š": "S", "Ž": "Z", "Đ": "DJ",
}


def log(message):
    print(message, file=sys.stderr, flush=True)


def cache_path(name):
    os.makedirs(CACHE_DIR, exist_ok=True)
    return os.path.join(CACHE_DIR, name)


def overpass(query, attempts=3):
    payload = query.encode("utf-8")
    last_error = None
    for attempt in range(attempts):
        for endpoint in OVERPASS_ENDPOINTS:
            try:
                request = urllib.request.Request(
                    endpoint, data=payload,
                    headers={"Content-Type": "application/x-www-form-urlencoded",
                             "User-Agent": USER_AGENT})
                with urllib.request.urlopen(request, timeout=300) as response:
                    body = response.read().decode("utf-8")
                if body.lstrip().startswith("{"):
                    return json.loads(body)
                last_error = "non-json response (rate limit?)"
            except Exception as error:
                last_error = error
            log(f"  overpass fail on {endpoint}: {last_error}")
        time.sleep(60 * (attempt + 1))
    raise RuntimeError(f"overpass unavailable: {last_error}")


def slugify(name):
    stripped = name
    for source, target in TRANSLITERATION.items():
        stripped = stripped.replace(source, target)
    stripped = unicodedata.normalize("NFD", stripped)
    stripped = "".join(character for character in stripped
                       if unicodedata.category(character) != "Mn")
    stripped = re.sub(r"[^A-Za-z0-9]+", "-", stripped).strip("-").upper()
    return stripped[:24].rstrip("-")


def normalize_unit_name(raw_name):
    name = raw_name.strip()
    if name.startswith("МЗ "):
        name = name[3:]
    if name.startswith("MZ "):
        name = name[3:]
    return name.strip()


def discover():
    log("discover: admin_level 8 (opstine)")
    municipalities = []
    result = overpass(
        f'[out:json][timeout:180];'
        f'relation["boundary"="administrative"]["admin_level"="8"]({BELGRADE_BBOX});'
        f"out tags;"
    )
    for element in result["elements"]:
        tags = element["tags"]
        name = tags.get("name:sr-Latn") or tags.get("name", "")
        if not name.startswith(MUNICIPALITY_PREFIX):
            continue
        bare = name[len(MUNICIPALITY_PREFIX):].strip()
        code = MUNICIPALITY_CODES.get(bare)
        if code is None:
            raise RuntimeError(f"nepoznata opstina iz OSM-a: {bare!r}")
        municipalities.append({
            "osm_relation_id": element["id"],
            "code": code,
            "name": bare,
            "city": "Beograd",
            "level": 1,
        })
    if len(municipalities) != 17:
        raise RuntimeError(f"ocekivano 17 opstina, dobijeno {len(municipalities)}")

    # Podrucje se gradi iz 17 opstina po id-u, a ne iz bboxa: bbox oko Beograda zahvata i
    # sela iz Rume, Stare Pazove i Kovina. Upit je tezak, pa kumi.systems obicno vrati 504
    # i posao preuzme overpass-api.de — zato oba endpointa i retry sa backoff-om.
    log("discover: admin_level 9/10 unutar podrucja 17 opstina")
    municipality_ids = ",".join(str(entry["osm_relation_id"]) for entry in municipalities)
    units = []
    seen = set()
    result = overpass(
        f"[out:json][timeout:300];"
        f"rel(id:{municipality_ids});"
        f"map_to_area->.belgrade;"
        f'relation["boundary"="administrative"]["admin_level"~"^(9|10)$"](area.belgrade);'
        f"out tags;"
    )
    for element in result["elements"]:
        tags = element["tags"]
        name = normalize_unit_name(tags.get("name:sr-Latn") or tags.get("name", ""))
        if not name or element["id"] in seen:
            continue
        seen.add(element["id"])
        units.append({
            "osm_relation_id": element["id"],
            "name": name,
            "city": "Beograd",
            "level": 2,
            "admin_level": int(tags["admin_level"]),
        })

    payload = {"municipalities": municipalities, "units": units}
    with open(cache_path("relations.json"), "w", encoding="utf-8") as handle:
        json.dump(payload, handle, ensure_ascii=False, indent=2)
    log(f"discover: {len(municipalities)} opstina, {len(units)} jedinica nivoa 2")
    return payload


def fetch_geometry(osm_relation_id):
    target = cache_path(f"geom-{osm_relation_id}.json")
    if os.path.exists(target):
        with open(target, encoding="utf-8") as handle:
            return json.load(handle)

    url = (f"{LOCATIONIQ_BASE}/lookup?key={LOCATIONIQ_KEY}"
           f"&osm_ids=R{osm_relation_id}&format=json&polygon_geojson=1")

    # LocationIQ free tier ima i minutnu kvotu pored 2/s, pa duzi niz zahteva pocne da
    # vraca 429 i oporavi se tek posle pauze. Backoff je zato u desetinama sekundi.
    payload = None
    for attempt in range(6):
        try:
            request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
            with urllib.request.urlopen(request, timeout=60) as response:
                payload = json.loads(response.read().decode("utf-8"))
            break
        except urllib.error.HTTPError as error:
            if error.code != 429 or attempt == 5:
                raise
            time.sleep(20 * (attempt + 1))
    record = payload[0] if isinstance(payload, list) else payload
    geometry = record.get("geojson")
    if geometry is None:
        raise RuntimeError(f"nema geojson za relaciju {osm_relation_id}")
    with open(target, "w", encoding="utf-8") as handle:
        json.dump(geometry, handle)
    time.sleep(LOCATIONIQ_DELAY_SECONDS)
    return geometry


def geometry(payload):
    everything = payload["municipalities"] + payload["units"]
    for index, entry in enumerate(everything, start=1):
        try:
            fetch_geometry(entry["osm_relation_id"])
        except Exception as error:
            log(f"  [{index}/{len(everything)}] FAIL {entry['name']}: {error}")
            entry["geometry_failed"] = True
            continue
        if index % 20 == 0:
            log(f"  [{index}/{len(everything)}]")
    failed = [entry for entry in everything if entry.get("geometry_failed")]
    log(f"geometry: gotovo, {len(failed)} neuspelih")
    return failed


def psql(sql, database=STAGING_DATABASE, tuples_only=True):
    command = ["psql", "-h", "localhost", "-U", "postgres", "-d", database, "-v", "ON_ERROR_STOP=1"]
    if tuples_only:
        command += ["-tA"]
    command += ["-c", sql]
    environment = dict(os.environ, PGPASSWORD=os.environ.get("DB_PASSWORD", "admin"))
    result = subprocess.run(command, capture_output=True, text=True, env=environment)
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip())
    return result.stdout.strip()


def psql_file(path, database=STAGING_DATABASE):
    environment = dict(os.environ, PGPASSWORD=os.environ.get("DB_PASSWORD", "admin"))
    result = subprocess.run(
        ["psql", "-h", "localhost", "-U", "postgres", "-d", database,
         "-v", "ON_ERROR_STOP=1", "-q", "-f", path],
        capture_output=True, text=True, env=environment)
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip()[:2000])
    return result.stdout


def stage(payload):
    environment = dict(os.environ, PGPASSWORD=os.environ.get("DB_PASSWORD", "admin"))
    subprocess.run(
        ["psql", "-h", "localhost", "-U", "postgres", "-d", "postgres",
         "-c", f"CREATE DATABASE \"{STAGING_DATABASE}\""],
        capture_output=True, text=True, env=environment)
    psql("CREATE EXTENSION IF NOT EXISTS postgis")
    psql("DROP TABLE IF EXISTS staging_zones")
    psql("""
        CREATE TABLE staging_zones (
            osm_relation_id BIGINT PRIMARY KEY,
            code            VARCHAR(40),
            name            VARCHAR(100) NOT NULL,
            city            VARCHAR(100) NOT NULL,
            level           SMALLINT NOT NULL,
            admin_level     SMALLINT,
            raw             geometry(Geometry, 4326) NOT NULL,
            boundary        geometry(MultiPolygon, 4326)
        )
    """)

    # Geometrija ide kroz fajl, ne kroz psql -c: pojedinacni poligon zna da bude preko
    # 100 KB, sto bi probilo limit duzine komandne linije.
    everything = [entry for entry in payload["municipalities"] + payload["units"]
                  if not entry.get("geometry_failed")]
    statements = []
    for entry in everything:
        with open(cache_path(f"geom-{entry['osm_relation_id']}.json"), encoding="utf-8") as handle:
            raw = handle.read()
        statements.append(
            "INSERT INTO staging_zones "
            "(osm_relation_id, code, name, city, level, admin_level, raw) VALUES ("
            f"{entry['osm_relation_id']}, "
            f"{quote(entry.get('code'))}, {quote(entry['name'])}, {quote(entry['city'])}, "
            f"{entry['level']}, {entry.get('admin_level') or 'NULL'}, "
            f"ST_Multi(ST_CollectionExtract(ST_MakeValid(ST_GeomFromGeoJSON({quote(raw)})), 3)));"
        )
    load_script = cache_path("staging-load.sql")
    with open(load_script, "w", encoding="utf-8") as handle:
        handle.write("\n".join(statements))
    psql_file(load_script)
    log(f"stage: uneto {len(everything)} redova")
    prune_redundant_settlements()

    for level in (1, 2):
        psql(f"""
            WITH simplified AS (
                SELECT osm_relation_id,
                       ST_CoverageSimplify(raw, {SIMPLIFY_TOLERANCE_DEGREES[level]}) OVER () AS geom
                FROM staging_zones WHERE level = {level}
            )
            UPDATE staging_zones s
            SET boundary = ST_Multi(ST_CollectionExtract(ST_MakeValid(simplified.geom), 3))
            FROM simplified
            WHERE simplified.osm_relation_id = s.osm_relation_id
        """)
        invalid = psql(f"""
            WITH edges AS (
                SELECT ST_CoverageInvalidEdges(boundary, 0.0) OVER () AS bad
                FROM staging_zones WHERE level = {level}
            )
            SELECT count(*) FROM edges WHERE bad IS NOT NULL
        """)
        total, points = psql(
            f"SELECT count(*), coalesce(sum(ST_NPoints(boundary)),0) "
            f"FROM staging_zones WHERE level = {level}").split("|")
        log(f"stage: nivo {level} -> {total} zona, {points} tacaka, {invalid} nevalidnih ivica")

    assign_codes()


def prune_redundant_settlements():
    """Zadrzava naseljeno mesto (admin_level 9) samo tamo gde mesna zajednica ne postoji.

    U urbanom jezgru OSM ima oba sloja preko istog tla: "Beograd (Vracar)" na nivou 9
    pokriva celu opstinu i preklapa se sa svim mesnim zajednicama u njoj. Oba na level = 2
    znace da level 2 nije disjunktan, pa razresavanje zone postaje nedeterministicko.
    """
    removed = psql("""
        WITH mz AS (
            SELECT ST_Union(raw) AS geom FROM staging_zones WHERE admin_level = 10
        )
        DELETE FROM staging_zones s
        USING mz
        WHERE s.admin_level = 9
          AND ST_Area(ST_Intersection(s.raw, mz.geom)) > 0.5 * ST_Area(s.raw)
        RETURNING s.name
    """)
    names = [line for line in removed.splitlines()
             if line and not re.fullmatch(r"DELETE \d+", line)]
    log(f"stage: uklonjeno {len(names)} naselja pokrivenih mesnim zajednicama")
    if names:
        log(f"        {', '.join(sorted(names)[:12])}{' ...' if len(names) > 12 else ''}")

    # Jedinica koja je prakticno jednaka svojoj opstini ne nosi nikakvu informaciju: OSM za
    # Vracar i Stari grad ima samo naseljeno mesto "Beograd (Vracar)" koje pokriva celu
    # opstinu, bez ijedne mesne zajednice. Takva zona bi se prikazivala kao
    # "Beograd (Vracar), Vracar" i lazno bi obecavala finiju granulaciju. Bolje je pustiti
    # te tacke da padnu na opstinu kroz tier 2 u zone_resolve.
    redundant = psql("""
        WITH parent AS (
            SELECT child.osm_relation_id, child.name,
                   ST_Area(child.raw) / ST_Area(municipality.raw) AS share
            FROM staging_zones child
            JOIN LATERAL (
                SELECT p.raw FROM staging_zones p
                WHERE p.level = 1 AND p.raw && child.raw
                ORDER BY ST_Area(ST_Intersection(p.raw, child.raw)) DESC, p.code
                LIMIT 1
            ) municipality ON true
            WHERE child.level = 2
        )
        DELETE FROM staging_zones s
        USING parent
        WHERE s.osm_relation_id = parent.osm_relation_id AND parent.share > 0.9
        RETURNING s.name
    """)
    redundant_names = [line for line in redundant.splitlines()
                       if line and not re.fullmatch(r"DELETE \d+", line)]
    log(f"stage: uklonjeno {len(redundant_names)} jedinica jednakih svojoj opstini"
        f"{': ' + ', '.join(sorted(redundant_names)) if redundant_names else ''}")

    overlaps = psql("""
        SELECT count(*)
        FROM staging_zones a JOIN staging_zones b ON a.osm_relation_id < b.osm_relation_id
        WHERE a.level = 2 AND b.level = 2
          AND ST_Area(ST_CollectionExtract(ST_Intersection(a.raw, b.raw), 3)::geography) > 500
    """)
    log(f"stage: preostalih preklapanja na nivou 2: {overlaps}")


def assign_codes():
    """Kod jedinice nivoa 2 je <kod_opstine>-<slug imena>.

    Roditelj se odreduje po najvecoj povrsini preseka, isto kao u V39, jer granice mesnih
    zajednica i opstina nisu digitalizovane zajedno pa dete redovno viri van roditelja.
    Prefiks opstine je i ono sto razlikuje istoimene jedinice: "MZ Centar" postoji u vise
    opstina, pa bi bez njega kodovi bili u koliziji.
    """
    psql("""
        UPDATE staging_zones child
        SET code = (
            SELECT parent.code FROM staging_zones parent
            WHERE parent.level = 1 AND parent.boundary && child.boundary
            ORDER BY ST_Area(ST_Intersection(parent.boundary, child.boundary)) DESC, parent.code
            LIMIT 1)
        WHERE child.level = 2
    """)
    orphans = psql("SELECT count(*) FROM staging_zones WHERE level = 2 AND code IS NULL")
    if orphans != "0":
        raise RuntimeError(f"{orphans} jedinica nivoa 2 nema roditelja")

    rows = psql("""
        SELECT osm_relation_id, code, name FROM staging_zones WHERE level = 2 ORDER BY code, name
    """).splitlines()
    seen = {}
    for row in rows:
        osm_relation_id, parent_code, name = row.split("|", 2)
        code = f"{parent_code}-{slugify(name)}"
        if code in seen:
            raise RuntimeError(f"kolizija koda {code}: {seen[code]} i {name}")
        seen[code] = name
        if len(code) > 40:
            raise RuntimeError(f"kod duzi od 40 znakova: {code}")
        psql(f"UPDATE staging_zones SET code = {quote(code)} "
             f"WHERE osm_relation_id = {osm_relation_id}")
    log(f"stage: dodeljeno {len(seen)} kodova nivoa 2, bez kolizija")


def emit_level2(output_path):
    rows = psql("""
        SELECT osm_relation_id, code, name, city, ST_AsGeoJSON(boundary, 6)
        FROM staging_zones WHERE level = 2 ORDER BY code
    """).splitlines()

    lines = [
        "-- Zone nivoa 2: mesne zajednice (OSM admin_level 10) i naseljena mesta (admin_level 9).",
        "--",
        f"-- {len(rows)} jedinica. Medijana povrsine mesne zajednice je ~1.2 km2, naseljenog mesta",
        "-- ~13 km2, umesto ~157 km2 koliko je medijana opstine. Sloj je time gustinski adaptivan:",
        "-- gusto urbano jezgro dobija sitne jedinice, retka periferija krupne, pa je broj ljudi po",
        "-- zoni priblizno ujednacen.",
        "--",
        "-- Naseljena mesta koja se preklapaju sa mesnim zajednicama su izbacena u generatoru",
        "-- (npr. \"Beograd (Vracar)\" pokriva celu opstinu i sve njene mesne zajednice), pa je",
        "-- nivo 2 disjunktan. V39 to i proverava.",
        "--",
        "-- Geometrija je uproscena kroz ST_CoverageSimplify na 0.0001 stepeni (~11 m), pa su",
        "-- zajednicke granice poklopljene i nema procepa izmedu susednih jedinica.",
        "--",
        "-- ON CONFLICT (osm_relation_id) DO UPDATE: buduce osvezavanje geometrije je isti fajl sa",
        "-- novim brojem verzije. Azurira redove u mestu, pa zones.id nikada ne mrda i FK iz",
        "-- locations.zone_id ostaje netaknut. Uz takvo osvezavanje OBAVEZNO ide i klon V41.",
        "--",
        "-- Centroid i area_km2 se racunaju u V39, posle dodele roditelja.",
        "",
    ]
    for row in rows:
        osm_relation_id, code, name, city, geojson = row.split("|", 4)
        lines.append(
            "INSERT INTO zones (code, name, city, boundary, centroid_latitude, "
            "centroid_longitude, level, osm_relation_id)\nVALUES ("
            f"{quote(code)}, {quote(name)}, {quote(city)}, "
            f"ST_Multi(ST_CollectionExtract(ST_MakeValid(ST_GeomFromGeoJSON({quote(geojson)})), 3)), "
            f"0, 0, 2, {osm_relation_id})\n"
            "ON CONFLICT (osm_relation_id) DO UPDATE SET "
            "boundary = EXCLUDED.boundary, name = EXCLUDED.name, code = EXCLUDED.code;"
        )
    with open(output_path, "w", encoding="utf-8") as handle:
        handle.write("\n".join(lines) + "\n")
    log(f"emit: {output_path} ({os.path.getsize(output_path)} bajtova, {len(rows)} zona)")


def quote(value):
    if value is None:
        return "NULL"
    return "'" + str(value).replace("'", "''") + "'"


def emit_level1(output_path):
    rows = psql("""
        SELECT osm_relation_id, code, name,
               ST_AsGeoJSON(boundary, 6)
        FROM staging_zones WHERE level = 1 ORDER BY code
    """).splitlines()

    lines = [
        "-- Ispravka granica gradskih opstina (nivo 1).",
        "--",
        "-- V33 je poligone dohvatao PRETRAGOM po imenu kroz LocationIQ, sto je za 10 od 17",
        "-- opstina vratilo poligon NASELJENOG MESTA umesto opstine: Obrenovac 9.3 km2 umesto",
        "-- 411, Sopot 1.2 umesto 271, Mladenovac 11.4 umesto 339. Unija svih 17 zona bila je",
        "-- 963 km2, a Beograd ima 3227 km2.",
        "--",
        "-- Posledica u aplikaciji: ReportService.getNearbyReports odbacuje oglase bez zone, pa",
        "-- oglas u juznom Vozdovcu ili Obrenovcu nikada nije ulazio u \"Found nearby\".",
        "--",
        "-- Geometrija se sada dohvata po EKSPLICITNOM OSM relation id-u (tools/zones/build_zones.py)",
        "-- i uproscava kroz ST_CoverageSimplify, pa zajednicke granice ostaju poklopljene.",
        "--",
        "-- UPDATE, nikada DELETE+INSERT: locations.zone_id ima FK na ove redove, pa bi",
        "-- renumeracija zones.id oborila referencijalni integritet.",
        "",
    ]
    for row in rows:
        osm_relation_id, code, name, geojson = row.split("|", 3)
        lines.append(
            f"UPDATE zones SET boundary = "
            f"ST_Multi(ST_CollectionExtract(ST_MakeValid(ST_GeomFromGeoJSON({quote(geojson)})), 3))"
            f" WHERE code = {quote(code)};"
        )
    lines += [
        "",
        "UPDATE zones",
        "SET centroid_latitude = ST_Y(ST_PointOnSurface(boundary)),",
        "    centroid_longitude = ST_X(ST_PointOnSurface(boundary));",
        "",
        "DO $$",
        "DECLARE covered_km2 numeric;",
        "BEGIN",
        "    SELECT ST_Area(ST_Union(boundary)::geography) / 1e6 INTO covered_km2 FROM zones;",
        "    IF covered_km2 < 3000 THEN",
        "        RAISE EXCEPTION 'Pokrivenost nivoa 1 je samo % km2, ocekivano ~3227',"
        " round(covered_km2);",
        "    END IF;",
        "END $$;",
        "",
    ]
    with open(output_path, "w", encoding="utf-8") as handle:
        handle.write("\n".join(lines))
    log(f"emit: {output_path} ({os.path.getsize(output_path)} bajtova, {len(rows)} zona)")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("stage", choices=["discover", "geometry", "stage",
                                          "emit-level1", "emit-level2"])
    parser.add_argument("--output", default=None)
    arguments = parser.parse_args()

    if arguments.stage == "discover":
        discover()
        return
    with open(cache_path("relations.json"), encoding="utf-8") as handle:
        payload = json.load(handle)
    if arguments.stage == "geometry":
        geometry(payload)
    elif arguments.stage == "stage":
        stage(payload)
    elif arguments.stage == "emit-level1":
        emit_level1(arguments.output)
    elif arguments.stage == "emit-level2":
        emit_level2(arguments.output)


if __name__ == "__main__":
    main()
