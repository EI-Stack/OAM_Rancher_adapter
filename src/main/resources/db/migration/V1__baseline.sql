CREATE TABLE backend.app_group
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    name					character varying(32) NOT NULL,
    uuid					character varying(36) NOT NULL,
    paas_request   			jsonb NOT NULL,
    description	    		character varying(256),
    app_infos       		jsonb NOT NULL,
    CONSTRAINT pkey_app_group PRIMARY KEY (id)
);