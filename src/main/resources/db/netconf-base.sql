CREATE TABLE nfm.audit
(
    id 							bigint NOT NULL,
    entity_version    			integer NOT NULL,
    collection_execution_time 	timestamp with time zone,
    collection_name 			character varying(256),
    detail 						jsonb,
    result 						integer NOT NULL,
    scan_time 					timestamp with time zone,
    security_control_family_id 	character varying(256),
    security_control_no 		character varying(256),
    tag 						bytea,
    CONSTRAINT pkey_audit PRIMARY KEY (id)
)

DROP TABLE IF EXISTS nfm.apm_alarm;
CREATE TABLE nfm.apm_alarm
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

DROP TABLE IF EXISTS nfm.dtm_alarm;
CREATE TABLE nfm.dtm_alarm
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
    perceived_severity		character varying(16) NOT NULL,
    detection_type			character varying(32) NOT NULL,
    detection_interface		character varying(8) NOT NULL,
    end_point				jsonb NOT NULL,
    CONSTRAINT pkey_dtm_alarm PRIMARY KEY (id)
);

-- DROP TABLE IF EXISTS nfm.mitigation;
CREATE TABLE nfm.mitigation
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    name					character varying(32) NOT NULL,
    description	    		character varying(256),
    detection_type			character varying(32),
    detection_interface		character varying(8),
    perceived_severity		character varying(16),
    procedures        		jsonb NOT NULL,
    parameters        		jsonb NOT NULL,
    CONSTRAINT pkey_mitigation PRIMARY KEY (id)
);

-- DROP TABLE IF EXISTS nfm.profile;
CREATE TABLE nfm.profile
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

-- DROP TABLE IF EXISTS nfm.network_slice;
CREATE TABLE nfm.network_slice
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    name					character varying(32) NOT NULL,
    description	    		character varying(256),
    service_profiles        jsonb NOT NULL,
    CONSTRAINT pkey_network_slice PRIMARY KEY (id)
);

-- DROP TABLE IF EXISTS nfm.network_slice_instance;
CREATE TABLE nfm.network_slice_instance
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    description	    		character varying(256),
    network_slice_id        bigint NOT NULL,
    ric_slice_list	        text[] NOT NULL,
    CONSTRAINT pkey_network_slice_instance PRIMARY KEY (id)
);

-- DROP TABLE IF EXISTS nfm.system_parameter;
CREATE TABLE nfm.system_parameter
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    name					character varying(32) NOT NULL,
    parameter		        jsonb NOT NULL,
    CONSTRAINT pkey_system_parameter PRIMARY KEY (id)
);

-- DROP TABLE IF EXISTS nfm.mail_group;
CREATE TABLE nfm.mail_group
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    name					character varying(16) NOT NULL,
    mail_addresses	        text[] NOT NULL,
    CONSTRAINT pkey_mail_group PRIMARY KEY (id)
);

-- DROP TABLE IF EXISTS nfm.statistic;
CREATE TABLE nfm.statistic
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    date			        date NOT NULL,
    data					jsonb NOT NULL,
    CONSTRAINT pkey_statistic PRIMARY KEY (id)
);

DROP TABLE IF EXISTS nfm.fault_alarm;
CREATE TABLE nfm.fault_alarm
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    network_type			character varying(8) NOT NULL,
    alarm_id				character varying(64) NOT NULL,
    perceived_severity		character varying(16) NOT NULL,
    alarm_changed_time		timestamp with time zone,
    alarm_raised_time		timestamp with time zone NOT NULL,
    alarm_type				character varying(64),
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

DROP TABLE IF EXISTS nfm.mec_fault_alarm;
CREATE TABLE nfm.mec_fault_alarm
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    network_type			character varying(8) NOT NULL,
    alarm_id				character varying(64) NOT NULL,
    perceived_severity		character varying(16) NOT NULL,
    alarm_changed_time		timestamp with time zone,
    alarm_raised_time		timestamp with time zone NOT NULL,
    alarm_type				character varying(64),
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

DROP TABLE IF EXISTS nfm.ric_fault_alarm;
CREATE TABLE nfm.ric_fault_alarm
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    network_type			character varying(8) NOT NULL,
    alarm_id				character varying(64) NOT NULL,
    perceived_severity		character varying(16) NOT NULL,
    alarm_changed_time		timestamp with time zone,
    alarm_raised_time		timestamp with time zone NOT NULL,
    alarm_type				character varying(64),
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

DROP TABLE IF EXISTS nfm.fault_alarm_physical;
CREATE TABLE nfm.fault_alarm_physical
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    network_type			character varying(8) NOT NULL,
    alarm_id				character varying(64) NOT NULL,
    perceived_severity		character varying(16) NOT NULL,
    alarm_changed_time		timestamp with time zone,
    alarm_raised_time		timestamp with time zone NOT NULL,
    alarm_type				character varying(64),
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



-- DROP TABLE IF EXISTS nfm.fault_error_message;
CREATE TABLE nfm.fault_error_message
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    network_type			character varying(8) NOT NULL,
    code					integer NOT NULL,
    message					character varying(256) NOT NULL,
    sop						character varying(1024),
    mail_disabled		    boolean NOT NULL,
    mail_addresses			text[] NOT NULL,	
    CONSTRAINT pkey_fault_error_message PRIMARY KEY (id)
);

-- DROP TABLE IF EXISTS nfm.performance_rule;
CREATE TABLE nfm.performance_rule
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    network_type			character varying(8) NOT NULL,
    name					character varying(128) NOT NULL,
    comparison				character varying(16) NOT NULL,
    threshold				double precision NOT NULL,
    period					integer NOT NULL,
    trigger_time			timestamp with time zone,
    severity				character varying(8) NOT NULL,
    disabled				boolean NOT NULL,
    description				character varying(1024),
    sop						character varying(1024),
    mail_disabled		    boolean NOT NULL,
    mail_addresses			text[] NOT NULL,	
    CONSTRAINT pkey_performance_rule PRIMARY KEY (id)
);

-- DROP TABLE IF EXISTS nfm.performance_alarm;
CREATE TABLE nfm.performance_alarm
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    network_type			character varying(8) NOT NULL,
    name					character varying(32) NOT NULL,
    severity				character varying(16) NOT NULL,
    description				character varying(255),
    time					timestamp with time zone NOT NULL,
    comparison				character varying(16) NOT NULL,
    threshold				double precision NOT NULL,
    value					double precision NOT NULL,
    detail					jsonb NOT NULL,
    CONSTRAINT pkey_performance_alarm PRIMARY KEY (id)
);

-- DROP TABLE IF EXISTS nfm.mec_app_package;
CREATE TABLE nfm.mec_app_package
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    name              		character varying(64) NOT NULL,
    description 			character varying(1024),
    icon		 			character varying(102400),
    files					jsonb NOT NULL,
    creation_time			timestamp with time zone NOT NULL,
    CONSTRAINT pkey_mec_app_package PRIMARY KEY (id)
);