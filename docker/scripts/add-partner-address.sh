#!/usr/bin/env bash
set -euo pipefail

sql_escape() {
  printf '%s' "$1" | sed "s/'/''/g"
}

CONTAINER="${DOCKER_PG_CONTAINER:-nodex-postgres}"
DB="${POSTGRES_DB:-db-telas}"
PGUSER="${POSTGRES_USER:-postgres}"
PGPASSWORD="${POSTGRES_PASSWORD:-postgres}"
EMAIL="${PARTNER_EMAIL:?defina PARTNER_EMAIL (e-mail do partner)}"
ADDR_STREET="${PARTNER_ADDR_STREET:-Partner Bootstrap St}"
ADDR_ZIP="${PARTNER_ADDR_ZIP:-33101}"
ADDR_CITY="${PARTNER_ADDR_CITY:-Miami}"
ADDR_STATE="${PARTNER_ADDR_STATE:-FL}"
ADDR_LAT="${PARTNER_ADDR_LAT:-25.761681}"
ADDR_LON="${PARTNER_ADDR_LON:--80.191788}"

if ! docker ps --format '{{.Names}}' | grep -qx "${CONTAINER}"; then
  echo "Erro: container PostgreSQL '${CONTAINER}' não está em execução." >&2
  exit 1
fi

export PGPASSWORD
E_EMAIL=$(sql_escape "${EMAIL}")
E_STREET=$(sql_escape "${ADDR_STREET}")
E_ZIP=$(sql_escape "${ADDR_ZIP}")
E_CITY=$(sql_escape "${ADDR_CITY}")
E_STATE=$(sql_escape "${ADDR_STATE}")

docker exec -e PGPASSWORD="${PGPASSWORD}" -i "${CONTAINER}" \
  psql -U "${PGUSER}" -d "${DB}" -v ON_ERROR_STOP=1 <<SQL
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
  c.id,
  'bootstrap',
  'bootstrap',
  now(),
  now()
FROM clients c
JOIN contacts ct ON c.contact_id = ct.id
WHERE ct.email = '${E_EMAIL}'
  AND c.role = 'PARTNER'
  AND NOT EXISTS (SELECT 1 FROM addresses a WHERE a.client_id = c.id);
SQL

echo "Se o partner existia sem endereço, uma linha foi inserida. Caso contrário (já tinha endereço ou e-mail inexistente), nada muda."
