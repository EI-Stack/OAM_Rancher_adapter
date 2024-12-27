-- DROP TABLE IF EXISTS backend.app_group;
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

--DROP TABLE IF EXISTS backend.app_group_event;
CREATE TABLE backend.app_group_event
(
    event_time 				text,
    name    		        text,
    site					text,
    status					text,
    CONSTRAINT pkey_app_group_event PRIMARY KEY (event_time)
);

-- DROP TABLE IF EXISTS main.network_slice;
CREATE TABLE main.network_slice
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    name					character varying(32) NOT NULL,
    description	    		character varying(256),
    service_profiles        jsonb NOT NULL,
    CONSTRAINT pkey_network_slice PRIMARY KEY (id)
);

-- DROP TABLE IF EXISTS main.network_slice_instance;
CREATE TABLE main.network_slice_instance
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    description	    		character varying(256),
    network_slice_id        bigint NOT NULL,
    ric_slice_list	        text[] NOT NULL,
    CONSTRAINT pkey_network_slice_instance PRIMARY KEY (id)
);

-- DROP TABLE IF EXISTS main.system_parameter;
CREATE TABLE main.system_parameter
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    name					character varying(32) NOT NULL,
    parameter		        jsonb NOT NULL,
    CONSTRAINT pkey_system_parameter PRIMARY KEY (id)
);

-- DROP TABLE IF EXISTS main.mail_group;
CREATE TABLE main.mail_group
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    name					character varying(16) NOT NULL,
    mail_addresses	        text[] NOT NULL,
    CONSTRAINT pkey_mail_group PRIMARY KEY (id)
);

-- DROP TABLE IF EXISTS main.statistic;
CREATE TABLE main.statistic
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    date			        date NOT NULL,
    data					jsonb NOT NULL,
    CONSTRAINT pkey_statistic PRIMARY KEY (id)
);

DROP TABLE IF EXISTS main.fault_alarm;
CREATE TABLE main.fault_alarm
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

DROP TABLE IF EXISTS main.mec_fault_alarm;
CREATE TABLE main.mec_fault_alarm
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

DROP TABLE IF EXISTS main.ric_fault_alarm;
CREATE TABLE main.ric_fault_alarm
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

DROP TABLE IF EXISTS main.fault_alarm_physical;
CREATE TABLE main.fault_alarm_physical
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

DROP TABLE IF EXISTS main.detection_alarm;
CREATE TABLE main.detection_alarm
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
    detection_type			character varying(16),
    detection_interface		character varying(8),
    end_point				jsonb,
    Interface_info			jsonb,
    CONSTRAINT pkey_detection_alarm PRIMARY KEY (id)
);

-- DROP TABLE IF EXISTS main.fault_error_message;
CREATE TABLE main.fault_error_message
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

-- DROP TABLE IF EXISTS main.performance_rule;
CREATE TABLE main.performance_rule
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

-- DROP TABLE IF EXISTS main.performance_alarm;
CREATE TABLE main.performance_alarm
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

-- DROP TABLE IF EXISTS main.mec_app_package;
CREATE TABLE main.mec_app_package
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