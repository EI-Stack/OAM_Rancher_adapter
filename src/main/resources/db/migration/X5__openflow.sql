DROP TABLE IF EXISTS "tnslice";

CREATE TABLE "tnslice"
(
    "id" bigserial NOT NULL PRIMARY KEY,
    entity_version integer NOT NULL,
    "vlan_id" integer NOT NULL,
    "pcp" integer NOT NULL,
    "head_node" jsonb NOT NULL,
    "tail_node" jsonb NOT NULL,
    "middle_nodes" jsonb NOT NULL default '[]',
    "uplink_max_bitrate" bigint NOT NULL default 0,
    "downlink_max_bitrate" bigint NOT NULL default 0,
    "uplink_min_bitrate" bigint NOT NULL default 0,
    "downlink_min_bitrate" bigint NOT NULL default 0,
    "dest_mac" varchar(17) NOT NULL
);

DROP TABLE IF EXISTS "cell" ;

CREATE TABLE "cell"
(
    "id" bigserial NOT NULL PRIMARY KEY,
    entity_version integer NOT NULL,
    "mac" varchar(17) NOT NULL UNIQUE,
    "tnslice_id" bigint
);
