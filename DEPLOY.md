# Deploy — Padel Hub

SaaS multi-tenant de reserva de canchas de pádel. Dos artefactos: **backend** (`padelBack`, Spring
Boot + MySQL) y **frontend** (`padelFront`, Angular SSR estático). Se despliegan en orígenes
separados; el front le pega al back por `apiBase` y resuelve el tenant por subdominio (`X-Tenant`).

## Backend (`padelBack`)

Jar ejecutable de Spring Boot. Build:

```bash
cd padelBack
./mvnw clean package -DskipTests      # los tests necesitan Docker (Testcontainers); ver más abajo
java -jar target/*.jar
```

O con Docker (hay `padelBack/Dockerfile`):

```bash
docker build -t padel-back ./padelBack
docker run -p 8080:8080 --env-file padelBack/.env padel-back
```

### Variables de entorno

| Var | Default (dev) | Notas |
|-----|---------------|-------|
| `PORT` | `8080` | La plataforma (Railway/Render) suele inyectarlo. |
| `DB_URL` | `jdbc:mysql://localhost:3306/padeldb?...` | JDBC del MySQL gestionado. |
| `DB_USERNAME` / `DB_PASSWORD` | `padel` / `padel` | Credenciales de la DB. |
| `DB_POOL_SIZE` | ver `application.yml` | Tamaño del pool de conexiones (Hikari). Subirlo si hay muchos tenants concurrentes. |
| `PADEL_JWT_SECRET` | secret de dev | **Obligatorio en prod** (≥32 chars, HS256). |
| `PADEL_JWT_EXPIRATION` | `2592000000` (30 d) | Vida del token, en ms. |
| `PADEL_OWNER_EMAIL` | `owner@padelhub.com` | Email del owner sembrado. **Sólo aplica fuera de prod:** el `OwnerSeeder` engancha el owner al tenant cuyo dominio es `localhost`, que lo crea el seed de demo — y en prod ese seed no se carga. |
| `PADEL_OWNER_PASSWORD` | — | **No hace falta en prod.** Sin el club de demo no hay a qué tenant engancharlo, así que el seeder no siembra nada aunque la definas. Los owners reales los crea el alta de club en `/plataforma`. |
| `PADEL_PLATFORM_ADMIN_EMAIL` | `admin@padelhub.com` | Email del super-admin de plataforma (panel `/plataforma`). |
| `PADEL_PLATFORM_ADMIN_PASSWORD` | — | **Obligatorio en prod** con `SPRING_PROFILES_ACTIVE=prod`: sin esto, no se siembra ningún super-admin y el panel `/plataforma` queda sin usuario para entrar. |
| `padel.platform.admin-key` | — | Opcional: clave fija para pegarle a `/platform/**` con `X-Platform-Key` desde scripts/CI, sin pasar por login. No usar en vez del JWT del panel; solo para automatización. |
| `PADEL_ZONA_NEGOCIO` | `America/Argentina/Cordoba` | Zona horaria de negocio (horarios, reservas, reportes). |
| `PADEL_TRUST_PROXY` | `false` | Poné `true` si hay un proxy/CDN delante que setea `X-Forwarded-For`; si no, el rate-limit por IP usaría siempre la IP del proxy. |
| `PADEL_CORS_ALLOWED_ORIGIN_PATTERNS` | `http://localhost:*,http://*.localhost:*` | **Obligatoria en prod.** El default es de dev: si no la ponés, el back rechaza con 403 `Invalid CORS request` todo POST/PUT/DELETE que venga de un navegador. Poné el apex Y el comodín: `https://tuapp.com,https://*.tuapp.com` — el patrón con asterisco no cubre el apex, y ahí vive el panel de plataforma. |
| `SPRING_PROFILES_ACTIVE` | — | Poné `prod` en producción. Además de Flyway, activa `server.forward-headers-strategy=FRAMEWORK`, sin la cual Spring no se entera de que el TLS lo terminó el proxy. |

Flyway aplica el schema en el primer arranque. **En prod la base queda vacía**: el club de
demostración vive en una ubicación aparte (`db/seed`) que sólo cargan dev y los tests, así que a un
cliente no se le instala un complejo falso. Lo fija `application-prod.yml` y lo cuida
`SeedFueraDeProduccionTest`.

Para un tenant productivo real **no hace falta tocar la base a mano**: el alta de clubes es por el
panel de plataforma (`/plataforma`,
protegido con el super-admin de arriba) → formulario "+ Nuevo club". Crea tenant, dominios, complejo
base, canchas, horarios y el usuario OWNER en una sola transacción, y el subdominio queda resuelto al
instante (por slug vía header `X-Tenant`, o por host si se cargó un dominio propio). Si el club
pierde el acceso, el mismo panel tiene un botón para resetear la password del owner.

## Frontend (`padelFront`)

Angular 21 con SSR. Build estático + servidor Node:

```bash
cd padelFront
npm ci
npm run build                          # genera dist/padel-hub (browser + server)
node dist/padel-hub/server/server.mjs  # SSR; o servir dist/padel-hub/browser como estático
```

Ajustar `src/environments/environment.prod.ts`:

```ts
export const environment = {
  production: true,
  apiBase: 'https://api.padel-hub.com.ar', // URL pública del backend
  baseDomain: 'padel-hub.com.ar',          // lo que va DESPUÉS del subdominio del tenant
};
```

### Multi-tenant por subdominio

El tenant se deriva del subdominio del host: `demo.padel-hub.com.ar → X-Tenant: demo`; el apex
(`padel-hub.com.ar`) muestra la landing de marketing. En el DNS, apuntar un **wildcard**
`*.padel-hub.com.ar` al hosting del front. El backend acepta CORS desde
`https://*.padel-hub.com.ar` (config `padel.cors.allowed-origin-patterns`).

## Tests antes de deployar

```bash
cd padelBack && ./mvnw verify          # unit + integración (Testcontainers; requiere Docker)
cd padelFront && npm run build         # build verde
# E2E contra stack real:
cd padelFront && npx playwright test   # ver README de tests / playwright.config.ts
```

## Smoke tras el deploy

`scripts/smoke.*` corre los chequeos de humo (`@smoke`) contra el entorno. Ver `scripts/`.

## Mercado Pago (señas online)

Variables de entorno del back:

| Variable | Qué es |
|---|---|
| `PADEL_CRYPTO_KEY` | Base64 de 32 bytes (`openssl rand -base64 32`). Cifra los tokens OAuth en reposo. **Rotarla invalida las conexiones existentes.** |
| `PADEL_MP_CLIENT_ID` / `PADEL_MP_CLIENT_SECRET` | Credenciales de la aplicación de plataforma creada en developers.mercadopago.com (tipo "Pagos online", modelo marketplace). |
| `PADEL_MP_REDIRECT_URI` | `https://<api>/public/pagos/mp/oauth/callback` — debe estar registrada EXACTA en la app de MP. |
| `PADEL_MP_WEBHOOK_BASE_URL` | `https://<api>` (sin path). Vacía = las preferencias se crean sin webhook (dev). |

Flujo por tenant: el dueño clickea "Conectar Mercado Pago" en Configuración → autoriza en MP →
las señas nuevas se cobran por Checkout Pro y el webhook confirma la reserva sola. Sin conexión,
todo sigue funcionando con el alias por transferencia + validación manual.

Prueba end-to-end real (staging): requiere cuentas de prueba de MP (vendedor + comprador) y URL
pública https para el webhook (en dev: túnel tipo cloudflared/ngrok apuntando al :8095).

> **Deploy checklist — rate limit:** detrás de un proxy (Railway) setear `PADEL_TRUST_PROXY=true`, si no el throttle de escrituras públicas (y el anti-abuso de reservas por IP) clavan todas las requests en la IP del proxy: o bloquean a todos juntos o no sirven. Verificar que el proxy reescriba X-Forwarded-For de un solo salto.
