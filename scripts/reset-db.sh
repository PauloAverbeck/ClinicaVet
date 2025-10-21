#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   ./scripts/reset-db.sh
# Env overrides (optional):
#   SERVICE=db DB=vet DB_USER=postgres ./scripts/reset-db.sh

SERVICE="${SERVICE:-db}"
DB="${DB:-vet}"
DB_USER="${DB_USER:-postgres}"

echo "🔄 Truncating tables on service '${SERVICE}', database '${DB}'..."
docker compose exec -T "$SERVICE" psql -U "$DB_USER" -d "$DB" -v ON_ERROR_STOP=1 -c \
"TRUNCATE TABLE animal, app_user, attendance, company, person, task, user_company RESTART IDENTITY CASCADE;"

echo ""
echo "✅ Counts after truncate (should all be 0):"
docker compose exec -T "$SERVICE" psql -U "$DB_USER" -d "$DB" -A -t -c \
"SELECT 'animal' AS table, COUNT(*) FROM animal UNION ALL
 SELECT 'app_user', COUNT(*) FROM app_user UNION ALL
 SELECT 'attendance', COUNT(*) FROM attendance UNION ALL
 SELECT 'company', COUNT(*) FROM company UNION ALL
 SELECT 'person', COUNT(*) FROM person UNION ALL
 SELECT 'task', COUNT(*) FROM task UNION ALL
 SELECT 'user_company', COUNT(*) FROM user_company;"

echo ""
echo "Reset completo!"
