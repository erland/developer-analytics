ALTER TABLE repository_activity_month ADD COLUMN privacy_provenance VARCHAR(32) NOT NULL DEFAULT 'PUBLIC_ONLY';
ALTER TABLE user_activity_month ADD COLUMN privacy_provenance VARCHAR(32) NOT NULL DEFAULT 'PUBLIC_ONLY';
ALTER TABLE technology_activity_month ADD COLUMN privacy_provenance VARCHAR(32) NOT NULL DEFAULT 'PUBLIC_ONLY';
ALTER TABLE repository_technology_evidence ADD COLUMN privacy_provenance VARCHAR(32) NOT NULL DEFAULT 'PUBLIC_ONLY';
ALTER TABLE user_technology_assessment ADD COLUMN privacy_provenance VARCHAR(32) NOT NULL DEFAULT 'PUBLIC_ONLY';
ALTER TABLE repository_project_category ADD COLUMN privacy_provenance VARCHAR(32) NOT NULL DEFAULT 'PUBLIC_ONLY';
ALTER TABLE project_significance_assessment ADD COLUMN privacy_provenance VARCHAR(32) NOT NULL DEFAULT 'PUBLIC_ONLY';

UPDATE repository_technology_evidence e SET privacy_provenance = CASE WHEN r.visibility='PRIVATE' THEN 'PRIVATE_AGGREGATE' ELSE 'PUBLIC_ONLY' END FROM source_repository r WHERE r.id=e.repository_id;
UPDATE repository_project_category c SET privacy_provenance = CASE WHEN r.visibility='PRIVATE' THEN 'PRIVATE_AGGREGATE' ELSE 'PUBLIC_ONLY' END FROM source_repository r WHERE r.id=c.repository_id;
UPDATE project_significance_assessment a SET privacy_provenance = CASE WHEN r.visibility='PRIVATE' THEN 'PRIVATE_AGGREGATE' ELSE 'PUBLIC_ONLY' END FROM source_repository r WHERE r.id=a.repository_id;
UPDATE repository_activity_month a SET privacy_provenance = CASE WHEN r.visibility='PRIVATE' THEN 'PRIVATE_AGGREGATE' ELSE 'PUBLIC_ONLY' END FROM source_repository r WHERE r.id=a.source_repository_id;
