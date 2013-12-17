CREATE KEYSPACE "TelestaxSMSC"
         WITH REPLICATION = {'class' : 'SimpleStrategy', 'replication_factor': 1};

USE "TelestaxSMSC";

CREATE TABLE "LIVE" (
	"TARGET_ID" ascii PRIMARY KEY,

	"ADDR_DST_DIGITS" ascii,
	"ADDR_DST_TON" int, 
	"ADDR_DST_NPI" int, 

	"IN_SYSTEM" int, 
	"IN_SYSTEM_DATE" timestamp, 
	"DUE_DATE" timestamp,
	"DUE_DELAY" int,
	"SM_STATUS" int,
	"ALERTING_SUPPORTED" boolean,
);

CREATE INDEX ON "LIVE" ("IN_SYSTEM");
CREATE INDEX ON "LIVE" ("DUE_DATE");
CREATE INDEX ON "LIVE" ("IN_SYSTEM_DATE");

ALTER COLUMNFAMILY "LIVE" WITH gc_grace_seconds=0;

CREATE TABLE "LIVE_SMS" ( 
	"ID" uuid, 
	"TARGET_ID" ascii,

	"ADDR_DST_DIGITS" ascii,
	"ADDR_DST_TON" int, 
	"ADDR_DST_NPI" int, 

	"MESSAGE_ID" bigint,
	"MO_MESSAGE_REF" int,
	"ORIG_ESME_NAME" text,
	"ORIG_SYSTEM_ID" text, 
	"SUBMIT_DATE" timestamp, 

	"ADDR_SRC_DIGITS" ascii, 
	"ADDR_SRC_TON" int, 
	"ADDR_SRC_NPI" int, 

	"SERVICE_TYPE" text,  
	"ESM_CLASS" int , 
	"PROTOCOL_ID" int , 
	"PRIORITY" int, 
	"REGISTERED_DELIVERY" int, 
	"REPLACE" int, 
	"DATA_CODING" int, 
	"DEFAULT_MSG_ID" int, 

	"MESSAGE" blob,
	"OPTIONAL_PARAMETERS" text, 
	"SCHEDULE_DELIVERY_TIME" timestamp,
	"VALIDITY_PERIOD" timestamp,
	"LAST_DELIVERY" timestamp,
	"DELIVERY_COUNT" int,

	PRIMARY KEY ("ID") 
);

CREATE INDEX ON "LIVE_SMS" ("TARGET_ID");
CREATE INDEX ON "LIVE_SMS" ("MESSAGE_ID");

ALTER COLUMNFAMILY "LIVE_SMS" WITH gc_grace_seconds=0;

CREATE TABLE "ARCHIVE" ( 
	"ID" uuid, 
    "IN_SYSTEM" int, 

	"ADDR_DST_DIGITS" ascii,
	"ADDR_DST_TON" int, 
	"ADDR_DST_NPI" int, 
	"ADDR_SRC_DIGITS" ascii, 
	"ADDR_SRC_TON" int, 
	"ADDR_SRC_NPI" int, 

	"MESSAGE_ID" bigint,
	"MO_MESSAGE_REF" int,
	"ORIG_ESME_NAME" text,
	"ORIG_SYSTEM_ID" text, 
	"DEST_CLUSTER_NAME" text, 
	"DEST_ESME_NAME" text,
	"DEST_SYSTEM_ID" text,
	"SUBMIT_DATE" timestamp, 
	"DELIVERY_DATE" timestamp, 

	"SERVICE_TYPE" text,  
	"ESM_CLASS" int , 
	"PROTOCOL_ID" int , 
	"PRIORITY" int, 
	"REGISTERED_DELIVERY" int, 
	"REPLACE" int, 
	"DATA_CODING" int, 
	"DEFAULT_MSG_ID" int, 

	"MESSAGE" blob,
	"OPTIONAL_PARAMETERS" text, 
	"SCHEDULE_DELIVERY_TIME" timestamp,
	"VALIDITY_PERIOD" timestamp,

	"IMSI" ascii,
	"NNN_DIGITS" ascii,
	"NNN_AN" int,
	"NNN_NP" int,
	"SM_STATUS" int,
	"SM_TYPE" int, 
	"DELIVERY_COUNT" int, 

	PRIMARY KEY ("ID") 
);

CREATE INDEX ON "ARCHIVE" ("ADDR_DST_DIGITS");
CREATE INDEX ON "ARCHIVE" ("ADDR_SRC_DIGITS");


ALTER COLUMNFAMILY "ARCHIVE" WITH gc_grace_seconds=0;
ALTER COLUMNFAMILY "ARCHIVE" WITH caching='NONE';

CREATE TABLE "SMS_ROUTING_RULE" (
    "ADDRESS" text PRIMARY KEY,
    "CLUSTER_NAME" text, 
);

