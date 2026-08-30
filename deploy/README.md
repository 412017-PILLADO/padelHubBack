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

**`ACME_EMAIL` tiene que ser un mail tuyo de verdad**, no el del ejemplo. El compose sólo verifica
que la variable exista, así que un placeholder pasa el arranque sin quejarse y los certificados se
emiten igual — la diferencia aparece el día que una renovación falla: Let's Encrypt avisa por mail
antes de que el certificado venza, y ese aviso se pierde. Te enterarías por un cliente contando que
el navegador le muestra una advertencia roja. Para ver con qué cuenta estás emitiendo hoy:

```bash
docker compose -f docker-compose.prod.yml logs caddy | grep -o '"account_contact":\[[^]]*\]' | tail -1
```

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

## Alta de un club — checklist

1. Panel de plataforma (`/plataforma`, con el super-admin) → **"+ Nuevo club"**. Crea tenant,
   dominios, complejo base, canchas, horarios y el usuario OWNER en una sola transacción.
2. **Reiniciá Caddy antes de entrar al subdominio del club nuevo, no después:**

   ```bash
   cd /opt/padel-hub/padelBack/deploy && docker compose -f docker-compose.prod.yml restart caddy
   ```

   El motivo está en la sección de abajo ("Un subdominio no levanta con HTTPS"), pero la regla
   corta es: si alguien —vos incluido— visita `slug.padel-hub.com.ar` ANTES de este paso, Caddy le
   pregunta al back si el club existe, el back todavía dice que no, y esa respuesta queda anotada
   sin vencimiento. Reiniciar después de crear el club, y antes de la primera visita, evita pisar
   esa trampa.
3. Confirmá que el subdominio levanta:

   ```bash
   curl -sS -o /dev/null -w "%{http_code}\n" --max-time 40 "https://slug-del-club.padel-hub.com.ar"
   ```

   `200` = listo. Si no, ver "Un subdominio no levanta con HTTPS" más abajo.
4. Pasale al dueño su email y contraseña. Entra por `https://padel-hub.com.ar` → "Ingresar" — el
   login es uno solo para todos los clubes, no hay URL de login por subdominio.

---

## Actualizar a una versión nueva

```bash
cd /opt/padel-hub/padelBack && git pull
cd ../padelFront && git pull
cd ../padelBack/deploy && docker compose -f docker-compose.prod.yml up -d --build
```

Flyway aplica solo las migraciones nuevas al arrancar. **Hacé un backup antes** si la versión trae
migraciones que tocan datos.

Ojo con una cosa: `--build` recrea `front` y `back`, pero **no toca a `caddy`** — su imagen no
cambia. Si el deploy arregla algo de lo que Caddy depende (el portero de certificados, por ejemplo),
Caddy no se entera hasta que lo reinicies. Ver la sección siguiente.

## Un subdominio no levanta con HTTPS

Síntoma: `algo.padel-hub.com.ar` no abre, el navegador habla de un problema de certificado, y en los
logs de Caddy **no hay ningún error** — no hay ni un intento de emisión que leer.

La causa casi siempre es la misma. Caddy emite certificados a demanda y antes de cada emisión le
pregunta al back si ese host corresponde a un club existente (`on_demand_tls ask` en el `Caddyfile`
→ `/public/tenant/existe`). **Esa respuesta se la guarda.** Si alguien visitó el subdominio *antes*
de que el club existiera, Caddy preguntó, le dijeron que no, y se lo anotó: cuando después creás el
club, el portero ya responde que sí pero Caddy nunca vuelve a preguntar. El silencio en el log es
justamente eso — no hay intento, así que no hay nada que loguear.

Se destraba reiniciando Caddy, que descarta lo que tenía anotado y vuelve a preguntar:

```bash
cd /opt/padel-hub/padelBack/deploy && docker compose -f docker-compose.prod.yml restart caddy
```

Es una operación barata y sin riesgo: los certificados y las claves de cuenta viven en el volumen
`caddy-data`, no en el contenedor, así que no se pierde ni se re-emite nada de lo que ya funciona.
Después entrá al subdominio y confirmá que la emisión ocurrió:

```bash
docker compose -f docker-compose.prod.yml logs caddy --since 2m | grep -v http.log.access | grep -iE "obtain|challenge"
```

Tenés que ver `obtaining certificate` y, unos segundos más tarde, `certificate obtained
successfully`.

> **Al dar de alta un club, entrá a su subdominio recién después de crearlo.** Visitarlo antes es
> exactamente lo que deja la negativa anotada, y convierte un alta de dos minutos en media hora de
> buscar un error que no existe.

**Antes de reiniciar, descartá que sea otra cosa.** Si el portero responde que no, reiniciar no
arregla nada — el club no existe, está INACTIVE, o el host tiene más de un nivel (ver abajo):

```bash
curl -s -o /dev/null -w "%{http_code}\n" "https://padel-hub.com.ar/public/tenant/existe?domain=EL_HOST"
```

`200` = el portero autoriza y el problema es la caché de Caddy → reiniciá. `404` = el problema está
en los datos, y reiniciar sólo te hace perder el tiempo.

### Los subdominios son de un solo nivel

`rozapadel.padel-hub.com.ar` funciona; `www.rozapadel.padel-hub.com.ar` **no**, y es a propósito. Las
tres capas coinciden en la regla: el `Caddyfile` usa `*.padel-hub.com.ar`, que en Caddy matchea una
sola etiqueta; `TlsAskController` rechaza cualquier slug que contenga un punto; y el front resuelve
el tenant sólo del primer nivel. El `www` del apex es la única excepción, y está permitida
explícitamente porque es la landing de venta.

No es una limitación que valga la pena levantar: nadie tipea `www.<club>`, y hacerlo andar
significaría emitir certificados para hosts de dos niveles que ningún cliente usa. Si algún club
quiere un dominio propio (`turnos.suclub.com`), el camino es cargarlo en `tenant_dominios` desde el
panel de plataforma — eso sí está contemplado y el portero lo autoriza.

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
