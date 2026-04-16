#!/usr/bin/env bash
set -euo pipefail

if [ -z "${DISCORD_DEPLOY_WEBHOOK_URL:-}" ]; then
  echo "DISCORD_DEPLOY_WEBHOOK_URL não configurado — notificação Discord ignorada."
  exit 0
fi

APP_NAME="${APP_NAME:-telas}"
VERSION="unknown"
if [ -f pom.xml ]; then
  VERSION=$(grep -A8 '<artifactId>telas</artifactId>' pom.xml | grep '<version>' | head -1 | sed 's/.*<version>//;s/<\/version>.*//')
  if [ -z "$VERSION" ]; then
    VERSION=$(grep -oE '<version>[^<]+</version>' pom.xml | tail -1 | sed 's/<version>//;s/<\/version>//')
  fi
elif [ -f package.json ]; then
  VERSION=$(grep -m1 '"version"' package.json | sed -E 's/.*"version"[[:space:]]*:[[:space:]]*"([^"]+)".*/\1/' || true)
  [ -z "$VERSION" ] && VERSION="unknown"
fi

SHORT_SHA="${GITHUB_SHA:0:7}"
REF="${GITHUB_REF_NAME:-}"
AUTHOR=$(jq -r '.head_commit.author.name // .pusher.name // "unknown"' "$GITHUB_EVENT_PATH")
MSG=$(jq -r '.head_commit.message // .commits[0].message // "N/A"' "$GITHUB_EVENT_PATH")

REPO="${GITHUB_REPOSITORY:-}"
RUN_URL="${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID}"
TS=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

PAYLOAD=$(jq -n \
  --arg app "$APP_NAME" \
  --arg ver "$VERSION" \
  --arg sha "$SHORT_SHA" \
  --arg ref "$REF" \
  --arg author "$AUTHOR" \
  --arg msg "$MSG" \
  --arg repo "$REPO" \
  --arg url "$RUN_URL" \
  --arg ts "$TS" \
  '{
    embeds: [{
      title: ("Telas — atualização: " + $app),
      fields: [
        { name: "Versão (manifest)", value: ("`" + $ver + "`"), inline: true },
        { name: "Commit", value: ("`" + $sha + "`"), inline: true },
        { name: "Branch", value: ("`" + $ref + "`"), inline: true },
        { name: "Autor", value: $author, inline: true },
        { name: "Repositório", value: $repo, inline: true },
        { name: "Workflow", value: ("[abrir execução](" + $url + ")"), inline: true },
        { name: "Mensagem do commit", value: (if (($msg | length) > 1024) then (($msg | .[0:1021]) + "…") else $msg end), inline: false }
      ],
      color: 3066993,
      footer: { text: "GitHub Actions" },
      timestamp: $ts
    }]
  }')

curl -sS -X POST "$DISCORD_DEPLOY_WEBHOOK_URL" \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD"

echo "Notificação Discord enviada."
