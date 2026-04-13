#!/usr/bin/env bash
set -euo pipefail

sql_escape() {
  printf '%s' "$1" | sed "s/'/''/g"
}

ROLE_RAW="${1:-}"
ROLE=$(printf '%s' "$ROLE_RAW" | tr '[:lower:]' '[:upper:]')

CONTAINER="${DOCKER_PG_CONTAINER:-nodex-postgres}"
DB="${POSTGRES_DB:-db-telas}"
PGUSER="${POSTGRES_USER:-postgres}"
PGPASSWORD="${POSTGRES_PASSWORD:-postgres}"
DEFAULT_BCRYPT='$2b$10$KdM2kczbi2q0Mh4zldXLYuapLw5eFdRhjvu80tpm8UBAdv8prk5Ry'

usage() {
  echo "Uso: $0 <ADMIN|PARTNER|CLIENT>" >&2
  echo "Variáveis (por tipo): ADMIN_EMAIL, PARTNER_EMAIL, CLIENT_EMAIL, *_BUSINESS_NAME, *_PASSWORD_BCRYPT" >&2
  echo "Partner (endereço opcional): PARTNER_ADDR_STREET, PARTNER_ADDR_ZIP, PARTNER_ADDR_CITY, PARTNER_ADDR_STATE, PARTNER_ADDR_LAT, PARTNER_ADDR_LON" >&2
  exit 1
}

case "${ROLE}" in
  ADMIN)
    EMAIL="${ADMIN_EMAIL:-admin@telas.local}"
    BUSINESS="${ADMIN_BUSINESS_NAME:-Admin}"
    PASSWORD_BCRYPT="${ADMIN_PASSWORD_BCRYPT:-$DEFAULT_BCRYPT}"
    LABEL="Administrador"
    ADDR_STREET=""
    ;;
  PARTNER)
    EMAIL="${PARTNER_EMAIL:-partner@telas.local}"
    BUSINESS="${PARTNER_BUSINESS_NAME:-Partner}"
    PASSWORD_BCRYPT="${PARTNER_PASSWORD_BCRYPT:-$DEFAULT_BCRYPT}"
    LABEL="Partner"
    ADDR_STREET="${PARTNER_ADDR_STREET:-Partner Bootstrap St}"
    ADDR_ZIP="${PARTNER_ADDR_ZIP:-33101}"
    ADDR_CITY="${PARTNER_ADDR_CITY:-Miami}"
    ADDR_STATE="${PARTNER_ADDR_STATE:-FL}"
    ADDR_LAT="${PARTNER_ADDR_LAT:-25.761681}"
    ADDR_LON="${PARTNER_ADDR_LON:--80.191788}"
    ;;
  CLIENT)
    EMAIL="${CLIENT_EMAIL:-client@telas.local}"
    BUSINESS="${CLIENT_BUSINESS_NAME:-Cliente}"
    PASSWORD_BCRYPT="${CLIENT_PASSWORD_BCRYPT:-$DEFAULT_BCRYPT}"
    LABEL="Cliente"
    ADDR_STREET=""
    ;;
  *)
    usage
    ;;
esac

if ! docker ps --format '{{.Names}}' | grep -qx "${CONTAINER}"; then
  echo "Erro: container PostgreSQL '${CONTAINER}' não está em execução." >&2
  exit 1
fi

export PGPASSWORD

E_EMAIL=$(sql_escape "${EMAIL}")
E_BUSINESS=$(sql_escape "${BUSINESS}")
E_PW=$(sql_escape "${PASSWORD_BCRYPT}")

exists=$(docker exec -e PGPASSWORD="${PGPASSWORD}" "${CONTAINER}" \
  psql -U "${PGUSER}" -d "${DB}" -v ON_ERROR_STOP=1 -t -A \
  -c "SELECT CAST(COUNT(*) AS int) FROM contacts WHERE email = '${E_EMAIL}';")

if [[ "${exists}" != "0" ]]; then
  echo "Nada a fazer: já existe contato com e-mail '${EMAIL}'."
  exit 0
fi

if [[ "${ROLE}" == "PARTNER" ]]; then
  E_STREET=$(sql_escape "${ADDR_STREET}")
  E_ZIP=$(sql_escape "${ADDR_ZIP}")
  E_CITY=$(sql_escape "${ADDR_CITY}")
  E_STATE=$(sql_escape "${ADDR_STATE}")
  docker exec -e PGPASSWORD="${PGPASSWORD}" -i "${CONTAINER}" \
    psql -U "${PGUSER}" -d "${DB}" -v ON_ERROR_STOP=1 <<SQL
BEGIN;

WITH vc AS (
  INSERT INTO verification_codes (id, code, type, fl_validated, expires_at)
  VALUES (
    gen_random_uuid(),
    '000000',
    'CONTACT',
    TRUE,
    TIMESTAMPTZ '2099-01-01 00:00:00+00'
  )
  RETURNING id
),
ct AS (
  INSERT INTO contacts (id, email, phone, username_create, username_update, created_at, updated_at)
  VALUES (
    gen_random_uuid(),
    '${E_EMAIL}',
    '11999999999',
    'bootstrap',
    'bootstrap',
    now(),
    now()
  )
  RETURNING id
),
new_client AS (
  INSERT INTO clients (
    id,
    business_name,
    password,
    role,
    status,
    verification_code_id,
    contact_id,
    term_condition_id,
    term_accepted_at,
    username_create,
    username_update,
    created_at,
    updated_at
  )
  SELECT
    gen_random_uuid(),
    '${E_BUSINESS}',
    '${E_PW}',
    '${ROLE}',
    'ACTIVE',
    vc.id,
    ct.id,
    (SELECT id FROM terms_conditions ORDER BY created_at DESC LIMIT 1),
    CASE
      WHEN EXISTS (SELECT 1 FROM terms_conditions) THEN now()
      ELSE NULL
    END,
    'bootstrap',
    'bootstrap',
    now(),
    now()
  FROM vc
  CROSS JOIN ct
  RETURNING id
),
ins_wishlist AS (
  INSERT INTO wishlist (id, client_id)
  SELECT gen_random_uuid(), id FROM new_client
),
ins_addr AS (
  INSERT INTO addresses (
    id,
    street,
    zip_code,
    city,
    state,
    country,
    latitude,
    longitude,
    client_id,
    username_create,
    username_update,
    created_at,
    updated_at
  )
  SELECT
    gen_random_uuid(),
    '${E_STREET}',
    '${E_ZIP}',
    '${E_CITY}',
    '${E_STATE}',
    'US',
    ${ADDR_LAT}::double precision,
    ${ADDR_LON}::double precision,
    id,
    'bootstrap',
    'bootstrap',
    now(),
    now()
  FROM new_client
)
SELECT 1;

COMMIT;
SQL
else
  docker exec -e PGPASSWORD="${PGPASSWORD}" -i "${CONTAINER}" \
    psql -U "${PGUSER}" -d "${DB}" -v ON_ERROR_STOP=1 <<SQL
BEGIN;

WITH vc AS (
  INSERT INTO verification_codes (id, code, type, fl_validated, expires_at)
  VALUES (
    gen_random_uuid(),
    '000000',
    'CONTACT',
    TRUE,
    TIMESTAMPTZ '2099-01-01 00:00:00+00'
  )
  RETURNING id
),
ct AS (
  INSERT INTO contacts (id, email, phone, username_create, username_update, created_at, updated_at)
  VALUES (
    gen_random_uuid(),
    '${E_EMAIL}',
    '11999999999',
    'bootstrap',
    'bootstrap',
    now(),
    now()
  )
  RETURNING id
),
new_client AS (
  INSERT INTO clients (
    id,
    business_name,
    password,
    role,
    status,
    verification_code_id,
    contact_id,
    term_condition_id,
    term_accepted_at,
    username_create,
    username_update,
    created_at,
    updated_at
  )
  SELECT
    gen_random_uuid(),
    '${E_BUSINESS}',
    '${E_PW}',
    '${ROLE}',
    'ACTIVE',
    vc.id,
    ct.id,
    (SELECT id FROM terms_conditions ORDER BY created_at DESC LIMIT 1),
    CASE
      WHEN EXISTS (SELECT 1 FROM terms_conditions) THEN now()
      ELSE NULL
    END,
    'bootstrap',
    'bootstrap',
    now(),
    now()
  FROM vc
  CROSS JOIN ct
  RETURNING id
)
INSERT INTO wishlist (id, client_id)
SELECT gen_random_uuid(), id FROM new_client;

COMMIT;
SQL
fi

echo "${LABEL} criado."
echo "  E-mail: ${EMAIL}"
echo "  Role: ${ROLE}"
if [[ "${ROLE}" == "PARTNER" ]]; then
  echo "  Endereço partner: ${ADDR_STREET}, ${ADDR_ZIP} ${ADDR_CITY}, ${ADDR_STATE} (lat/lon ${ADDR_LAT}, ${ADDR_LON})"
fi
echo "  Senha padrão (se não definiu *_PASSWORD_BCRYPT): admin123"
echo "  Login: POST /api/auth/login com username = e-mail e password."
