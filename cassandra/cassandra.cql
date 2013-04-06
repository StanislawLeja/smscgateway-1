CREATE KEYSPACE "TelestaxSMSC"
         WITH REPLICATION = {'CLASS' : 'SimpleStrategy', 'replication_factor': 1};

USE "TelestaxSMSC";

CREATE TABLE "LIVE" (
	"ADDR_DST_DIGITS" ascii,
	"ADDR_DST_TON" int, 
	"ADDR_DST_NPI" int, 

	"IN_SYSTEM" int, 
	"DUE_DATE" timestamp,
	"SM_STATUS" int,
	"DELIVERY_COUNT" int,
	"LAST_DELIVERY" timestamp,
	"ALERTING_SUPPORTED" boolean,

	PRIMARY KEY ("ADDR_DST_DIGITS", "ADDR_DST_TON", "ADDR_DST_NPI") 
);

CREATE INDEX ON "LIVE" ("IN_SYSTEM");
CREATE INDEX ON "LIVE" ("DUE_DATE");

CREATE TABLE "LIVE_SMS" ( 
	"ID" uuid, 

	"ADDR_DST_DIGITS" ascii,
	"ADDR_DST_TON" int, 
	"ADDR_DST_NPI" int, 

	"MESSAGE_ID" bigint,
	"ORIG_ESME_ID" text,
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
	"OPTIONAL_PARAMETERS" MAP<int, blob>, 
	"SCHEDULE_DELIVERY" timestamp,
	"VALIDITY_PERIOD" timestamp,

	PRIMARY KEY ("ADDR_DST_DIGITS", "ADDR_DST_TON", "ADDR_DST_NPI", "ID") 
);

CREATE TABLE "ARCHIVE" ( 
	"ID" uuid, 

	"ADDR_DST_DIGITS" ascii,
	"ADDR_DST_TON" int, 
	"ADDR_DST_NPI" int, 
	"ADDR_SRC_DIGITS" ascii, 
	"ADDR_SRC_TON" int, 
	"ADDR_SRC_NPI" int, 

	"MESSAGE_ID" bigint,
	"ORIG_ESME_ID" text,
	"ORIG_SYSTEM_ID" text, 
	"DEST_CLUSTER_NAME" text, 
	"DEST_ESME_ID" text,
	"DEST_SYSTEM_ID" text,
	"SUBMIT_DATE" timestamp, 
	"DELIVER_DATE" timestamp, 

	"SERVICE_TYPE" text,  
	"ESM_CLASS" int , 
	"PROTOCOL_ID" int , 
	"PRIORITY" int, 
	"REGISTERED_DELIVERY" int, 
	"REPLACE" int, 
	"DATA_CODING" int, 
	"DEFAULT_MSG_ID" int, 

	"MESSAGE" text,
	"OPTIONAL_PARAMETERS" MAP<int, blob>, 
	"SCHEDULE_DELIVERY" timestamp,
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

