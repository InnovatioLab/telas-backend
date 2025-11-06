CREATE
EXTENSION IF NOT EXISTS unaccent;

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
    "audit_id"         BIGINT DEFAULT nextval('public.audit_seq') NOT NULL,
    "old_data"         VARCHAR(4096) NULL DEFAULT NULL,
    "changed_at"       TIMESTAMP NULL DEFAULT NULL,
    "username"         VARCHAR(255) NULL DEFAULT NULL,
    "timestamp_number" BIGINT NULL DEFAULT NULL,
    CONSTRAINT "pk_tbauditoria" PRIMARY KEY ("audit_id")
);

CREATE TABLE "box_address"
(
    "id"         UUID PRIMARY KEY,
    "ip"         VARCHAR(45)              NOT NULL UNIQUE,
    "mac"        VARCHAR(17)              NOT NULL UNIQUE,
    "dns"        VARCHAR(255) NULL     DEFAULT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now())
);

CREATE TABLE "boxes"
(
    "id"             UUID PRIMARY KEY,
    "fl_active"      BOOLEAN                           DEFAULT TRUE,
    "box_address_id" UUID                     NOT NULL,
    "created_at"     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    CONSTRAINT "fk_box_ip" FOREIGN KEY ("box_address_id") REFERENCES "box_address" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION
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
    "username_create" VARCHAR(255) NULL     DEFAULT NULL,
    "username_update" VARCHAR(255) NULL     DEFAULT NULL,
    "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now())
);

CREATE TABLE "contacts_aud"
(
    "id"         UUID   NOT NULL,
    "email"      VARCHAR(255),
    "phone"      VARCHAR(11),
    "audit_id"   BIGINT NOT NULL,
    "audit_type" SMALLINT NULL DEFAULT NULL,
    CONSTRAINT "pk_tbcontacts_aud" PRIMARY KEY ("id", "audit_id"),
    CONSTRAINT "fk_tbcontacts_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "social_medias"
(
    "id"              UUID PRIMARY KEY,
    "instagram_url"   TEXT NULL     DEFAULT NULL,
    "facebook_url"    TEXT NULL     DEFAULT NULL,
    "linkedin_url"    TEXT NULL     DEFAULT NULL,
    "x_url"           TEXT NULL     DEFAULT NULL,
    "tiktok_url"      TEXT NULL     DEFAULT NULL,
    "username_create" VARCHAR(255) NULL     DEFAULT NULL,
    "username_update" VARCHAR(255) NULL     DEFAULT NULL,
    "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now())
);

CREATE TABLE "social_medias_aud"
(
    "id"            UUID   NOT NULL,
    "instagram_url" TEXT NULL DEFAULT NULL,
    "facebook_url"  TEXT NULL DEFAULT NULL,
    "linkedin_url"  TEXT NULL DEFAULT NULL,
    "x_url"         TEXT NULL DEFAULT NULL,
    "tiktok_url"    TEXT NULL DEFAULT NULL,
    "audit_id"      BIGINT NOT NULL,
    "audit_type"    SMALLINT NULL DEFAULT NULL,
    CONSTRAINT "pk_tbsocial_medias_aud" PRIMARY KEY ("id", "audit_id"),
    CONSTRAINT "fk_tbsocial_medias_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "terms_conditions"
(
    "id"              UUID PRIMARY KEY,
    "version"         VARCHAR(10)              NOT NULL,
    "content"         TEXT                     NOT NULL,
    "username_create" VARCHAR(255) NULL     DEFAULT NULL,
    "username_update" VARCHAR(255) NULL     DEFAULT NULL,
    "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now())
);

CREATE TABLE "privacy_policy"
(
    "id"              UUID PRIMARY KEY,
    "version"         VARCHAR(10)              NOT NULL,
    "content"         TEXT                     NOT NULL,
    "username_create" VARCHAR(255) NULL     DEFAULT NULL,
    "username_update" VARCHAR(255) NULL     DEFAULT NULL,
    "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now())
);

CREATE TABLE "clients"
(
    "id"                   UUID PRIMARY KEY,
    "stripe_customer_id"   VARCHAR(255) NULL     DEFAULT NULL,
    "business_name"        VARCHAR(255)             NOT NULL,
    "website_url"          TEXT NULL     DEFAULT NULL,
    "password"             TEXT,
    "role"                 VARCHAR(20)              NOT NULL DEFAULT 'CLIENT',
    "industry"             VARCHAR(50) NULL     DEFAULT NULL,
    "status"               VARCHAR(15)              NOT NULL,
    "verification_code_id" UUID                     NOT NULL,
    "contact_id"           UUID                     NOT NULL,
    "social_media_id"      UUID NULL     DEFAULT NULL,
    "term_condition_id"    UUID NULL     DEFAULT NULL,
    "username_create"      VARCHAR(255) NULL     DEFAULT NULL,
    "username_update"      VARCHAR(255) NULL     DEFAULT NULL,
    "created_at"           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    "updated_at"           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    "deleted_at"           TIMESTAMP WITH TIME ZONE NULL     DEFAULT NULL,
    "term_accepted_at"     TIMESTAMP WITH TIME ZONE NULL     DEFAULT NULL,
    CONSTRAINT "fk_client_verification_code" FOREIGN KEY ("verification_code_id") REFERENCES "verification_codes" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT "fk_client_term_condition" FOREIGN KEY ("term_condition_id") REFERENCES "terms_conditions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT "fk_client_contact" FOREIGN KEY ("contact_id") REFERENCES "contacts" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT "fk_client_social_media" FOREIGN KEY ("social_media_id") REFERENCES "social_medias" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "clients_aud"
(
    "id"              UUID   NOT NULL,
    "business_name"   VARCHAR(255),
    "role"            VARCHAR(20),
    "industry"        VARCHAR(50),
    "status"          VARCHAR(15),
    "contact_id"      UUID,
    "social_media_id" UUID,
    "audit_id"        BIGINT NOT NULL,
    "audit_type"      SMALLINT NULL DEFAULT NULL,
    CONSTRAINT "pk_tbclients_aud" PRIMARY KEY ("id", "audit_id"),
    CONSTRAINT "fk_tbclients_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "addresses"
(
    "id"                   UUID PRIMARY KEY,
    "street"               VARCHAR(100)             NOT NULL,
    "zip_code"             VARCHAR(5)               NOT NULL,
    "city"                 VARCHAR(50)              NOT NULL,
    "state"                VARCHAR(2)               NOT NULL,
    "country"              VARCHAR(50)              NOT NULL DEFAULT 'US',
    "address_2"            VARCHAR(100) NULL     DEFAULT NULL,
    "latitude"             DOUBLE PRECISION,
    "longitude"            DOUBLE PRECISION,
    "location_name"        VARCHAR(255) NULL     DEFAULT NULL,
    "location_description" VARCHAR(255) NULL     DEFAULT NULL,
    "photo_url"            TEXT NULL     DEFAULT NULL,
    "client_id"            UUID NULL     DEFAULT NULL,
    "username_create"      VARCHAR(255) NULL     DEFAULT NULL,
    "username_update"      VARCHAR(255) NULL     DEFAULT NULL,
    "created_at"           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    "updated_at"           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    CONSTRAINT "client_address" FOREIGN KEY ("client_id") REFERENCES "clients" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE "addresses_aud"
(
    "id"                   UUID   NOT NULL,
    "street"               VARCHAR(100),
    "zip_code"             VARCHAR(5),
    "city"                 VARCHAR(50),
    "state"                VARCHAR(2),
    "country"              VARCHAR(50),
    "address_2"            VARCHAR(100),
    "latitude"             DOUBLE PRECISION,
    "longitude"            DOUBLE PRECISION,
    "location_name"        VARCHAR(255),
    "location_description" VARCHAR(255),
    "photo_url"            TEXT,
    "client_id"            UUID,
    "audit_id"             BIGINT NOT NULL,
    "audit_type"           SMALLINT NULL DEFAULT NULL,
    CONSTRAINT "pk_tbaddresses_aud" PRIMARY KEY ("id", "audit_id"),
    CONSTRAINT "fk_tbaddresses_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "monitors"
(
    "id"                   UUID PRIMARY KEY,
    "fl_active"            BOOLEAN                           DEFAULT TRUE,
    "box_id"               UUID NULL     DEFAULT NULL,
    "address_id"           UUID                     NOT NULL,
    "max_blocks"           INTEGER                  NOT NULL DEFAULT 17,
    "product_id"           VARCHAR(255)             NOT NULL,
    "location_description" VARCHAR(255) NULL     DEFAULT NULL,
    "username_create"      VARCHAR(255) NULL     DEFAULT NULL,
    "username_update"      VARCHAR(255) NULL     DEFAULT NULL,
    "created_at"           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    "updated_at"           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    CONSTRAINT "fk_monitor_address" FOREIGN KEY ("address_id") REFERENCES "addresses" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE "monitors_aud"
(
    "id"                   UUID   NOT NULL,
    "fl_active"            BOOLEAN,
    "address_id"           UUID,
    "max_blocks"           INTEGER,
    "product_id"           VARCHAR(255),
    "location_description" VARCHAR(255),
    "audit_id"             BIGINT NOT NULL,
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
    "stripe_id"       VARCHAR(255) NULL     DEFAULT NULL,
    "started_at"      TIMESTAMP WITH TIME ZONE,
    "ends_at"         TIMESTAMP WITH TIME ZONE,
    "username_create" VARCHAR(255) NULL     DEFAULT NULL,
    "username_update" VARCHAR(255) NULL     DEFAULT NULL,
    "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    CONSTRAINT "fk_subscription_client" FOREIGN KEY ("client_id") REFERENCES "clients" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE "subscriptions_aud"
(
    "id"         UUID   NOT NULL,
    "client_id"  UUID,
    "recurrence" VARCHAR(15),
    "fl_bonus"   BOOLEAN,
    "fl_upgrade" BOOLEAN,
    "status"     VARCHAR(15),
    "stripe_id"  VARCHAR(255),
    "audit_id"   BIGINT NOT NULL,
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
    "subscription_id" UUID   NOT NULL,
    "monitor_id"      UUID   NOT NULL,
    "audit_id"        BIGINT NOT NULL,
    "audit_type"      SMALLINT NULL DEFAULT NULL,
    CONSTRAINT "pk_tbsubscriptions_monitors_aud" PRIMARY KEY ("subscription_id", "monitor_id", "audit_id"),
    CONSTRAINT "fk_tbsubscriptions_monitors_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "payments"
(
    "id"              UUID PRIMARY KEY,
    "subscription_id" UUID                     NOT NULL,
    "payment_method"  VARCHAR(50)                       DEFAULT 'card',
    "currency"        VARCHAR(3)               NOT NULL DEFAULT 'usd',
    "stripe_id"       VARCHAR(255) NULL     DEFAULT NULL,
    "amount"          NUMERIC(10, 2)           NOT NULL,
    "status"          VARCHAR(50)              NOT NULL DEFAULT 'PENDING',
    "username_create" VARCHAR(255) NULL     DEFAULT NULL,
    "username_update" VARCHAR(255) NULL     DEFAULT NULL,
    "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    CONSTRAINT "fk_transaction_subscription" FOREIGN KEY ("subscription_id") REFERENCES "subscriptions" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE "payments_aud"
(
    "id"              UUID   NOT NULL,
    "subscription_id" UUID,
    "payment_method"  VARCHAR(50),
    "currency"        VARCHAR(3),
    "stripe_id"       VARCHAR(255),
    "amount"          NUMERIC(10, 2),
    "status"          VARCHAR(15),
    "audit_id"        BIGINT NOT NULL,
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
    "username_create" VARCHAR(255) NULL     DEFAULT NULL,
    "username_update" VARCHAR(255) NULL     DEFAULT NULL,
    "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    CONSTRAINT "fk_attachment_client" FOREIGN KEY ("client_id") REFERENCES "clients" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE "attachments_aud"
(
    "id"         UUID   NOT NULL,
    "name"       VARCHAR(255),
    "mime_type"  VARCHAR(5),
    "client_id"  UUID,
    "audit_id"   BIGINT NOT NULL,
    "audit_type" SMALLINT NULL DEFAULT NULL,
    CONSTRAINT "pk_tbattachments_aud" PRIMARY KEY ("id", "audit_id"),
    CONSTRAINT "fk_tbattachments_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "ad_requests"
(
    "id"              UUID PRIMARY KEY,
    "client_id"       UUID                     NOT NULL,
    "message"         TEXT                     NOT NULL,
    "attachment_ids"  TEXT,
    "active"          BOOLEAN                           DEFAULT TRUE,
    "phone"           VARCHAR(11),
    "email"           VARCHAR(255),
    "username_create" VARCHAR(255) NULL     DEFAULT NULL,
    "username_update" VARCHAR(255) NULL     DEFAULT NULL,
    "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    CONSTRAINT "fk_ad_request_client" FOREIGN KEY ("client_id") REFERENCES "clients" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE "ad_requests_aud"
(
    "id"             UUID,
    "client_id"      UUID,
    "message"        TEXT,
    "attachment_ids" TEXT,
    "phone"          VARCHAR(11),
    "email"          VARCHAR(255),
    "active"         BOOLEAN,
    "audit_id"       BIGINT NOT NULL,
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
    "ad_request_id"   UUID NULL     DEFAULT NULL,
    "validation"      VARCHAR(15)              NOT NULL DEFAULT 'PENDING',
    "username_create" VARCHAR(255) NULL     DEFAULT NULL,
    "username_update" VARCHAR(255) NULL     DEFAULT NULL,
    "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    CONSTRAINT "fk_ad_client" FOREIGN KEY ("client_id") REFERENCES "clients" ("id") ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT "fk_ad_request" FOREIGN KEY ("ad_request_id") REFERENCES "ad_requests" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "ads_aud"
(
    "id"         UUID   NOT NULL,
    "name"       VARCHAR(255),
    "mime_type"  VARCHAR(15),
    "client_id"  UUID,
    "validation" VARCHAR(15),
    "audit_id"   BIGINT NOT NULL,
    "audit_type" SMALLINT NULL DEFAULT NULL,
    CONSTRAINT "pk_tbads_aud" PRIMARY KEY ("id", "audit_id"),
    CONSTRAINT "fk_tbads_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "refused_ads"
(
    "id"              UUID PRIMARY KEY,
    "justification"   VARCHAR(100)             NOT NULL,
    "description"     VARCHAR(255) NULL     DEFAULT NULL,
    "ad_id"           UUID                     NOT NULL,
    "username_create" VARCHAR(255) NULL     DEFAULT NULL,
    "username_update" VARCHAR(255) NULL     DEFAULT NULL,
    "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    CONSTRAINT "fk_refused_ads_ad" FOREIGN KEY ("ad_id") REFERENCES "ads" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE "refused_ads_aud"
(
    "id"            UUID   NOT NULL,
    "justification" VARCHAR(100),
    "description"   VARCHAR(255),
    "ad_id"         UUID,
    "audit_id"      BIGINT NOT NULL,
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
    "ad_id"         UUID   NOT NULL,
    "attachment_id" UUID   NOT NULL,
    "audit_id"      BIGINT NOT NULL,
    "audit_type"    SMALLINT NULL DEFAULT NULL,
    CONSTRAINT "fk_tbads_attachments_aud_tbaudit" FOREIGN KEY ("audit_id") REFERENCES "audit" ("audit_id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

CREATE TABLE "monitors_ads"
(
    "monitor_id"      UUID                     NOT NULL,
    "ad_id"           UUID                     NOT NULL,
    "order_index"     INTEGER                  NOT NULL,
    "username_create" VARCHAR(255) NULL     DEFAULT NULL,
    "username_update" VARCHAR(255) NULL     DEFAULT NULL,
    "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    PRIMARY KEY ("monitor_id", "ad_id"),
    CONSTRAINT "fk_monitorads_monitor" FOREIGN KEY ("monitor_id") REFERENCES "monitors" ("id") ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT "fk_monitorads_ad" FOREIGN KEY ("ad_id") REFERENCES "ads" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE "monitors_ads_aud"
(
    "monitor_id"  UUID   NOT NULL,
    "ad_id"       UUID   NOT NULL,
    "order_index" INTEGER,
    "audit_id"    BIGINT NOT NULL,
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
    "action_url"      TEXT NULL     DEFAULT NULL,
    "client_id"       UUID                     NOT NULL,
    "username_create" VARCHAR(255) NULL     DEFAULT NULL,
    "username_update" VARCHAR(255) NULL     DEFAULT NULL,
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
    "username_create" VARCHAR(255) NULL     DEFAULT NULL,
    "username_update" VARCHAR(255) NULL     DEFAULT NULL,
    "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    CONSTRAINT "fk_cart_client" FOREIGN KEY ("client_id") REFERENCES "clients" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE "carts_aud"
(
    "id"         UUID   NOT NULL,
    "client_id"  UUID   NOT NULL,
    "fl_active"  BOOLEAN,
    "recurrence" VARCHAR(15),
    "audit_id"   BIGINT NOT NULL,
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
    "username_create" VARCHAR(255) NULL     DEFAULT NULL,
    "username_update" VARCHAR(255) NULL     DEFAULT NULL,
    "created_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    "updated_at"      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (now()),
    CONSTRAINT "fk_cart_item_cart" FOREIGN KEY ("cart_id") REFERENCES "carts" ("id") ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT "fk_cart_item_monitor" FOREIGN KEY ("monitor_id") REFERENCES "monitors" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE "carts_items_aud"
(
    "id"             UUID   NOT NULL,
    "cart_id"        UUID   NOT NULL,
    "monitor_id"     UUID   NOT NULL,
    "block_quantity" INTEGER,
    "audit_id"       BIGINT NOT NULL,
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
    CONSTRAINT "fk_wishlist_client" FOREIGN KEY ("client_id") REFERENCES "clients" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE "wishlist_monitors"
(
    "wishlist_id" UUID NOT NULL,
    "monitor_id"  UUID NOT NULL,
    PRIMARY KEY ("wishlist_id", "monitor_id"),
    CONSTRAINT "fk_wishlist_monitor_wishlist" FOREIGN KEY ("wishlist_id") REFERENCES "wishlist" ("id") ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT "fk_wishlist_monitor_monitor" FOREIGN KEY ("monitor_id") REFERENCES "monitors" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
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
VALUES (gen_random_uuid(),
        '0.0.1',
        '<div class="content-intro">
  These Terms of Service (“Terms”) govern your access to and use of Telas’ digital advertising platform, website, and related services (the “Services”). By using our Services, you agree to these Terms. If you do not agree, you may not use the Services.
</div>

<div class="document-section">
  <h4>1. Eligibility</h4>
  <div class="section-content">
    You must be at least 18 years old to use our Services. By using the Services, you represent and warrant that you meet this requirement.
  </div>
</div>

<div class="document-section">
  <h4>2. Account Registration</h4>
  <div class="section-content">
    You must provide accurate and complete information when creating an account. You are responsible for maintaining the confidentiality of your login credentials and for all activities under your account. Notify us immediately of any unauthorized access.
  </div>
</div>

<div class="document-section">
  <h4>3. Use of Services</h4>
  <div class="section-content">
    You agree to use the Services only for lawful purposes and in compliance with these Terms. You may not:
        <ul class="content-list">
            <li>Upload or publish content that is illegal, fraudulent, misleading, harmful, offensive, or otherwise objectionable.</li>
            <li>Interfere with or disrupt the functionality of the Services.</li>
            <li>Misuse the Services in a way that harms Telas, its reputation, or its users.</li>
          </ul>
  </div>
</div>

<div class="document-section">
  <h4>4. Advertising Content</h4>
  <div class="section-content">
    You retain ownership of all content you submit (“Content”). By submitting Content, you grant Telas a worldwide, non-exclusive, royalty-free license to use, display, and distribute such Content solely for the purpose of providing the Services.
  </div>
  <div class="section-content">
    You represent and warrant that you own or have all necessary rights to submit the Content, and that it does not infringe the rights of any third party.
  </div>
  <div class="section-content">
    <strong>Content Review Rights:</strong> Telas reserves the right, at its sole discretion, to review, reject, or remove Content that violates these Terms or may harm our brand, users, or community standards.
  </div>
</div>

<div class="document-section">
  <h4>5. Payments & Subscriptions</h4>
  <div class="section-content">
    Fees for Services must be paid in accordance with the pricing and billing terms presented at the time of purchase.
     <ul class="content-list">
        <li><strong>Automatic Renewal:</strong> Unless canceled, subscriptions automatically renew at the end of each billing cycle.</li>
        <li><strong>Cancellations:</strong>  You may cancel your subscription at any time. Cancellations take effect at the end of the current billing period.</li>
        <li><strong>Refunds:</strong>  Except where required by law, all payments are non-refundable.</li>
      </ul>
  </div>
</div>

<div class="document-section">
  <h4>6. Intellectual Property</h4>
  <div class="section-content">
    All intellectual property rights in the Services (including software, design, trademarks, and content not provided by you) are owned by Telas or its licensors. You may not copy, modify, distribute, or reverse engineer any part of the Services without written permission.
  </div>
</div>

<div class="document-section">
  <h4>7. Data & Privacy</h4>
  <div class="section-content">
    Your use of the Services is also governed by our <a href="/privacy-policy">Privacy Policy</a>. By using the Services, you consent to our collection, use, and processing of personal data as described in that policy.
  </div>
</div>

<div class="document-section">
  <h4>8. Service Availability</h4>
  <div class="section-content">
    Telas strives to keep the Services available but does not guarantee uninterrupted access. We may suspend or limit Services for maintenance, upgrades, or technical issues without liability.
  </div>
</div>

<div class="document-section">
  <h4>9. Termination</h4>
  <div class="section-content">
   Telas may suspend or terminate your account if you violate these Terms, fail to pay fees, or engage in harmful conduct. You may stop using the Services at any time.
  </div>
</div>

<div class="document-section">
  <h4>10. Disclaimers</h4>
    <div class="section-content">
      The Services are provided “as is” and “as available.” Telas make no warranties or guarantees, express or implied, including but not limited to merchantability, fitness for a particular purpose, or expected advertising results.
    </div>
</div>

<div class="document-section">
  <h4>11. Limitation of Liability</h4>
  <div class="section-content">
    To the maximum extent permitted by law, Telas shall not be liable for indirect, incidental, consequential, or punitive damages. Telas’ total liability for any claim shall not exceed the amount you paid for the Services during the twelve (12) months preceding the claim.
  </div>
</div>

<div class="document-section">
  <h4>12. Indemnification</h4>
  <div class="section-content">
    You agree to indemnify, defend, and hold harmless Telas, its affiliates, employees, and partners from any claims, damages, losses, or expenses arising from your Content, your use of the Services, or your violation of these Terms.
  </div>
</div>

<div class="document-section">
  <h4>13. Force Majeure</h4>
  <div class="section-content">
    Telas shall not be liable for any failure or delay caused by circumstances beyond its reasonable control, including natural disasters, power outages, labor disputes, cyberattacks, or government actions.
  </div>
</div>

<div class="document-section">
  <h4>14. Governing Law & Dispute Resolution</h4>
  <div class="section-content">
    These Terms are governed by the laws of [Insert State/Country].
    </br>
	Any dispute arising out of or relating to these Terms shall be resolved by <strong>binding arbitration</strong> under the rules of [AAA/JAMS or similar], conducted in [Insert Location]. You and Telas 	waive any right to participate in class actions or jury trials.
  </div>
</div>

<div class="document-section">
  <h4>15. Changes to Terms</h4>
  <div class="section-content">
    Telas may update these Terms periodically. We will notify you of material changes by email or through the platform. Continued use of the Services after changes take effect constitutes acceptance of the updated Terms.
  </div>
</div>

<div class="document-section">
  <h4>16. Entire Agreement</h4>
  <div class="section-content">
    These Terms, together with our Privacy Policy, constitute the entire agreement between you and Telas and supersede all prior agreements or understandings. If any part of these Terms is found invalid, the remaining provisions remain in full force.
  </div>
</div>


<div class="document-section">
  <h4>17. Contact</h4>
  <div class="contact-info">
    <div class="contact-item company-name"><strong>Telas - Legal Department</strong></div>
    <div class="contact-item email"><strong class="label">Email: </strong>
              <a href="mailto:support@telas-ads.com">support@telas-ads.com </a>
    </div>
    <div class="contact-item address"><span class="label">Address: </span>[Insert Company Address]</div>
  </div>
</div>
',
        now(),
        now(),
        'Virtual Assistant',
        'Virtual Assistant');

INSERT INTO "privacy_policy" (id, version, content, created_at, updated_at, username_create, username_update)
VALUES (gen_random_uuid(),
        '0.0.1',
        '<div class="content-intro">
          Telas ("we," "our," "us") is committed to protecting your privacy. This Privacy Policy explains how we
          collect, use, disclose, and safeguard your information when you use our digital advertising services,
          platform, and website (collectively, the "Services").
        </div>

        <div class="document-section">
          <h4>1. Information We Collect</h4>
          <ul class="content-list">
            <li>Personal Information: Name, email, phone, billing info, company details.</li>
            <li>Usage Data: IP address, device info, browser type, usage logs.</li>
            <li>Advertising Data: Campaign metrics, interaction data.</li>
          </ul>
        </div>

        <div class="document-section">
          <h4>2. How We Use Information</h4>
          <ul class="content-list">
            <li>Provide and improve Services.</li>
            <li>Manage accounts, billing, and customer support.</li>
            <li>Analyze campaign performance.</li>
            <li>Send service updates and communications.</li>
            <li>Comply with legal obligations.</li>
          </ul>
        </div>

        <div class="document-section">
          <h4>3. Sharing of Information</h4>
          <ul class="content-list">
            <li>With service providers.</li>
            <li>During business transfers.</li>
            <li>For legal compliance.</li>
          </ul>
        </div>

        <div class="document-section">
          <h4>4. Data Security</h4>
          <p>We implement safeguards to protect data.
          </p>
        </div>

        <div class="document-section">
          <h4>5. Data Retention
          </h4>
          <p>We retain data only as long as necessary.
          </p>
        </div>

        <div class="document-section">
          <h4>6. Your Rights
          </h4>
          <p>Access, update, delete, or restrict use of your data by contacting us.
          </p>
        </div>

        <div class="document-section">
          <h4>7. Cookies & Tracking
          </h4>
          <p>We use cookies and similar tech; you can adjust browser settings.
          </p>
        </div>

        <div class="document-section">
          <h4>8. Third-Party Links
          </h4>
          <p>We are not responsible for external sites.
          </p>
        </div>

        <div class="document-section">
          <h4>9. International Data Transfers
          </h4>
          <p>Your data may be transferred to other countries.
          </p>
        </div>

        <div class="document-section">
          <h4>10. Changes to Policy</h4>
          <p>We may update periodically; updates will be posted.</p>
        </div>

        <div class="document-section">
          <h4>11. Contact Us</h4>
          <div class="contact-info">
            <div class="contact-item company-name"><strong>Telas - Privacy Office</strong></div>
            <div class="contact-item email"><strong class="label">Email: </strong>
              <a href="mailto:support@telas-ads.com">support@telas-ads.com </a>
            </div>
            <div class="contact-item address"><strong class="label">Address: </strong>[Insert Company Address]</div>
          </div>
        </div>
',
        now(),
        now(),
        'Virtual Assistant',
        'Virtual Assistant');

CREATE INDEX idx_email ON contacts (email);

CREATE INDEX idx_address_zip_code ON addresses (zip_code);

CREATE INDEX idx_webhook_events_id ON webhook_events (id);

CREATE INDEX idx_clients_id_status ON clients (id, status);

CREATE INDEX idx_monitors_address_id ON monitors (address_id);
CREATE INDEX idx_addresses_lat_long ON addresses (latitude, longitude);

CREATE INDEX idx_ads_client_id_validation ON ads (client_id, validation);
CREATE INDEX idx_subscriptions_client_id_status ON subscriptions (client_id, status);
CREATE INDEX idx_subscriptions_ends_at_status ON subscriptions (ends_at, status);
CREATE INDEX idx_payments_stripe_id ON payments (stripe_id);
CREATE INDEX idx_boxes_ip_id ON boxes (box_address_id);
CREATE INDEX idx_boxes_id ON boxes (id);
CREATE INDEX idx_monitors_box_id ON monitors (box_id);
CREATE INDEX idx_monitors_id ON monitors (id);
CREATE INDEX idx_monitors_fl_active ON monitors (fl_active);