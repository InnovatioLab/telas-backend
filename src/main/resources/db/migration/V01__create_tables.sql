CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE SEQUENCE public.audit_seq
  INCREMENT BY 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START WITH 1 CACHE 1
  NO CYCLE;

CREATE SEQUENCE hibernate_sequence
  INCREMENT BY 1
  MINVALUE 1
  START 1
  CACHE 1
  NO CYCLE;

CREATE TABLE "audit"
(
  "audit_id"         BIGINT             DEFAULT nextval('public.audit_seq') NOT NULL,
  "old_data"         VARCHAR(4096) NULL DEFAULT NULL,
  "changed_at"       TIMESTAMP     NULL DEFAULT NULL,
  "username"         VARCHAR(255)  NULL DEFAULT NULL,
  "timestamp_number" BIGINT        NULL DEFAULT NULL,
  CONSTRAINT "pk_tbauditoria" PRIMARY KEY ("audit_id")
);

CREATE TABLE "verification_codes"
(
  "id"           UUID PRIMARY KEY,
  "code"         VARCHAR(6)               NOT NULL,
  "type"         VARCHAR(10)              NOT NULL,
  "fl_validated" BOOLEAN DEFAULT FALSE,
  "expires_at"   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE "contacts"
(
  "id"                 UUID PRIMARY KEY,
  "contact_preference" VARCHAR(10)              NOT NULL,
  "email"              VARCHAR(255) UNIQUE      NOT NULL,
  "phone"              VARCHAR(11),
  "username_create"    VARCHAR(255)             NULL     DEFAULT NULL,
  "username_update"    VARCHAR(255)             NULL     DEFAULT NULL,
  "created_at"         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "updated_at"         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now())
);

CREATE TABLE "contacts_aud"
(
  "id"                 UUID     NOT NULL,
  "contact_preference" VARCHAR(15),
  "email"              VARCHAR(255),
  "phone"              VARCHAR(11),
  "audit_id"           BIGINT   NOT NULL,
  "audit_type"         SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "pk_tbcontacts_aud" PRIMARY KEY ("id", "audit_id"),
  CONSTRAINT "fk_tbcontacts_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "social_medias"
(
  "id"              UUID PRIMARY KEY,
  "instagram_url"   TEXT                     NULL     DEFAULT NULL,
  "facebook_url"    TEXT                     NULL     DEFAULT NULL,
  "linkedin_url"    TEXT                     NULL     DEFAULT NULL,
  "x_url"           TEXT                     NULL     DEFAULT NULL,
  "username_create" VARCHAR(255)             NULL     DEFAULT NULL,
  "username_update" VARCHAR(255)             NULL     DEFAULT NULL,
  "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now())
);

CREATE TABLE "social_medias_aud"
(
  "id"            UUID     NOT NULL,
  "instagram_url" TEXT     NULL DEFAULT NULL,
  "facebook_url"  TEXT     NULL DEFAULT NULL,
  "linkedin_url"  TEXT     NULL DEFAULT NULL,
  "x_url"         TEXT     NULL DEFAULT NULL,
  "audit_id"      BIGINT   NOT NULL,
  "audit_type"    SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "pk_tbsocial_medias_aud" PRIMARY KEY ("id", "audit_id"),
  CONSTRAINT "fk_tbsocial_medias_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "owners"
(
  "id"                    UUID PRIMARY KEY,
  "first_name"            VARCHAR(50)              NOT NULL,
  "last_name"             VARCHAR(150)             NULL     DEFAULT NULL,
  "email"                 VARCHAR(255) UNIQUE      NOT NULL,
  "identification_number" VARCHAR(15)              NOT NULL UNIQUE,
  "phone"                 VARCHAR(11),
  "username_create"       VARCHAR(255)             NULL     DEFAULT NULL,
  "username_update"       VARCHAR(255)             NULL     DEFAULT NULL,
  "created_at"            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "updated_at"            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now())
);

CREATE TABLE "owners_aud"
(
  "id"                    UUID                NOT NULL,
  "first_name"            VARCHAR(50),
  "last_name"             VARCHAR(150),
  "email"                 VARCHAR(255) UNIQUE NOT NULL,
  "identification_number" VARCHAR(15),
  "phone"                 VARCHAR(11),
  "audit_id"              BIGINT              NOT NULL,
  "audit_type"            SMALLINT            NULL DEFAULT NULL,
  CONSTRAINT "pk_tbowners_aud" PRIMARY KEY ("id", "audit_id"),
  CONSTRAINT "fk_tbowners_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "clients"
(
  "id"                    UUID PRIMARY KEY,
  "business_name"         VARCHAR(255)             NOT NULL,
  "identification_number" VARCHAR(9)               NOT NULL UNIQUE,
  "password"              TEXT,
  "role"                  VARCHAR(20)              NOT NULL DEFAULT 'CLIENT',
  "business_field"        VARCHAR(50)              NOT NULL,
  "status"                VARCHAR(15)              NOT NULL,
  "verification_code_id"  UUID                     NOT NULL,
  "contact_id"            UUID                     NOT NULL,
  "owner_id"              UUID                     NOT NULL,
  "social_media_id"       UUID                     NULL     DEFAULT NULL,
  "username_create"       VARCHAR(255)             NULL     DEFAULT NULL,
  "username_update"       VARCHAR(255)             NULL     DEFAULT NULL,
  "created_at"            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "updated_at"            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "deleted_at"            TIMESTAMP WITH TIME ZONE NULL     DEFAULT NULL,
  "trial_ends_at"         TIMESTAMP WITH TIME ZONE NULL     DEFAULT NULL,
  CONSTRAINT "fk_client_verification_code" FOREIGN KEY ("verification_code_id") REFERENCES "verification_codes" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT "fk_client_contact" FOREIGN KEY ("contact_id") REFERENCES "contacts" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT "fk_client_owner" FOREIGN KEY ("owner_id") REFERENCES "owners" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT "fk_client_social_media" FOREIGN KEY ("social_media_id") REFERENCES "social_medias" ("id") ON UPDATE NO ACTION ON DELETE SET NULL
);

CREATE TABLE "clients_aud"
(
  "id"                    UUID     NOT NULL,
  "business_name"         VARCHAR(255),
  "identification_number" VARCHAR(9),
  "role"                  VARCHAR(20),
  "business_field"        VARCHAR(50),
  "status"                VARCHAR(15),
  "contact_id"            UUID,
  "owner_id"              UUID,
  "social_media_id"       UUID,
  "audit_id"              BIGINT   NOT NULL,
  "audit_type"            SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "pk_tbclients_aud" PRIMARY KEY ("id", "audit_id"),
  CONSTRAINT "fk_tbclients_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "plans"
(
  "id"                      UUID PRIMARY KEY,
  "name"                    VARCHAR(100)             NOT NULL unique,
  "description"             VARCHAR(255)                      DEFAULT NULL,
  "monthly_price"           NUMERIC(10, 2),
  "quarterly_price"         NUMERIC(10, 2),
  "semi_annual_price"       NUMERIC(10, 2),
  "yearly_price"            NUMERIC(10, 2),
  "status"                  VARCHAR(15)              NOT NULL,
  "monitors_quantity"       INTEGER                  NOT NULL,
  "advertising_attachments" INTEGER                  NOT NULL,
  "username_create"         VARCHAR(255)             NULL     DEFAULT NULL,
  "username_update"         VARCHAR(255)             NULL     DEFAULT NULL,
  "created_at"              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "updated_at"              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "inactivated_at"          TIMESTAMP WITH TIME ZONE
);

CREATE TABLE "plans_aud"
(
  "id"                      UUID     NOT NULL,
  "name"                    VARCHAR(100),
  "description"             VARCHAR(255),
  "monthly_price"           NUMERIC(10, 2),
  "quarterly_price"         NUMERIC(10, 2),
  "semi_annual_price"       NUMERIC(10, 2),
  "yearly_price"            NUMERIC(10, 2),
  "status"                  VARCHAR(15),
  "monitors_quantity"       INTEGER,
  "advertising_attachments" INTEGER,
  "audit_id"                BIGINT   NOT NULL,
  "audit_type"              SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "pk_tbplans_aud" PRIMARY KEY ("id", "audit_id"),
  CONSTRAINT "fk_tbplans_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "subscriptions"
(
  "id"              UUID PRIMARY KEY,
  "plan_id"         UUID                     NOT NULL,
  "client_id"       UUID                     NOT NULL,
  "discount"        DECIMAL(5, 2)            NOT NULL DEFAULT 0.00,
  "status"          VARCHAR(15)              NOT NULL DEFAULT 'PENDING',
  "username_create" VARCHAR(255)             NULL     DEFAULT NULL,
  "username_update" VARCHAR(255)             NULL     DEFAULT NULL,
  "started_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "ends_at"         TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT "fk_subscription_plan" FOREIGN KEY ("plan_id") REFERENCES "plans" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT "fk_subscription_client" FOREIGN KEY ("client_id") REFERENCES "clients" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "subscriptions_aud"
(
  "id"         UUID     NOT NULL,
  "plan_id"    UUID,
  "client_id"  UUID,
  "discount"   DECIMAL(5, 2),
  "status"     VARCHAR(15),
  "audit_id"   BIGINT   NOT NULL,
  "audit_type" SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "pk_tbsubscriptions_aud" PRIMARY KEY ("id", "audit_id"),
  CONSTRAINT "fk_tbsubscriptions_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "payments"
(
  "id"              UUID PRIMARY KEY,
  "subscription_id" UUID                     NOT NULL,
  "payment_method"  VARCHAR(50)              NOT NULL,
  "amount"          NUMERIC(10, 2)           NOT NULL,
  "status"          VARCHAR(15)              NOT NULL DEFAULT 'PENDING',
  "username_create" VARCHAR(255)             NULL     DEFAULT NULL,
  "username_update" VARCHAR(255)             NULL     DEFAULT NULL,
  "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  CONSTRAINT "fk_transaction_subscription" FOREIGN KEY ("subscription_id") REFERENCES "subscriptions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "payments_aud"
(
  "id"              UUID     NOT NULL,
  "subscription_id" UUID,
  "payment_method"  VARCHAR(50),
  "amount"          NUMERIC(10, 2),
  "status"          VARCHAR(15),
  "audit_id"        BIGINT   NOT NULL,
  "audit_type"      SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "pk_tbpayments_aud" PRIMARY KEY ("id", "audit_id"),
  CONSTRAINT "fk_tbpayments_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "addresses"
(
  "id"              UUID PRIMARY KEY,
  "street"          VARCHAR(100)             NOT NULL,
  "number"          VARCHAR(10)              NOT NULL,
  "zip_code"        VARCHAR(10)              NOT NULL,
  "city"            VARCHAR(50)              NOT NULL,
  "state"           VARCHAR(2)               NOT NULL,
  "country"         VARCHAR(100)             NOT NULL DEFAULT 'USA',
  "complement"      VARCHAR(100),
  "client_id"       UUID                     NULL     DEFAULT NULL,
  "username_create" VARCHAR(255)             NULL     DEFAULT NULL,
  "username_update" VARCHAR(255)             NULL     DEFAULT NULL,
  "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  CONSTRAINT "client_address" FOREIGN KEY ("client_id") REFERENCES "clients" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "addresses_aud"
(
  "id"         UUID     NOT NULL,
  "street"     VARCHAR(100),
  "number"     VARCHAR(10),
  "zip_code"   VARCHAR(10),
  "city"       VARCHAR(50),
  "state"      VARCHAR(2),
  "country"    VARCHAR(100),
  "complement" VARCHAR(100),
  "client_id"  UUID,
  "audit_id"   BIGINT   NOT NULL,
  "audit_type" SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "pk_tbaddresses_aud" PRIMARY KEY ("id", "audit_id"),
  CONSTRAINT "fk_tbaddresses_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "monitors"
(
  "id"              UUID PRIMARY KEY,
  "fl_active"       BOOLEAN                           DEFAULT TRUE,
  "address_id"      UUID                     NOT NULL,
  "type"            VARCHAR(50)              NOT NULL DEFAULT 'BASIC',
  "size_in_inches"  NUMERIC(5, 2)            NOT NULL DEFAULT 0.00,
  "latitude"        DOUBLE PRECISION         NOT NULL,
  "longitude"       DOUBLE PRECISION         NOT NULL,
  "username_create" VARCHAR(255)             NULL     DEFAULT NULL,
  "username_update" VARCHAR(255)             NULL     DEFAULT NULL,
  "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  CONSTRAINT "fk_monitor_address" FOREIGN KEY ("address_id") REFERENCES "addresses" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE "monitors_aud"
(
  "id"             UUID     NOT NULL,
  "fl_active"      BOOLEAN,
  "address_id"     UUID,
  "type"           VARCHAR(50),
  "size_in_inches" NUMERIC(5, 2),
  "latitude"       DOUBLE PRECISION,
  "longitude"      DOUBLE PRECISION,
  "audit_id"       BIGINT   NOT NULL,
  "audit_type"     SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "pk_tbmonitors_aud" PRIMARY KEY ("id", "audit_id"),
  CONSTRAINT "fk_tbmonitors_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "attachments"
(
  "id"              UUID PRIMARY KEY,
  "name"            VARCHAR(255)             NOT NULL,
  "mime_type"       VARCHAR(15)              NOT NULL,
  "client_id"       UUID                     NOT NULL,
  "username_create" VARCHAR(255)             NULL     DEFAULT NULL,
  "username_update" VARCHAR(255)             NULL     DEFAULT NULL,
  "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  CONSTRAINT "fk_attachment_client" FOREIGN KEY ("client_id") REFERENCES "clients" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE "attachments_aud"
(
  "id"         UUID     NOT NULL,
  "name"       VARCHAR(255),
  "mime_type"  VARCHAR(5),
  "client_id"  UUID,
  "audit_id"   BIGINT   NOT NULL,
  "audit_type" SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "pk_tbattachments_aud" PRIMARY KEY ("id", "audit_id"),
  CONSTRAINT "fk_tbattachments_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "advertising_attachments"
(
  "id"              UUID PRIMARY KEY,
  "name"            VARCHAR(255)             NOT NULL,
  "mime_type"       VARCHAR(15)              NOT NULL,
  "client_id"       UUID                     NOT NULL,
  "username_create" VARCHAR(255)             NULL     DEFAULT NULL,
  "username_update" VARCHAR(255)             NULL     DEFAULT NULL,
  "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  CONSTRAINT "fk_advertising_attachment_client" FOREIGN KEY ("client_id") REFERENCES "clients" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE "advertising_attachments_aud"
(
  "id"         UUID     NOT NULL,
  "name"       VARCHAR(255),
  "mime_type"  VARCHAR(15),
  "client_id"  UUID,
  "audit_id"   BIGINT   NOT NULL,
  "audit_type" SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "pk_tbadvertising_attachments_aud" PRIMARY KEY ("id", "audit_id"),
  CONSTRAINT "fk_tbadvertising_attachments_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "advertising_attachments_attachments"
(
  "advertising_attachment_id" UUID NOT NULL,
  "attachment_id"             UUID NOT NULL,
  PRIMARY KEY ("advertising_attachment_id", "attachment_id"),
  CONSTRAINT "fk_advertising_attachment" FOREIGN KEY ("advertising_attachment_id") REFERENCES "advertising_attachments" ("id") ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT "fk_attachment" FOREIGN KEY ("attachment_id") REFERENCES "attachments" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE "advertising_attachments_attachments_aud"
(
  "advertising_attachment_id" UUID     NOT NULL,
  "attachment_id"             UUID     NOT NULL,
  "audit_id"                  BIGINT   NOT NULL,
  "audit_type"                SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "fk_tbadvertising_attachments_attachments_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

-- CREATE TABLE "client_attachments"
-- (
--   "client_id"     UUID NOT NULL,
--   "attachment_id" UUID NOT NULL,
--   PRIMARY KEY ("client_id", "attachment_id"),
--   CONSTRAINT "fk_client_attachment_client" FOREIGN KEY ("client_id") REFERENCES "clients" ("id") ON UPDATE NO ACTION ON DELETE CASCADE,
--   CONSTRAINT "fk_client_attachment_attachment" FOREIGN KEY ("attachment_id") REFERENCES "attachments" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
-- );
-- 
-- CREATE TABLE "client_advertising_attachments"
-- (
--   "client_id"                 UUID NOT NULL,
--   "advertising_attachment_id" UUID NOT NULL,
--   PRIMARY KEY ("client_id", "advertising_attachment_id"),
--   CONSTRAINT "fk_client_advertising_attachment_client" FOREIGN KEY ("client_id") REFERENCES "clients" ("id") ON UPDATE NO ACTION ON DELETE CASCADE,
--   CONSTRAINT "fk_client_advertising_attachment_advertising_attachment" FOREIGN KEY ("advertising_attachment_id") REFERENCES "advertising_attachments" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
-- );

CREATE TABLE "monitors_advertising_attachments"
(
  "monitor_id"                UUID NOT NULL,
  "advertising_attachment_id" UUID NOT NULL,
  PRIMARY KEY ("monitor_id", "advertising_attachment_id"),
  CONSTRAINT "fk_monitor_advertising_attachment_monitor" FOREIGN KEY ("monitor_id") REFERENCES "monitors" ("id") ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT "fk_monitor_advertising_attachment_advertising_attachment" FOREIGN KEY ("advertising_attachment_id") REFERENCES "advertising_attachments" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE "monitors_advertising_attachments_aud"
(
  "monitor_id"                UUID     NOT NULL,
  "advertising_attachment_id" UUID     NOT NULL,
  "audit_id"                  BIGINT   NOT NULL,
  "audit_type"                SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "fk_tbmonitors_advertising_attachments_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "notifications"
(
  "id"              UUID PRIMARY KEY,
  "message"         TEXT                     NOT NULL,
  "fl_visualized"   BOOLEAN                           DEFAULT FALSE,
  "action_url"      TEXT                     NULL     DEFAULT NULL,
  "client_id"       UUID                     NOT NULL,
  "username_create" VARCHAR(255)             NULL     DEFAULT NULL,
  "username_update" VARCHAR(255)             NULL     DEFAULT NULL,
  "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  CONSTRAINT "fk_notification_client" FOREIGN KEY ("client_id") REFERENCES "clients" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE INDEX idx_clients_identification_number ON clients (identification_number);
CREATE INDEX idx_monitors_fl_active ON monitors (fl_active);
CREATE INDEX idx_email ON contacts (email);

-- Índice para zip_code
CREATE INDEX idx_address_zip_code ON addresses (zip_code);