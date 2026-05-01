CREATE TABLE "ad_messages"
(
    "id"              UUID PRIMARY KEY,
    "ad_id"           UUID                      NOT NULL,
    "sender_role"     VARCHAR(15)               NOT NULL,
    "message"         VARCHAR(1000)             NOT NULL,
    "username_create" VARCHAR(255) NULL     DEFAULT NULL,
    "username_update" VARCHAR(255) NULL     DEFAULT NULL,
    "created_at"      TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT (now()),
    "updated_at"      TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT (now()),
    CONSTRAINT "fk_ad_messages_ad" FOREIGN KEY ("ad_id") REFERENCES "ads" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE INDEX "idx_ad_messages_ad_id_created_at" ON "ad_messages" ("ad_id", "created_at");

CREATE TABLE "ad_messages_aud"
(
    "id"          UUID   NOT NULL,
    "ad_id"       UUID,
    "sender_role" VARCHAR(15),
    "message"     VARCHAR(1000),
    "audit_id"    BIGINT NOT NULL,
    "audit_type"  SMALLINT NULL DEFAULT NULL,
    CONSTRAINT "pk_tbad_messages_aud" PRIMARY KEY ("id", "audit_id"),
    CONSTRAINT "fk_tbad_messages_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

