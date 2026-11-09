-- V14: Çevrimdışı keşif için tekrar-güvenli client_id.

ALTER TABLE survey_items
    ADD COLUMN IF NOT EXISTS client_id varchar(64);

ALTER TABLE survey_photos
    ADD COLUMN IF NOT EXISTS client_id varchar(64);

CREATE UNIQUE INDEX IF NOT EXISTS uq_survey_items_project_client
    ON survey_items (project_id, client_id)
    WHERE client_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_survey_photos_item_client
    ON survey_photos (survey_item_id, client_id)
    WHERE client_id IS NOT NULL;
