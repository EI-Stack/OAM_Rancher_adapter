ALTER TABLE nfm.fault_alarm ALTER COLUMN source TYPE character varying(32);
ALTER TABLE nfm.fault_alarm_physical ALTER COLUMN source TYPE character varying(32);
ALTER TABLE nfm.mec_fault_alarm ALTER COLUMN source TYPE character varying(32);
ALTER TABLE nfm.ric_fault_alarm ALTER COLUMN source TYPE character varying(32);