-- Kada je oglas oznacen kao spojen sa vlasnikom.
--
-- Status RESOLVED postoji u modelu od pocetka i do sada ga NISTA nije postavljalo, pa
-- aplikacija nije znala svoj najvazniji ishod — da li je stvar vracena. Za statistiku
-- zajednice jedini kandidat bio bi MATCHED, koji mesa dve razlicite stvari: vlasnik je sam
-- zatvorio oglas I nekome je odobren claim.
--
-- Kolona, a ne izvodjenje iz statusa, iz dva razloga:
--
-- 1. Statistika trazi TRENUTAK, ne stanje. „Ove nedelje spojeno 5" je upit nad vremenom
--    dogadjaja; iz status = 'RESOLVED' se ne vidi kada se to desilo.
--
-- 2. Moderacija brise status. AdminReportService.restoredStatusFor pri unflag-u IZVODI status
--    iz cinjenica, a „bio je spojen" nije cinjenica koju iko pamti — pa bi flag/unflag ciklus
--    tiho ponistio spajanje. Isti razlog zbog kog V42 pamti hidden_by_user_block umesto da ga
--    pogadja iz statusa.
ALTER TABLE reports
    ADD COLUMN resolved_at TIMESTAMP;

-- Parcijalni indeks: statistika pita „koji su oglasi spojeni i kada", a takvih je mali deo
-- tabele. Indeks nad celom kolonom bi vecinom bio pun NULL-ova.
CREATE INDEX idx_reports_resolved_at ON reports (resolved_at DESC)
    WHERE resolved_at IS NOT NULL;
