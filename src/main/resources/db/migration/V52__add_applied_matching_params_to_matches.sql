-- Parametri bodovanja se snimaju U RED MECA, ne citaju iz zive konfiguracije pri prikazu.
--
-- Bez ovoga admin prikaz racunice pokazuje tezine koje vaze SADA uz bodove izracunate KAD je mec
-- nastao: promena app.matching.* bez ponovnog racunanja dala bi ekran na kom formula ne daje broj
-- koji stoji pored nje. Isti razlog zbog kog racun cuva cenu u trenutku prodaje umesto da je vuce
-- iz sifarnika.
--
-- Time zapis meca postaje samodovoljan: ulazi (distance_km, text_similarity, time_gap_days) +
-- primenjeni parametri + izlazi (bodovi po komponenti, ukupan skor). Skor se iz reda moze
-- rekonstruisati i posle svake buduce izmene konfiguracije.
--
-- rescan-interval-ms se NE upisuje — odredjuje koliko cesto se racuna, ne kako se boduje.

ALTER TABLE report_matches
    ADD COLUMN applied_max_distance_km NUMERIC(6, 2),
    ADD COLUMN applied_weight_distance INTEGER,
    ADD COLUMN applied_weight_text     INTEGER,
    ADD COLUMN applied_weight_time     INTEGER,
    ADD COLUMN applied_time_decay_days INTEGER,
    ADD COLUMN applied_score_threshold INTEGER;

-- Zateceni redovi su izracunati pre nego sto su se parametri belezili, pa im se upisuju
-- podrazumevane vrednosti iz application.properties. To je tacno samo dok te vrednosti nisu
-- menjane — a jesu podrazumevane od uvodjenja mehanizma (V28), pa je pretpostavka drziva.
UPDATE report_matches
SET applied_max_distance_km = 25,
    applied_weight_distance = 40,
    applied_weight_text     = 35,
    applied_weight_time     = 25,
    applied_time_decay_days = 30,
    applied_score_threshold = 40
WHERE applied_weight_distance IS NULL;

ALTER TABLE report_matches
    ALTER COLUMN applied_max_distance_km SET NOT NULL,
    ALTER COLUMN applied_weight_distance SET NOT NULL,
    ALTER COLUMN applied_weight_text     SET NOT NULL,
    ALTER COLUMN applied_weight_time     SET NOT NULL,
    ALTER COLUMN applied_time_decay_days SET NOT NULL,
    ALTER COLUMN applied_score_threshold SET NOT NULL;
