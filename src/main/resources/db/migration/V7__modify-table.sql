DROP TABLE IF EXISTS apm_alarm;
CREATE TABLE apm_alarm
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,

    alarm_id				character varying(64) NOT NULL,
    alarm_changed_time		timestamp with time zone,
    alarm_raised_time		timestamp with time zone NOT NULL,
    probable_cause			character varying(256),
    source					character varying(256),
    additional_information	jsonb,
    proposed_repair_actions	character varying(1024),
    
    comments				jsonb,
    
    clear_user_id			bigint,
    clear_user_name			character varying(64),
    alarm_cleared_time		timestamp with time zone,
    
    duplicate_count			integer,
    duplicate_time			timestamp with time zone,
    
    error_code				character varying(64),
    
    -- Security APM Only
    perceived_severity		character varying(16) NOT NULL,
    end_point				jsonb,
    CONSTRAINT pkey_apm_alarm PRIMARY KEY (id)
);

DROP TABLE IF EXISTS dtm_alarm;
CREATE TABLE dtm_alarm
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,

    alarm_id				character varying(64) NOT NULL,
    alarm_changed_time		timestamp with time zone,
    alarm_raised_time		timestamp with time zone NOT NULL,
    probable_cause			character varying(256),
    source					character varying(256),
    additional_information	jsonb,
    proposed_repair_actions	character varying(1024),
    
    comments				jsonb,
  
    clear_user_id			bigint,
    clear_user_name			character varying(64),
    alarm_cleared_time		timestamp with time zone,
    
    duplicate_count			integer,
    duplicate_time			timestamp with time zone,
    
    error_code				character varying(64),
    
    -- Security DTM Only
    perceived_severity		character varying(16),
    detection_type			character varying(32),
    detection_interface		character varying(8),
    end_point				jsonb,
    CONSTRAINT pkey_dtm_alarm PRIMARY KEY (id)
);