UPDATE reports SET deleted_at = NOW() WHERE status = 'DELETED' AND deleted_at IS NULL;

DELETE FROM locations l
WHERE NOT EXISTS (SELECT 1 FROM reports r WHERE r.location_id = l.id);
