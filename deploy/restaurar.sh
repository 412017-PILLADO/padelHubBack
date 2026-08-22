#!/bin/sh
# Restaura un dump sobre la base. Es la mitad del backup que nadie prueba hasta que la necesita —
# probala UNA VEZ antes de que haga falta de verdad.
#
#   ./restaurar.sh /var/backups/padel-hub/padeldb-20260817-041500.sql.gz
#
# PISA LO QUE HAY. El dump trae DROP TABLE + CREATE de cada tabla, así que la base queda exactamente
# como estaba cuando se tomó: todo lo posterior se pierde. Por eso pide confirmación.

set -eu

DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
ARCHIVO="${1:-}"

if [ -z "$ARCHIVO" ] || [ ! -f "$ARCHIVO" ]; then
	echo "Uso: $0 <archivo.sql.gz>" >&2
	echo "Disponibles:" >&2
	ls -1t "${PADEL_BACKUP_DIR:-/var/backups/padel-hub}"/padeldb-*.sql.gz 2>/dev/null | head -10 >&2
	exit 1
fi

. "$DEPLOY_DIR/.env"

echo "Vas a restaurar:  $ARCHIVO"
echo "Sobre la base:    ${MYSQL_DATABASE:-padeldb}"
echo "Todo lo que haya pasado DESPUÉS de ese backup se pierde (reservas, clubes nuevos, todo)."
printf "Escribí SI para continuar: "
read -r RESPUESTA
[ "$RESPUESTA" = "SI" ] || { echo "Cancelado."; exit 1; }

gunzip -c "$ARCHIVO" | docker compose -f "$DEPLOY_DIR/docker-compose.prod.yml" exec -T mysql \
	mysql -u"${MYSQL_USER:-padel}" -p"${MYSQL_PASSWORD}" "${MYSQL_DATABASE:-padeldb}"

echo "Restaurado. Reiniciá el back para que no siga con datos viejos en memoria:"
echo "  docker compose -f $DEPLOY_DIR/docker-compose.prod.yml restart back"
