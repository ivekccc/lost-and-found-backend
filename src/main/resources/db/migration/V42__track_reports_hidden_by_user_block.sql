-- Razlikovanje oglasa koje je sakrila blokada naloga od onih koje je oznacio moderator.
--
-- blockUser prebacuje sve aktivne oglase korisnika u FLAGGED, ali unblockUser ih nije vracao:
-- korisnik bi se odblokirao, video svoje oglase u "moji oglasi", a niko drugi ih ne bi video
-- niti bi ulazili u matching — bez ijedne poruke i bez nacina da to sam popravi.
--
-- Vracanje ne sme da ide po statusu FLAGGED, jer bi tada unblock ponistio i nezavisnu odluku
-- moderacije o nekom drugom oglasu istog korisnika. Zato se pamti KO je sakrio oglas.

ALTER TABLE reports
    ADD COLUMN hidden_by_user_block BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_reports_hidden_by_user_block ON reports (hidden_by_user_block)
    WHERE hidden_by_user_block;
