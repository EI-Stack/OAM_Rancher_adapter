ALTER TABLE IF EXISTS backend.app_group_event
RENAME "name" TO "type";
ALTER TABLE IF EXISTS backend.app_group_event
RENAME "status" TO "event_id";
ALTER TABLE IF EXISTS backend.app_group_event
DROP COLUMN site;
ALTER TABLE IF EXISTS backend.app_group_event
ADD COLUMN group_name text;
ALTER TABLE IF EXISTS backend.app_group_event
ADD COLUMN task_name text;
CREATE TABLE backend.group_task_correspond
(
    id            SERIAL PRIMARY KEY NOT NULL,
    group_id      character varying(36) NOT NULL,
    group_name    text,
    task_id       character varying(36),
    task_name     text
);