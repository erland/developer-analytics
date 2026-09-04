ALTER TABLE source_repository
    ADD COLUMN repository_size_bytes BIGINT,
    ADD COLUMN code_size_bytes BIGINT;

UPDATE source_repository repository
SET code_size_bytes = language_totals.total_bytes
FROM (
    SELECT repository_id, SUM(measured_value) AS total_bytes
    FROM repository_technology_evidence
    WHERE evidence_type = 'LANGUAGE'
      AND measured_value IS NOT NULL
    GROUP BY repository_id
) language_totals
WHERE repository.id = language_totals.repository_id;
