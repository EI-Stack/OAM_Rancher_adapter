DROP TABLE IF EXISTS nfm.network_slice;
CREATE TABLE nfm.network_slice
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    name					character varying(32) NOT NULL,
    description	    		character varying(256),
    service_profiles        jsonb NOT NULL,
    CONSTRAINT pkey_network_slice PRIMARY KEY (id)
);

DROP TABLE IF EXISTS nfm.network_slice_instance;
CREATE TABLE nfm.network_slice_instance
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    description	    		character varying(256),
    network_slice_id        bigint NOT NULL,
    ric_slice_list	        text[] NOT NULL,
    CONSTRAINT pkey_network_slice_instance PRIMARY KEY (id)
);
