DROP TABLE IF EXISTS mitigation;
CREATE TABLE mitigation
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    name					character varying(32) NOT NULL,
    description	    		character varying(256),
    detection_type			character varying(32) NOT NULL,
    detection_interface		character varying(8) NOT NULL,
    perceived_severity		character varying(16) NOT NULL,
    procedures        		jsonb NOT NULL,
    parameters        		jsonb NOT NULL,
    CONSTRAINT pkey_mitigation PRIMARY KEY (id)
);

DROP TABLE IF EXISTS profile;
CREATE TABLE profile
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    name					character varying(32) NOT NULL,
    type					character varying(32) NOT NULL,
    description	    		character varying(256),
    json        			jsonb NOT NULL,
    change_time				timestamp with time zone NOT NULL,
    CONSTRAINT pkey_profile PRIMARY KEY (id)
);

DROP TABLE IF EXISTS fault_alarm;
CREATE TABLE fault_alarm
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    network_type			character varying(8) NOT NULL,
    alarm_id				character varying(64) NOT NULL,
    perceived_severity		character varying(16) NOT NULL,
    alarm_changed_time		timestamp with time zone,
    alarm_raised_time		timestamp with time zone NOT NULL,
    alarm_type				character varying(32),
    probable_cause			character varying(256),
    additional_text			character varying(256),
    additional_information	jsonb,
    proposed_repair_actions	character varying(1024),
    
    comments				jsonb,
    
    ack_state				character varying(16),
    ack_time				timestamp with time zone,
    ack_user_id				bigint,
    ack_user_name			character varying(64),
    
    clear_user_id			bigint,
    clear_user_name			character varying(64),
    alarm_cleared_time		timestamp with time zone,
    
    duplicate_count			integer,
    duplicate_time			timestamp with time zone,
    
    error_code				character varying(64),
    
	-- 5GC Only
    source_network_function character varying(32),

    CONSTRAINT pkey_fault_alarm PRIMARY KEY (id)
);

DROP TABLE IF EXISTS mec_fault_alarm;
CREATE TABLE mec_fault_alarm
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    network_type			character varying(8) NOT NULL,
    alarm_id				character varying(64) NOT NULL,
    perceived_severity		character varying(16) NOT NULL,
    alarm_changed_time		timestamp with time zone,
    alarm_raised_time		timestamp with time zone NOT NULL,
    alarm_type				character varying(32),
    probable_cause			character varying(256),
    additional_text			character varying(256),
    additional_information	jsonb,
    proposed_repair_actions	character varying(1024),
    
    comments				jsonb,
    
    ack_state				character varying(16),
    ack_time				timestamp with time zone,
    ack_user_id				bigint,
    ack_user_name			character varying(64),
    
    clear_user_id			bigint,
    clear_user_name			character varying(64),
    alarm_cleared_time		timestamp with time zone,
    
    duplicate_count			integer,
    duplicate_time			timestamp with time zone,
    
    error_code				character varying(64),

    -- MEC Only
    region_id				character varying(50) NOT NULL,
    app_id					character varying(32) NOT NULL,
    app_ip					character varying(32) NOT NULL,
    CONSTRAINT pkey_mec_fault_alarm PRIMARY KEY (id)
);

DROP TABLE IF EXISTS ric_fault_alarm;
CREATE TABLE ric_fault_alarm
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    network_type			character varying(8) NOT NULL,
    alarm_id				character varying(64) NOT NULL,
    perceived_severity		character varying(16) NOT NULL,
    alarm_changed_time		timestamp with time zone,
    alarm_raised_time		timestamp with time zone NOT NULL,
    alarm_type				character varying(32),
    probable_cause			character varying(256),
    additional_text			character varying(256),
    additional_information	jsonb,
    proposed_repair_actions	character varying(1024),
    
    comments				jsonb,
    
    ack_state				character varying(16),
    ack_time				timestamp with time zone,
    ack_user_id				bigint,
    ack_user_name			character varying(64),
    
    clear_user_id			bigint,
    clear_user_name			character varying(64),
    alarm_cleared_time		timestamp with time zone,
    
    duplicate_count			integer,
    duplicate_time			timestamp with time zone,
    
    error_code				character varying(64),

    -- RIC Only
    field_id				character varying(50) NOT NULL,
    nci						character varying(50) NOT NULL,
    CONSTRAINT pkey_ric_fault_alarm PRIMARY KEY (id)
);

DROP TABLE IF EXISTS fault_alarm_physical;
CREATE TABLE fault_alarm_physical
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    network_type			character varying(8) NOT NULL,
    alarm_id				character varying(64) NOT NULL,
    perceived_severity		character varying(16) NOT NULL,
    alarm_changed_time		timestamp with time zone,
    alarm_raised_time		timestamp with time zone NOT NULL,
    alarm_type				character varying(32),
    probable_cause			character varying(256),
    additional_text			character varying(256),
    additional_information	jsonb,
    proposed_repair_actions	character varying(1024),
    
    comments				jsonb,
    
    ack_state				character varying(16),
    ack_time				timestamp with time zone,
    ack_user_id				bigint,
    ack_user_name			character varying(64),
    
    clear_user_id			bigint,
    clear_user_name			character varying(64),
    alarm_cleared_time		timestamp with time zone,
    
    duplicate_count			integer,
    duplicate_time			timestamp with time zone,
    
    error_code				character varying(64),
    CONSTRAINT pkey_fault_alarm_physical PRIMARY KEY (id)
);

DROP TABLE IF EXISTS detection_alarm;
CREATE TABLE detection_alarm
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    network_type			character varying(8) NOT NULL,
    alarm_id				character varying(64) NOT NULL,
    perceived_severity		character varying(16) NOT NULL,
    alarm_changed_time		timestamp with time zone,
    alarm_raised_time		timestamp with time zone NOT NULL,
    alarm_type				character varying(32),
    probable_cause			character varying(256),
    additional_text			character varying(256),
    additional_information	jsonb,
    proposed_repair_actions	character varying(1024),
    
    comments				jsonb,
    
    ack_state				character varying(16),
    ack_time				timestamp with time zone,
    ack_user_id				bigint,
    ack_user_name			character varying(64),
    
    clear_user_id			bigint,
    clear_user_name			character varying(64),
    alarm_cleared_time		timestamp with time zone,
    
    duplicate_count			integer,
    duplicate_time			timestamp with time zone,
    
    error_code				character varying(64),
    
    -- Security Detection Only
    detection_type			character varying(32),
    detection_interface		character varying(8),
    end_point				jsonb,
    CONSTRAINT pkey_detection_alarm PRIMARY KEY (id)
);