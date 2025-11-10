# =====================================
# 🐳 Makefile para ambiente Docker Compose
# =====================================

# UID e GID do usuário local (garante permissões corretas nos volumes)
UID := $(shell id -u)
GID := $(shell id -g)
USER := $(shell whoami)

# Diretórios de volumes locais
VOLUMES = loki_data prometheus_data grafana_data minio_data

# =====================================
# 🧩 Comandos principais
# =====================================

## Sobe todos os containers (garante permissões corretas antes)
up: fix-perms
	@echo "🔼 Subindo containers..."
	@export UID=$(UID) GID=$(GID); docker compose up -d

## Para e remove containers, mantendo volumes
down:
	@echo "🛑 Parando containers..."
	@docker compose down

## Para e remove tudo (containers, volumes e redes)
clean:
	@echo "🧹 Removendo containers, volumes e redes..."
	@docker compose down -v
	@echo "🗑️  Limpando diretórios locais..."
	@sudo rm -rf $(VOLUMES)

## Corrige permissões e cria diretórios de volume, se não existirem
fix-perms:
	@echo "🔧 Corrigindo permissões de volumes..."
	@for dir in $(VOLUMES); do \
		mkdir -p ./$$dir; \
		sudo chown -R $(USER):$(USER) ./$$dir; \
	done
	@echo "✅ Permissões corrigidas (usuário: $(USER))"

## Mostra o status dos containers
ps:
	@docker compose ps

## Mostra os logs em tempo real
logs:
	@docker compose logs -f

# =====================================
# 📘 Ajuda (executar: make help)
# =====================================
help:
	@echo ""
	@echo "Comandos disponíveis:"
	@echo "  make up         - Corrige permissões e sobe os containers"
	@echo "  make down       - Para os containers (mantém volumes)"
	@echo "  make clean      - Remove containers, volumes e diretórios"
	@echo "  make fix-perms  - Corrige permissões dos volumes"
	@echo "  make ps         - Mostra status dos containers"
	@echo "  make logs       - Mostra logs em tempo real"
	@echo ""
