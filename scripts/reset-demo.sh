#!/usr/bin/env bash
# Resetea la base demo a un estado limpio: borra el volumen de MySQL y lo recrea.
# Flyway re-aplica las migraciones (schema V1 + seed V2: tenant `demo`, 1 complejo, 3 canchas,
# horarios 08:00-23:00) la próxima vez que arranca el backend; el owner lo recrea OwnerSeeder.
#
# Uso:  bash scripts/reset-demo.sh
# Luego: arrancar/reiniciar el backend para que corra Flyway + OwnerSeeder.
set -euo pipefail

cd "$(dirname "$0")/.."

echo "› Bajando MySQL y borrando el volumen…"
docker compose down -v

echo "› Levantando MySQL limpio…"
docker compose up -d

echo "› Esperando a que MySQL esté healthy…"
for i in $(seq 1 40); do
  status="$(docker inspect -f '{{.State.Health.Status}}' padel-mysql 2>/dev/null || echo starting)"
  if [ "$status" = "healthy" ]; then
    echo "✓ MySQL healthy. Datos demo listos para re-seedear al arrancar el backend."
    exit 0
  fi
  sleep 2
done

echo "✗ MySQL no llegó a healthy a tiempo." >&2
exit 1
