-- Oglasi koje je korisnik odlozio za kasnije.
--
-- Do sada je korisnik koji naidje na tudji oglas imao samo dva izlaza: da odmah pokrene
-- verifikaciju ili da ga zaboravi. Najcesci slucaj je treci — „licno na ono sto trazim, nisam
-- siguran, hocu da ispratim" — i za njega nije bilo mesta.
--
-- UNIQUE (user_id, report_id) je nosivi deo: cuvanje je idempotentno, ne broji se. Bez toga
-- bi dvostruki dodir napravio dva reda i oglas bi se u spisku pojavio dvaput.
CREATE TABLE saved_reports (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    report_id  BIGINT NOT NULL REFERENCES reports (id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_saved_reports_user_report UNIQUE (user_id, report_id)
);

-- Spisak se uvek cita po korisniku i sortira po vremenu cuvanja, najnovije prvo.
CREATE INDEX idx_saved_reports_user_created ON saved_reports (user_id, created_at DESC);
