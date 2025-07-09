CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE SEQUENCE IF NOT EXISTS public.audit_seq
  INCREMENT BY 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START WITH 1 CACHE 1
  NO CYCLE;

CREATE SEQUENCE IF NOT EXISTS hibernate_sequence
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

CREATE TABLE "ips"
(
  "id"         UUID PRIMARY KEY,
  "ip_address" VARCHAR(45)              NOT NULL,
  "dns"        VARCHAR(255)             NULL     DEFAULT NULL,
  "created_at" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now())
);

CREATE TABLE "boxes"
(
  "id"         UUID PRIMARY KEY,
  "fl_active"  BOOLEAN                           DEFAULT TRUE,
  "ip_id"      UUID                     NOT NULL,
  "created_at" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  CONSTRAINT "fk_box_ip" FOREIGN KEY ("ip_id") REFERENCES "ips" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION
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
  "id"              UUID PRIMARY KEY,
  "email"           VARCHAR(255) UNIQUE      NOT NULL,
  "phone"           VARCHAR(11)              NOT NULL,
  "username_create" VARCHAR(255)             NULL     DEFAULT NULL,
  "username_update" VARCHAR(255)             NULL     DEFAULT NULL,
  "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now())
);

CREATE TABLE "contacts_aud"
(
  "id"         UUID     NOT NULL,
  "email"      VARCHAR(255),
  "phone"      VARCHAR(11),
  "audit_id"   BIGINT   NOT NULL,
  "audit_type" SMALLINT NULL DEFAULT NULL,
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
  "tiktok_url"      TEXT                     NULL     DEFAULT NULL,
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
  "tiktok_url"    TEXT     NULL DEFAULT NULL,
  "audit_id"      BIGINT   NOT NULL,
  "audit_type"    SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "pk_tbsocial_medias_aud" PRIMARY KEY ("id", "audit_id"),
  CONSTRAINT "fk_tbsocial_medias_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "terms_conditions"
(
  "id"              UUID PRIMARY KEY,
  "version"         VARCHAR(10)              NOT NULL,
  "content"         VARCHAR(7000)            NOT NULL,
  "username_create" VARCHAR(255)             NULL     DEFAULT NULL,
  "username_update" VARCHAR(255)             NULL     DEFAULT NULL,
  "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now())
);

CREATE TABLE "owners"
(
  "id"                    UUID PRIMARY KEY,
  "first_name"            VARCHAR(50)              NOT NULL,
  "last_name"             VARCHAR(150)             NULL     DEFAULT NULL,
  "email"                 VARCHAR(255) UNIQUE      NOT NULL,
  "identification_number" VARCHAR(15)              NOT NULL UNIQUE,
  "phone"                 VARCHAR(11)              NOT NULL,
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
  "stripe_customer_id"    VARCHAR(255)             NULL     DEFAULT NULL,
  "business_name"         VARCHAR(255)             NOT NULL,
  "identification_number" VARCHAR(9)               NOT NULL UNIQUE,
  "website_url"           TEXT                     NULL     DEFAULT NULL,
  "password"              TEXT,
  "role"                  VARCHAR(20)              NOT NULL DEFAULT 'CLIENT',
  "industry"              VARCHAR(50)              NOT NULL,
  "status"                VARCHAR(15)              NOT NULL,
  "verification_code_id"  UUID                     NOT NULL,
  "contact_id"            UUID                     NOT NULL,
  "owner_id"              UUID                     NOT NULL,
  "social_media_id"       UUID                     NULL     DEFAULT NULL,
  "username_create"       VARCHAR(255)             NULL     DEFAULT NULL,
  "username_update"       VARCHAR(255)             NULL     DEFAULT NULL,
  "term_condition_id"     UUID                     NULL     DEFAULT NULL,
  "created_at"            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "updated_at"            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "deleted_at"            TIMESTAMP WITH TIME ZONE NULL     DEFAULT NULL,
  "trial_ends_at"         TIMESTAMP WITH TIME ZONE NULL     DEFAULT NULL,
  "term_accepted_at"      TIMESTAMP WITH TIME ZONE NULL     DEFAULT NULL,
  CONSTRAINT "fk_client_verification_code" FOREIGN KEY ("verification_code_id") REFERENCES "verification_codes" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT "fk_client_term_condition" FOREIGN KEY ("term_condition_id") REFERENCES "terms_conditions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION,
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
  "industry"              VARCHAR(50),
  "status"                VARCHAR(15),
  "contact_id"            UUID,
  "owner_id"              UUID,
  "social_media_id"       UUID,
  "audit_id"              BIGINT   NOT NULL,
  "audit_type"            SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "pk_tbclients_aud" PRIMARY KEY ("id", "audit_id"),
  CONSTRAINT "fk_tbclients_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "addresses"
(
  "id"              UUID PRIMARY KEY,
  "street"          VARCHAR(100)             NOT NULL,
  "zip_code"        VARCHAR(10)              NOT NULL,
  "city"            VARCHAR(50)              NOT NULL,
  "state"           VARCHAR(2)               NOT NULL,
  "country"         VARCHAR(100)             NOT NULL DEFAULT 'US',
  "complement"      VARCHAR(100),
  "latitude"        DOUBLE PRECISION,
  "longitude"       DOUBLE PRECISION,
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
  "zip_code"   VARCHAR(10),
  "city"       VARCHAR(50),
  "state"      VARCHAR(2),
  "country"    VARCHAR(100),
  "complement" VARCHAR(100),
  "latitude"   DOUBLE PRECISION,
  "longitude"  DOUBLE PRECISION,
  "client_id"  UUID,
  "audit_id"   BIGINT   NOT NULL,
  "audit_type" SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "pk_tbaddresses_aud" PRIMARY KEY ("id", "audit_id"),
  CONSTRAINT "fk_tbaddresses_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "monitors"
(
  "id"                   UUID PRIMARY KEY,
  "fl_active"            BOOLEAN                           DEFAULT TRUE,
  "box_id"               UUID                     NULL     DEFAULT NULL,
  "address_id"           UUID                     NOT NULL,
  "type"                 VARCHAR(50)              NOT NULL DEFAULT 'BASIC',
  "max_blocks"           INTEGER                  NOT NULL DEFAULT 17,
  "product_id"           VARCHAR(255)             NOT NULL,
  "location_description" VARCHAR(255)             NULL     DEFAULT NULL,
  "size_in_inches"       NUMERIC(5, 2)            NOT NULL DEFAULT 0.00,
  "username_create"      VARCHAR(255)             NULL     DEFAULT NULL,
  "username_update"      VARCHAR(255)             NULL     DEFAULT NULL,
  "created_at"           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "updated_at"           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  CONSTRAINT "fk_monitor_address" FOREIGN KEY ("address_id") REFERENCES "addresses" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE "monitors_aud"
(
  "id"                   UUID     NOT NULL,
  "fl_active"            BOOLEAN,
  "address_id"           UUID,
  "max_blocks"           INTEGER,
  "product_id"           VARCHAR(255),
  "location_description" VARCHAR(255),
  "type"                 VARCHAR(50),
  "size_in_inches"       NUMERIC(5, 2),
  "audit_id"             BIGINT   NOT NULL,
  "audit_type"           SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "pk_tbmonitors_aud" PRIMARY KEY ("id", "audit_id"),
  CONSTRAINT "fk_tbmonitors_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "subscriptions"
(
  "id"              UUID PRIMARY KEY,
  "client_id"       UUID                     NOT NULL,
  "recurrence"      VARCHAR(15)              NOT NULL CHECK ("recurrence" IN ('THIRTY_DAYS', 'SIXTY_DAYS', 'NINETY_DAYS', 'MONTHLY')),
  "fl_bonus"        BOOLEAN                  NOT NULL DEFAULT FALSE,
  "status"          VARCHAR(15)              NOT NULL DEFAULT 'PENDING',
  "fl_upgrade"      BOOLEAN                  NOT NULL DEFAULT FALSE,
  "stripe_id"       VARCHAR(255)             NULL     DEFAULT NULL,
  "started_at"      TIMESTAMP WITH TIME ZONE,
  "ends_at"         TIMESTAMP WITH TIME ZONE,
  "username_create" VARCHAR(255)             NULL     DEFAULT NULL,
  "username_update" VARCHAR(255)             NULL     DEFAULT NULL,
  "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  CONSTRAINT "fk_subscription_client" FOREIGN KEY ("client_id") REFERENCES "clients" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "subscriptions_aud"
(
  "id"         UUID     NOT NULL,
  "client_id"  UUID,
  "recurrence" VARCHAR(15),
  "fl_bonus"   BOOLEAN,
  "fl_upgrade" BOOLEAN,
  "status"     VARCHAR(15),
  "stripe_id"  VARCHAR(255),
  "audit_id"   BIGINT   NOT NULL,
  "audit_type" SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "pk_tbsubscriptions_aud" PRIMARY KEY ("id", "audit_id"),
  CONSTRAINT "fk_tbsubscriptions_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "subscriptions_monitors"
(
  "subscription_id" UUID NOT NULL,
  "monitor_id"      UUID NOT NULL,
  PRIMARY KEY ("subscription_id", "monitor_id"),
  CONSTRAINT "fk_subscription_monitor" FOREIGN KEY ("subscription_id") REFERENCES "subscriptions" ("id") ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT "fk_monitor_subscription" FOREIGN KEY ("monitor_id") REFERENCES "monitors" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE "subscriptions_monitors_aud"
(
  "subscription_id" UUID     NOT NULL,
  "monitor_id"      UUID     NOT NULL,
  "audit_id"        BIGINT   NOT NULL,
  "audit_type"      SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "pk_tbsubscriptions_monitors_aud" PRIMARY KEY ("subscription_id", "monitor_id", "audit_id"),
  CONSTRAINT "fk_tbsubscriptions_monitors_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "payments"
(
  "id"              UUID PRIMARY KEY,
  "subscription_id" UUID                     NOT NULL,
  "payment_method"  VARCHAR(50),
  "currency"        VARCHAR(3)               NOT NULL DEFAULT 'usd',
  "stripe_id"       VARCHAR(255)             NULL     DEFAULT NULL,
  "amount"          NUMERIC(10, 2)           NOT NULL,
  "status"          VARCHAR(50)              NOT NULL DEFAULT 'PENDING',
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
  "currency"        VARCHAR(3),
  "stripe_id"       VARCHAR(255),
  "amount"          NUMERIC(10, 2),
  "status"          VARCHAR(15),
  "audit_id"        BIGINT   NOT NULL,
  "audit_type"      SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "pk_tbpayments_aud" PRIMARY KEY ("id", "audit_id"),
  CONSTRAINT "fk_tbpayments_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
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

CREATE TABLE "ad_requests"
(
  "id"              UUID PRIMARY KEY,
  "client_id"       UUID                     NOT NULL,
  "message"         TEXT                     NOT NULL,
  "attachment_ids"  TEXT                     NOT NULL,
  "active"          BOOLEAN                           DEFAULT TRUE,
  "phone"           VARCHAR(11),
  "email"           VARCHAR(255),
  "refusal_count"   INT                               DEFAULT 0,
  "username_create" VARCHAR(255)             NULL     DEFAULT NULL,
  "username_update" VARCHAR(255)             NULL     DEFAULT NULL,
  "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  CONSTRAINT "fk_ad_request_client" FOREIGN KEY ("client_id") REFERENCES "clients" ("id") ON DELETE CASCADE
);

CREATE TABLE "ad_requests_aud"
(
  "id"             UUID     NOT NULL,
  "client_id"      UUID     NOT NULL,
  "message"        TEXT     NOT NULL,
  "attachment_ids" TEXT     NOT NULL,
  "phone"          VARCHAR(11),
  "email"          VARCHAR(255),
  "active"         BOOLEAN,
  "refusal_count"  INT,
  "audit_id"       BIGINT   NOT NULL,
  "audit_type"     SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "pk_tbads_requests_aud" PRIMARY KEY ("id", "audit_id"),
  CONSTRAINT "fk_tbads_requests_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "ads"
(
  "id"              UUID PRIMARY KEY,
  "name"            VARCHAR(255)             NOT NULL,
  "mime_type"       VARCHAR(15)              NOT NULL,
  "client_id"       UUID                     NOT NULL,
  "ad_request_id"   UUID                     NULL     DEFAULT NULL,
  "validation"      VARCHAR(15)              NOT NULL DEFAULT 'PENDING',
  "username_create" VARCHAR(255)             NULL     DEFAULT NULL,
  "username_update" VARCHAR(255)             NULL     DEFAULT NULL,
  "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  CONSTRAINT "fk_ad_client" FOREIGN KEY ("client_id") REFERENCES "clients" ("id") ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT "fk_ad_request" FOREIGN KEY ("ad_request_id") REFERENCES "ad_requests" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "ads_aud"
(
  "id"         UUID     NOT NULL,
  "name"       VARCHAR(255),
  "mime_type"  VARCHAR(15),
  "client_id"  UUID,
  "validation" VARCHAR(15),
  "audit_id"   BIGINT   NOT NULL,
  "audit_type" SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "pk_tbads_aud" PRIMARY KEY ("id", "audit_id"),
  CONSTRAINT "fk_tbads_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "refused_ads"
(
  "id"              UUID PRIMARY KEY,
  "justification"   VARCHAR(100)             NOT NULL,
  "description"     VARCHAR(255)             NULL     DEFAULT NULL,
  "validator_id"    UUID                     NOT NULL,
  "ad_request_id"   UUID                     NOT NULL,
  "username_create" VARCHAR(255)             NULL     DEFAULT NULL,
  "username_update" VARCHAR(255)             NULL     DEFAULT NULL,
  "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  CONSTRAINT "fk_refused_ads_validator" FOREIGN KEY ("validator_id") REFERENCES "clients" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT "fk_refused_ads_ad_request" FOREIGN KEY ("ad_request_id") REFERENCES "ad_requests" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "refused_ads_aud"
(
  "id"            UUID     NOT NULL,
  "justification" VARCHAR(100),
  "description"   VARCHAR(255),
  "validator_id"  UUID,
  "ad_request_id" UUID,
  "audit_id"      BIGINT   NOT NULL,
  "audit_type"    SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "pk_tbarefused_ads_aud" PRIMARY KEY ("id", "audit_id"),
  CONSTRAINT "fk_tbarefused_ads_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "ads_attachments"
(
  "ad_id"         UUID NOT NULL,
  "attachment_id" UUID NOT NULL,
  PRIMARY KEY ("ad_id", "attachment_id"),
  CONSTRAINT "fk_ad" FOREIGN KEY ("ad_id") REFERENCES "ads" ("id") ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT "fk_attachment" FOREIGN KEY ("attachment_id") REFERENCES "attachments" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE "ads_attachments_aud"
(
  "ad_id"         UUID     NOT NULL,
  "attachment_id" UUID     NOT NULL,
  "audit_id"      BIGINT   NOT NULL,
  "audit_type"    SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "fk_tbads_attachments_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "monitors_ads"
(
  "monitor_id"      UUID                     NOT NULL,
  "ad_id"           UUID                     NOT NULL,
  "order_index"     INTEGER                  NOT NULL,
  "username_create" VARCHAR(255)             NULL     DEFAULT NULL,
  "username_update" VARCHAR(255)             NULL     DEFAULT NULL,
  "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  PRIMARY KEY ("monitor_id", "ad_id"),
  CONSTRAINT "fk_monitorads_monitor" FOREIGN KEY ("monitor_id") REFERENCES "monitors" ("id") ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT "fk_monitorads_ad" FOREIGN KEY ("ad_id") REFERENCES "ads" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE "monitors_ads_aud"
(
  "monitor_id"  UUID     NOT NULL,
  "ad_id"       UUID     NOT NULL,
  "order_index" INTEGER,
  "audit_id"    BIGINT   NOT NULL,
  "audit_type"  SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "pk_tbmonitors_ads_aud" PRIMARY KEY ("monitor_id", "ad_id", "audit_id"),
  CONSTRAINT "fk_tbmonitors_ads_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "notifications"
(
  "id"              UUID PRIMARY KEY,
  "message"         TEXT                     NOT NULL,
  "reference"       VARCHAR(255)             NOT NULL,
  "fl_visualized"   BOOLEAN                           DEFAULT FALSE,
  "action_url"      TEXT                     NULL     DEFAULT NULL,
  "client_id"       UUID                     NOT NULL,
  "username_create" VARCHAR(255)             NULL     DEFAULT NULL,
  "username_update" VARCHAR(255)             NULL     DEFAULT NULL,
  "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  CONSTRAINT "fk_notification_client" FOREIGN KEY ("client_id") REFERENCES "clients" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE "carts"
(
  "id"              UUID PRIMARY KEY,
  "client_id"       UUID                     NOT NULL,
  "fl_active"       BOOLEAN                  NOT NULL DEFAULT TRUE,
  "recurrence"      VARCHAR(15)              NOT NULL CHECK ("recurrence" IN ('THIRTY_DAYS', 'SIXTY_DAYS', 'NINETY_DAYS', 'MONTHLY')),
  "username_create" VARCHAR(255)             NULL     DEFAULT NULL,
  "username_update" VARCHAR(255)             NULL     DEFAULT NULL,
  "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  CONSTRAINT "fk_cart_client" FOREIGN KEY ("client_id") REFERENCES "clients" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE "carts_aud"
(
  "id"         UUID     NOT NULL,
  "client_id"  UUID     NOT NULL,
  "fl_active"  BOOLEAN,
  "recurrence" VARCHAR(15),
  "audit_id"   BIGINT   NOT NULL,
  "audit_type" SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "pk_tbcarts_aud" PRIMARY KEY ("id", "audit_id"),
  CONSTRAINT "fk_tbcarts_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "carts_items"
(
  "id"              UUID PRIMARY KEY,
  "cart_id"         UUID                     NOT NULL,
  "monitor_id"      UUID                     NOT NULL,
  "block_quantity"  INTEGER                  NOT NULL DEFAULT 1,
  "username_create" VARCHAR(255)             NULL     DEFAULT NULL,
  "username_update" VARCHAR(255)             NULL     DEFAULT NULL,
  "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
  CONSTRAINT "fk_cart_item_cart" FOREIGN KEY ("cart_id") REFERENCES "carts" ("id") ON UPDATE NO ACTION ON DELETE CASCADE,
  CONSTRAINT "fk_cart_item_monitor" FOREIGN KEY ("monitor_id") REFERENCES "monitors" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE "carts_items_aud"
(
  "id"             UUID     NOT NULL,
  "cart_id"        UUID     NOT NULL,
  "monitor_id"     UUID     NOT NULL,
  "block_quantity" INTEGER,
  "audit_id"       BIGINT   NOT NULL,
  "audit_type"     SMALLINT NULL DEFAULT NULL,
  CONSTRAINT "pk_tbcarts_items_aud" PRIMARY KEY ("id", "audit_id"),
  CONSTRAINT "fk_tbcarts_items_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "subscriptions_flows"
(
  "id"        UUID PRIMARY KEY,
  "status"    VARCHAR(50) NOT NULL DEFAULT 'STARTED',
  "step"      INT                  DEFAULT 1,
  "client_id" UUID        NOT NULL,
  CONSTRAINT "fk_subscriptions_flows_client" FOREIGN KEY ("client_id") REFERENCES "clients" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE "wishlist"
(
  "id"        UUID PRIMARY KEY,
  "client_id" UUID NOT NULL UNIQUE,
  CONSTRAINT "fk_wishlist_client" FOREIGN KEY ("client_id") REFERENCES "clients" ("id") ON DELETE CASCADE
);

CREATE TABLE "wishlist_monitors"
(
  "wishlist_id" UUID NOT NULL,
  "monitor_id"  UUID NOT NULL,
  PRIMARY KEY ("wishlist_id", "monitor_id"),
  CONSTRAINT "fk_wishlist_monitor_wishlist" FOREIGN KEY ("wishlist_id") REFERENCES "wishlist" ("id") ON DELETE CASCADE,
  CONSTRAINT "fk_wishlist_monitor_monitor" FOREIGN KEY ("monitor_id") REFERENCES "monitors" ("id") ON DELETE CASCADE
);

CREATE TABLE webhook_events
(
  "id"          VARCHAR(255) PRIMARY KEY,
  "type"        VARCHAR(255),
  "received_at" TIMESTAMP DEFAULT now()
);

CREATE TABLE shedlock
(
  name       VARCHAR(64) PRIMARY KEY,
  lock_until TIMESTAMP(3) NOT NULL,
  locked_at  TIMESTAMP(3) NOT NULL,
  locked_by  VARCHAR(255) NOT NULL
);

INSERT INTO "terms_conditions" (id, version, content, created_at, updated_at, username_create, username_update)
VALUES ('eb1f62bf-9d16-45c1-be45-bd52f97dffb2',
        '0.0.1',
        'This is a generic condition term for version 0.0.1.',
        now(),
        now(),
        'Virtual Assistant',
        'Virtual Assistant');

INSERT INTO "ips" (id, ip_address)
VALUES (gen_random_uuid(), '192.168.0.7');

CREATE INDEX idx_email ON contacts (email);

CREATE INDEX idx_address_zip_code ON addresses (zip_code);

CREATE INDEX idx_webhook_events_id ON webhook_events (id);

CREATE INDEX idx_clients_identification_number_status ON clients (identification_number, status);

CREATE INDEX idx_clients_id_status ON clients (id, status);

CREATE INDEX idx_monitors_fl_active_type_size ON monitors (fl_active, type, size_in_inches);
CREATE INDEX idx_monitors_address_id ON monitors (address_id);
CREATE INDEX idx_addresses_lat_long ON addresses (latitude, longitude);

CREATE INDEX idx_ads_client_id_validation ON ads (client_id, validation);
CREATE INDEX idx_subscriptions_client_id_status ON subscriptions (client_id, status);
CREATE INDEX idx_subscriptions_ends_at_status ON subscriptions (ends_at, status);
CREATE INDEX idx_payments_stripe_id ON payments (stripe_id);
CREATE INDEX idx_boxes_ip_id ON boxes (ip_id);
CREATE INDEX idx_boxes_id ON boxes (id);
CREATE INDEX idx_monitors_box_id ON monitors (box_id);
CREATE INDEX idx_monitors_id ON monitors (id);
CREATE INDEX idx_monitors_fl_active ON monitors (fl_active);