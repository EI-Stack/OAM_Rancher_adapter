DROP TABLE IF EXISTS nfm.fault_alarm_physical;
CREATE TABLE nfm.fault_alarm_physical
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    network_type			character varying(8) NOT NULL,
    name					character varying(50) NOT NULL,
    severity				character varying(8) NOT NULL,
    fault_error_code		integer NOT NULL,
    description	    		character varying(256),
    time					timestamp with time zone NOT NULL,    
    source					character varying(8),
    alarm_type				character varying(32),
    detail					jsonb NOT NULL,
    alarm_count				integer,
    start_time				timestamp with time zone,
    acknowledged			boolean NOT NULL,
    acknowledge_comment		character varying(1024),    
    acknowledge_user_id		bigint,
    acknowledge_user_name	character varying(64),
    cleared					boolean,
    CONSTRAINT pkey_fault_alarm_physical PRIMARY KEY (id)
);