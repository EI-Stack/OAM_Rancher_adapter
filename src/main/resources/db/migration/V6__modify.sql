ALTER TABLE fault_alarm ALTER COLUMN alarm_type TYPE character varying(64);
ALTER TABLE fault_alarm_physical ALTER COLUMN alarm_type TYPE character varying(64);
ALTER TABLE mec_fault_alarm ALTER COLUMN alarm_type TYPE character varying(64);
ALTER TABLE ric_fault_alarm ALTER COLUMN alarm_type TYPE character varying(64);

-- ALTER TABLE dtm_alarm ALTER COLUMN detection_type DROP not null;
ALTER TABLE mitigation ALTER COLUMN detection_type DROP not null;
-- ALTER TABLE dtm_alarm ALTER COLUMN detection_interface DROP not null;
ALTER TABLE mitigation ALTER COLUMN detection_interface DROP not null;
ALTER TABLE mitigation ALTER COLUMN perceived_severity DROP not null;
