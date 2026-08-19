# Deploy en un Cloud Server de DonWeb

Guía para poner Padel Hub entero en una VPS con Docker. Está escrita para seguirse de arriba abajo
la primera vez; después, para volver a mirar un paso suelto.

## Lo que vas a levantar

Cuatro contenedores en una red interna. **Sólo Caddy ve internet**; MySQL no publica ningún puerto.

| contenedor | qué hace |
|---|---|
| `caddy` | recibe todo el tráfico, resuelve HTTPS y reparte según la URL |
| `front` | Angular con SSR (Node) |
| `back` | el JAR de Spring Boot |
| `mysql` | la base, con volumen persistente |

El reparto lo hace Caddy por prefijo: `/api`, `/public` y `/platform` van al back, y **todo lo demás**
al front. Como el navegador habla siempre con un solo dominio, no hay CORS en el medio.

---

## 1. Crear el Cloud Server

En el panel de DonWeb, un Cloud Server con:

- **Imagen: Docker** (viene con Docker y Docker Compose ya instalados)
- **8 GB de RAM** — la app usa ~2 GB; el resto es para compilar Angular y Maven en el server
- Anotá el **IP público** que te asignen

## 2. Entrar por SSH

Desde tu PC:

```bash
ssh root@TU_IP
```

La primera vez te va a preguntar si confiás en la huella del servidor; escribí `yes`.

**Antes de seguir, dejá de entrar con contraseña.** Un server con IP pública recibe intentos de
login automatizados desde el primer día. Desde *tu PC*:

```bash
ssh-keygen -t ed25519 -C "padel-hub"
ssh-copy-id root@TU_IP
```

Probá que entrás sin que te pida contraseña, y recién ahí, en el server:

```bash
sed -i 's/^#*PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config
systemctl restart ssh
```

> No cierres la sesión actual hasta comprobar en OTRA terminal que seguís entrando. Si algo salió
> mal, esa sesión abierta es lo único que te queda para arreglarlo.

## 3. Traer el código

Los dos repos van **uno al lado del otro**: el compose referencia al front por ruta relativa.

```bash
mkdir -p /opt/padel-hub && cd /opt/padel-hub
git clone https://github.com/412017-PILLADO/padelHubBack.git padelBack
git clone https://github.com/412017-PILLADO/padelHubFront.git padelFront
```

## 4. Completar los secretos

```bash
cd /opt/padel-hub/padelBack/deploy
cp .env.example .env
nano .env
```

Las contraseñas **generalas, no las inventes de memoria**:

```bash
openssl rand -base64 24   # para MYSQL_ROOT_PASSWORD y MYSQL_PASSWORD
openssl rand -base64 48   # para PADEL_JWT_SECRET
```

Las de Mercado Pago dejalas vacías por ahora: sin ellas el módulo de pagos queda apagado y la app
funciona igual con señas por transferencia.

## 5. Apuntar el dominio

En el DNS de `padel-hub.com.ar`, dos registros hacia el IP del server:

| tipo | nombre | valor |
|---|---|---|
| A | `@` | TU_IP |
| A | `*` | TU_IP |

El **wildcard (`*`) es el que hace funcionar a los clubes**: cualquier `loquesea.padel-hub.com.ar`
resuelve al server, y ahí Caddy decide si le corresponde certificado preguntándole al back.

> Esperá a que el DNS propague antes del paso siguiente. Comprobalo con
> `nslookup demo.padel-hub.com.ar` desde tu PC: tiene que devolver el IP del server. Si levantás
> Caddy antes, va a intentar sacar el certificado, fallar, y consumir intentos de Let's Encrypt.

## 6. Abrir el firewall

Sólo estos dos puertos hacia afuera (más el 22 de SSH, que ya está):

```bash
ufw allow 80/tcp && ufw allow 443/tcp && ufw allow 22/tcp && ufw enable
```

## 7. Levantar

```bash
cd /opt/padel-hub/padelBack/deploy
docker compose -f docker-compose.prod.yml up -d --build
```

La primera vez tarda varios minutos: compila el back con Maven y el front con Angular.

```bash
docker compose -f docker-compose.prod.yml ps          # los cuatro en "running"/"healthy"
docker compose -f docker-compose.prod.yml logs -f back
```

## 8. Comprobar que anda

```bash
curl -I https://padel-hub.com.ar                      # 200, y con certificado válido
curl https://padel-hub.com.ar/public/tenant/existe?domain=padel-hub.com.ar   # 200
```

Y en el navegador: entrá a `https://padel-hub.com.ar/plataforma`, logueate con
`PADEL_PLATFORM_ADMIN_EMAIL` y la contraseña que pusiste, y **dá de alta el primer club**. La base
arranca vacía a propósito — no hay club de demostración en producción.

Con el club creado, entrá a `https://<slug>.padel-hub.com.ar`: la primera visita puede demorar unos
segundos porque Caddy está sacando el certificado de ese subdominio en ese momento.

## 9. Backup diario

```bash
crontab -e
```

Agregá:

```
15 4 * * * /opt/padel-hub/padelBack/deploy/backup.sh >> /var/log/padel-backup.log 2>&1
```

**Probá la restauración una vez**, ahora que no hay datos que perder — un backup que nunca se
restauró es una suposición, no un respaldo:

```bash
./backup.sh
./restaurar.sh /var/backups/padel-hub/padeldb-<fecha>.sql.gz
```

Y bajate los dumps cada tanto a tu PC. Desde *tu PC*:

```bash
scp root@TU_IP:/var/backups/padel-hub/padeldb-*.sql.gz ~/backups-padel/
```

> El snapshot semanal que incluye DonWeb cubre "se murió la máquina". Estos dumps cubren "necesito
> los datos de ayer", y además son portables: los restaurás en cualquier MySQL.

---

## Actualizar a una versión nueva

```bash
cd /opt/padel-hub/padelBack && git pull
cd ../padelFront && git pull
cd ../padelBack/deploy && docker compose -f docker-compose.prod.yml up -d --build
```

Flyway aplica solo las migraciones nuevas al arrancar. **Hacé un backup antes** si la versión trae
migraciones que tocan datos.

## Cuando el disco se llene

Los 20 GB se consumen con imágenes viejas y cachés de compilación:

```bash
docker system df        # ver en qué se fue
docker system prune -a  # borrar lo que no está en uso (no toca los volúmenes)
```

`docker system prune` **no borra volúmenes** salvo que le pases `--volumes`. No se lo pases: ahí
viven la base y los certificados.

## Prender Mercado Pago

Necesita el sitio ya publicado con HTTPS, que es lo que acabás de hacer. Completá las cuatro
variables `PADEL_MP_*` y `PADEL_CRYPTO_KEY` en el `.env`, y reiniciá:

```bash
docker compose -f docker-compose.prod.yml up -d back
```

Cada club conecta su propia cuenta desde su panel, en Configuración → Cobros.
