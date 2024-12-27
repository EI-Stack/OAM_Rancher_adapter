CREATE TABLE backend.app_group_event
(
    eventTime 				text,
    name    		        text,
    site					text,
    status					text,
    CONSTRAINT pkey_app_group_event PRIMARY KEY (eventTime)
);