#!/bin/sh
# Backup diario de la base. Genera un .sql comprimido y borra los que pasaron la retención.
#
# QUÉ CUBRE Y QUÉ NO. El snapshot semanal que incluye DonWeb cubre "se murió la máquina". Esto cubre
# lo otro: "necesito los datos de ayer" — un club que borró turnos por error, una migración que
# salió mal. Y a diferencia del snapshot, el archivo es portable: lo restaurás en cualquier MySQL.
#
# NO SIRVE DE NADA SI SE QUEDA EN EL SERVIDOR. Un backup en el mismo disco que la base es una copia,
# no un backup: si el disco muere, se va con todo. Bajate los dumps cada tanto con `descargar.sh`.
#
# Instalación (una vez, en el server):
#   crontab -e
#   15 4 * * * /opt/padel-hub/padelBack/deploy/backup.sh >> /var/log/padel-backup.log 2>&1
# (4:15 AM, cuando no hay nadie reservando)

set -eu

DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
DESTINO="${PADEL_BACKUP_DIR:-/var/backups/padel-hub}"
RETENCION_DIAS="${PADEL_BACKUP_RETENCION:-7}"

# Las credenciales salen del mismo .env que usa el stack: una sola fuente de verdad.
. "$DEPLOY_DIR/.env"

mkdir -p "$DESTINO"
ARCHIVO="$DESTINO/padeldb-$(date +%Y%m%d-%H%M%S).sql.gz"

# --single-transaction: toma una foto consistente SIN bloquear las tablas, así un backup a las 4 AM
# no le corta una reserva a alguien que esté sacando turno a esa hora.
# --no-tablespaces: MySQL 8 le pide el privilegio PROCESS a mysqldump para volcar info de
# tablespaces, y el usuario de la app no lo tiene ni debería. Sin esta opción el dump igual se
# completa, pero escupe un "Access denied ... PROCESS" que ensucia el log del cron todas las
# noches — y un error que se ve siempre es un error que se deja de leer.
docker compose -f "$DEPLOY_DIR/docker-compose.prod.yml" exec -T mysql \
	mysqldump --single-transaction --quick --no-tablespaces \
	-u"${MYSQL_USER:-padel}" -p"${MYSQL_PASSWORD}" "${MYSQL_DATABASE:-padeldb}" \
	| gzip > "$ARCHIVO"

# Un backup roto que parece backup es peor que no tenerlo. Y el tamaño NO alcanza para saberlo: un
# dump cortado a la mitad pesa de sobra. Tampoco sirve el código de salida, porque en
# `mysqldump | gzip` el que ve `set -e` es el de gzip, que sale bien aunque mysqldump haya muerto.
#
# Lo único que prueba que el dump llegó hasta el final es su última línea: mysqldump escribe
# "-- Dump completed on ..." recién cuando termina. Si no está, el archivo se borra y el comando
# falla, así el cron lo reporta en vez de dejar una copia inservible que nadie mira hasta que la
# necesita.
if [ ! -s "$ARCHIVO" ] || ! gunzip -c "$ARCHIVO" 2>/dev/null | tail -5 | grep -q -- '-- Dump completed'; then
	rm -f "$ARCHIVO"
	echo "$(date '+%F %T') ERROR: el dump no llegó a completarse; no se guardó nada" >&2
	exit 1
fi

echo "$(date '+%F %T') OK: $ARCHIVO ($(du -h "$ARCHIVO" | cut -f1))"

# Rotación: los 20 GB de disco no son infinitos.
find "$DESTINO" -name 'padeldb-*.sql.gz' -type f -mtime "+$RETENCION_DIAS" -delete
echo "$(date '+%F %T') retención: quedan $(find "$DESTINO" -name 'padeldb-*.sql.gz' | wc -l) copias"
