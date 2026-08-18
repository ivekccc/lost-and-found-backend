ALTER TABLE reports ADD COLUMN closed_at TIMESTAMP;

-- MATCHED carried two outcomes until now: an approved claim (ownership confirmed) and the owner
-- simply withdrawing the listing. Only the first keeps MATCHED; the rest become CLOSED.
-- The condition mirrors ClaimService.approveClaim, which sets MATCHED only on a FOUND listing.
--
-- closed_at is stamped with NOW() rather than a real close time: nothing recorded when these
-- listings were withdrawn, and the column exists so the status can be rebuilt after moderation
-- rather than as an audit trail.
UPDATE reports r
SET status = 'CLOSED',
    closed_at = NOW()
WHERE r.status = 'MATCHED'
  AND NOT (
      r.type = 'FOUND'
      AND EXISTS (
          SELECT 1 FROM claims c
          JOIN challenges ch ON ch.id = c.challenge_id
          WHERE ch.report_id = r.id AND c.status = 'APPROVED'
      )
  );
