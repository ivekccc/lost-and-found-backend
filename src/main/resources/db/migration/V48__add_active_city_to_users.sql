-- Grad koji korisnik trenutno pretrazuje.
--
-- Kolona je NOT NULL, a ne "NULL znaci da nije birao". Nullable bi znacio grananje na svakom
-- filteru i pitanje sta /reports uopste vraca kad grad nije izabran — verovatno sve gradove,
-- cime bi izbor prestao da bude izbor. Zatecen korisnik dobija Beograd, sto je i jedini grad
-- u kome do sada postoje oglasi; aplikacija na osnovu GPS-a predlozi promenu ako pogresi.
ALTER TABLE users
    ADD COLUMN active_city_id BIGINT;

UPDATE users
SET active_city_id = (SELECT id FROM cities WHERE code = 'BG');

ALTER TABLE users
    ALTER COLUMN active_city_id SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT fk_users_active_city FOREIGN KEY (active_city_id) REFERENCES cities (id);
