-- Grad kao prvorazredan pojam.
--
-- Do sada je grad postojao samo kao tekst u zones.city, uvek 'Beograd', i nijedan upit ga
-- nije koristio jer je grad bio jedan. Cim ih ima vise, gradu trebaju stvari koje tekst ne
-- moze da nosi: stabilan identitet za korisnikov izbor (users.active_city_id), okvir za
-- LocationIQ pretragu, centar za mapu, i prekidac kojim se polupopunjen grad ne nudi.
--
-- code je ono sto se pojavljuje u kodovima zona (BG-VOZ, NS-NSD-DETELINARA) i u
-- generatoru, pa je stabilan identifikator — za razliku od imena, koje je prevodivo.
--
-- osm_relation_id je isti mehanizam kao kod zona: ime jedinice se u OSM-u menja, id ne.
-- Zahvaljujuci njemu se buduce osvezavanje granica moze vezati za grad bez pretrage po imenu,
-- greske koja je Beogradu dala 963 km2 umesto 3227 (vidi V36).
CREATE TABLE cities (
    id                 BIGSERIAL PRIMARY KEY,
    code               VARCHAR(8) NOT NULL,
    name               VARCHAR(100) NOT NULL,
    country_code       CHAR(2) NOT NULL,
    osm_relation_id    BIGINT NOT NULL,
    -- Okvir i centar se racunaju iz unije zona tog grada, pa ostaju prazni do V47.
    center_latitude    NUMERIC(10, 8),
    center_longitude   NUMERIC(11, 8),
    bbox_min_latitude  NUMERIC(10, 8),
    bbox_min_longitude NUMERIC(11, 8),
    bbox_max_latitude  NUMERIC(10, 8),
    bbox_max_longitude NUMERIC(11, 8),
    active             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cities_code UNIQUE (code),
    CONSTRAINT uq_cities_osm_relation_id UNIQUE (osm_relation_id)
);

-- Tri razlicita oblika administrativne podele, namerno:
--   Beograd      Grad (OSM admin_level 7) sa 17 gradskih opstina ispod sebe
--   Novi Sad     Grad (7) BEZ ijedne jedinice na nivou 8 — mesne zajednice vise direktno
--   Bajina Basta obicna opstina (8), bez ijedne mesne zajednice, samo naseljena mesta
-- Ako model podnese sva tri, podnosi bilo koju opstinu u Srbiji.
INSERT INTO cities (code, name, country_code, osm_relation_id) VALUES
    ('BG', 'Beograd', 'RS', 1677007),
    ('NS', 'Novi Sad', 'RS', 1649672),
    ('BB', 'Bajina Bašta', 'RS', 2872865);
