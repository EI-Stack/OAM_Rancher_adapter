-- DROP TABLE IF EXISTS system_parameter;
CREATE TABLE system_parameter
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    name					character varying(32) NOT NULL,
    parameter		        jsonb NOT NULL,
    CONSTRAINT pkey_system_parameter PRIMARY KEY (id)
);

-- DROP TABLE IF EXISTS mail_group;
CREATE TABLE mail_group
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    name					character varying(16) NOT NULL,
    mail_addresses	        text[] NOT NULL,
    CONSTRAINT pkey_mail_group PRIMARY KEY (id)
);

-- DROP TABLE IF EXISTS statistic;
CREATE TABLE statistic
(
    id 						bigserial NOT NULL,
    entity_version    		integer NOT NULL,
    date			        date NOT NULL,
    data					jsonb NOT NULL,
    CONSTRAINT pkey_statistic PRIMARY KEY (id)
);

-- DROP TABLE IF EXISTS fault_alarm;
CREATE TABLE fault_alarm
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
    CONSTRAINT pkey_fault_alarm PRIMARY KEY (id)
);

-- DROP TABLE IF EXISTS mec_fault_alarm;
CREATE TABLE mec_fault_alarm
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
    region_id				character varying(50) NOT NULL,
    app_id					character varying(32) NOT NULL,
    app_ip					character varying(32) NOT NULL,
    CONSTRAINT pkey_mec_fault_alarm PRIMARY KEY (id)
);

-- DROP TABLE IF EXISTS ric_fault_alarm;
CREATE TABLE ric_fault_alarm
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
    alarm_type				character varying(256),
    detail					jsonb NOT NULL,
    alarm_count				integer,
    start_time				timestamp with time zone,
    acknowledged			boolean NOT NULL,
    acknowledge_comment		character varying(1024),    
    acknowledge_user_id		bigint,
    acknowledge_user_name	character varying(64),
    cleared					boolean,
    field_id				character varying(50) NOT NULL,
    nci						character varying(50) NOT NULL,
    CONSTRAINT pkey_ric_fault_alarm PRIMARY KEY (id)
);

-- DROP TABLE IF EXISTS fault_error_message;
CREATE TABLE fault_error_message
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

-- DROP TABLE IF EXISTS performance_rule;
CREATE TABLE performance_rule
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

-- DROP TABLE IF EXISTS performance_alarm;
CREATE TABLE performance_alarm
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

-- DROP TABLE IF EXISTS mec_app_package;
CREATE TABLE mec_app_package
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