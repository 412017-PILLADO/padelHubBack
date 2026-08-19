# Padel Hub · Estado del trabajo de plantillas y qué sigue

**Fecha:** 2026-08-17 (cierre; revisa el documento del 2026-08-16)
**Escrito para:** quien retome esto sin haber estado en la sesión anterior.

---

## ✅ CERRADO · la fase de plantillas está en `main` y pusheada

**El rediseño de C se terminó, se revisó y se mergeó en los dos repos.** No queda trabajo a medias.

| repo | qué entró | estado |
|---|---|---|
| `padelBack` | `feat/plantilla-default-explicita` (V18 + la default del alta) | mergeada, rama borrada, **pusheada** |
| `padelBack` | `fix/plantilla-default-columna` (V19, el default de la columna) | mergeada, rama borrada, **pusheada** |
| `padelFront` | `feat/plantilla-c-basica` (19 commits) | mergeada, rama borrada, **pusheada** |

Puertas sobre `main`: back `mvnw verify` **54 unit + 56 IT** · front `npm test` **287** ·
`npm run build` limpio **501,67 kB** · `npx playwright test e2e` **24/24**.

**La default de producto vive en TRES lugares y los tres ya están de acuerdo**: `PLANTILLA_DEFAULT`
(front), `DEFAULT_PLANTILLA` (back, `TenantProvisioningService`) y el `DEFAULT` de la columna
`tenants.plantilla` (V19). El tercero está dormido —el INSERT del provisioning nombra la columna
siempre— y por eso pudo quedar desalineado sin que nada fallara: el día que aparezca otro camino que
inserte un tenant sin nombrar la columna, el club nacería con lo que diga el esquema.

La única rama que sigue viva es `feat/plantilla-d-cancha`, **a propósito**: decisión del owner de
dejarla por si algún día se retoma o refactoriza D (ver §1 y §3).

### Las tres fallas del e2e: eran de ENTORNO, no de la rama

`precio-franjas`, `reserva` y `sena` fallaban porque el complejo de `demo` tenía
**`autoasignacion = 1`**. Con ese flag el flujo saltea el paso de cancha (`@if (showCancha())` en
`booking-flow.html`), así que `.ccard.any` no se dibuja nunca y las tres specs que pasan por ahí no
pueden pasar. Probado: flag ON → 3 failed · flag OFF → 3 passed, mismo código y misma base.

`plantillas.spec` usaba el mismo helper y pasaba porque apunta a los otros tenants, que tienen el
flag en 0 — de ahí el reparto exacto 20/3. Las otras dos hipótesis quedaron descartadas con datos:
cero bloqueos futuros y horario 08:00–23:00 los siete días (y `horarios_complejo` tiene
`hora_inicio`/`hora_fin`, no `apertura`/`cierre`).

**Por qué costó tanto:** `elegirDiaYSlot` tiraba *"ningún día de la semana tiene slots libres"* aunque
la falla real fuera la aserción de `.ccard.any`, y esa frase mandó una sesión entera a buscar
agotamiento de datos. Ya no: el helper distingue los dos modos y nombra la causa conocida con el PUT
que la revierte.

> **Si volvés a ver estas tres specs en rojo, mirá primero `autoasignacion` de `demo`.** Alguien la
> prende desde el panel para probar y queda puesta; `config.spec` la re-guarda tal como está.

### Los tres desvíos del brief de la tarea 8: verificados, los tres correctos

El más importante confirmó que **el plan estaba mal**: el lomo es `:host::before` de `app-shell-c`
(clase `.tpl-c`), mientras que `[data-tpl]` vive en el host de `app-landing`. Medir sobre
`[data-tpl="C"]` da `NaN`. El spec quedó con el selector correcto y el porqué escrito al lado.

### Lo que encontró el review de rama, que nunca se había hecho

Seis hallazgos, los seis arreglados antes del merge. Dos valen contarlos:

1. **La default no le llegaba a ningún club nuevo.** "C es la default" estaba implementado sólo en el
   fallback de `normalizarPlantilla()`, que actúa ante un valor desconocido — y el alta nunca deja uno
   desconocido: el back estampaba `DEFAULT_PLANTILLA = "A"` y el form del panel dev mandaba `'A'`.
   Verificado contra el back real: alta por API sin plantilla devolvía `"plantilla":"A"`. O sea que la
   `V18` —el motivo del "primero el back, el orden no es negociable"— protegía contra un cambio que no
   le llegaba a nadie. Y la puerta e2e que debía atajarlo era una **tautología**: creaba el club en B,
   lo editaba a C y recién ahí afirmaba que un club nuevo sale en C.
2. **El lomo se borraba en su mitad de abajo.** Es un degradado y los tests medían sólo el primer
   stop; el segundo mezclaba hacia `--paper` y daba, contra el papel: teal 2,01 · naranja 1,72 ·
   amarillo 1,19 · **casi blanco 1,02** · fucsia 2,08. La firma de C desaparecía con cinco de las seis
   paletas — el modo de falla exacto que hundió a D, adentro de la plantilla que existe para no
   tenerlo, con la suite en verde. Ahora el degradado va hacia la tinta (30%, peor caso 6,32) y hay
   puerta sobre el segundo stop.

La lección que dejan los dos: **medir un extremo y creer que se midió el conjunto**. Pasó con el
degradado (un stop de dos) y con la default (un fallback de tres lugares).

### Dónde está todo

| qué | dónde |
|---|---|
| El ledger con el detalle tarea por tarea | `padelFront/.superpowers/sdd/progress-c.md` |
| La spec | `padelFront/docs/superpowers/specs/2026-08-16-plantilla-c-basica-design.md` |
| El plan | `padelFront/docs/superpowers/plans/2026-08-16-plantilla-c-basica.md` |
| Los reportes de cada tarea | `padelFront/.superpowers/sdd/task-N-report.md` |

⚠️ **`.superpowers/` está gitignoreado**: el ledger y los reportes viven **sólo en esta máquina**. Si
se pierden, se pierde el registro de las mediciones y de por qué se decidió cada cosa. El `git log`
sobrevive; el resto no.

### Cómo se venía ejecutando

Con **subagent-driven-development**: un subagente implementador por tarea, un reviewer después de
cada una, y un review de rama entera al final. El proceso encontró cosas que ningún test veía — un
tripwire infalseable, una deuda pagada 1 de 3, un ratio medido con la tinta equivocada. Conviene
seguir con el mismo método y no terminar a mano.

**El review de rama, que era el que faltaba, se hizo el 2026-08-17 y fue el que más encontró** (seis
hallazgos, dos de ellos con la suite entera en verde). Vale como argumento para no saltearlo: las
revisiones por tarea miran el cambio de esa tarea, y los dos defectos serios estaban justamente en la
juntura entre tareas — un valor que la tarea 5 escribió y la tarea 7 pineó a medias, y una decisión
de producto repartida entre dos repos.

Este documento cubre el trabajo sobre las **plantillas visuales de la landing pública** (el
white-label: cada club ve su landing con su color, su logo y su plantilla). No cubre el resto del
producto.

---

## 0. Estado de `main` al 2026-08-16 · todo lo ANTERIOR a C está mergeado

> Ojo con la fecha: esta sección describe `main` **antes** del rediseño de C, que se mergeó el
> 2026-08-17. Los números de acá quedaron viejos a propósito, como foto de aquel momento; los
> vigentes están arriba de todo.

**`main` está verde.** Del trabajo anterior a C no queda nada esperando en una rama.

| puerta en `main` | resultado |
|---|---|
| `npm test` | **272 passed**, 25 archivos |
| `npm run build` | limpio, **501,67 kB** (presupuesto en 550) |
| `npx playwright test e2e` | **23 passed** |

Lo que entró en este cierre, en tres merges:

1. **`fix/filo-cta-a11y`** — el filo del CTA para las cuatro cáscaras (§3-bis). Su e2e, que era la
   única puerta que le faltaba, se corrió y dio **21**.
2. **`feat/galeria-plantillas`** — la galería del panel, el preview vivo, y las dos secciones nuevas
   de marketing (§4-bis y §4-ter).
3. **`fix/cierre-panel-y-mobile`** — la revisión visual del panel, que encontró tres problemas que
   ningún test veía (§4-quater), más el ocultamiento de las cards en mobile.

**Las dos ramas mergeadas se pueden borrar.** `feat/plantilla-d-cancha` sigue existiendo y es
decisión del owner qué hacer con ella (ver §1).

**Lo único que quedaba abierto de la spec era el rediseño de C (§4.2)**, que el owner pidió
explícitamente — *"si me gustaría algún refactor de C, la verdad que no me gusta mucho"*— y que
**arrancó al día siguiente y quedó a medias**: está en su rama, no en `main`. Ver el bloque del
principio y la §10.

---

## 1. Decisión del owner, y es lo primero que hay que saber

> **La plantilla D · Cancha queda DESCARTADA. No se sigue trabajando.**

El owner la miró en `canchapadel.localhost:4400` y la rechazó visualmente. La cáscara está
construida, testeada y revisada — pero **no va a producción**. No hay que "mejorarla".

**RATIFICADO el 2026-08-16:** *"D de la cancha sí queda fuera, en un futuro implementaremos algunas
más"*. O sea que el catálogo se queda en **cuatro**, y las plantillas que vengan serán nuevas, no un
rescate de D. Todo lo de D que valía la pena ya salió a `main` por otro lado (§3).

`feat/plantilla-d-cancha` **sigue sin borrarse**, a propósito: no cuesta nada tenerla y ahí está el
registro completo de la fase —las mediciones de contraste, el ledger, los reportes— por si una
plantilla futura quiere volver a mirar cómo se resolvió el campo de color o las líneas como
estructura. Borrarla es una decisión de una línea cuando el owner quiera.

Esto **no** afecta a A, B, C ni E, que se quedan.

---

## 2. Dónde está cada cosa

### Mergeado en `main` (front y back)

Las cuatro plantillas que se quedan, y toda la deuda que se pagó alrededor:

| plantilla | nombre | estado |
|---|---|---|
| **A** | Afiche | la original, editorial, marca grande |
| **B** | Nocturna | rediseñada — oscura, con luz de cancha |
| **C** | Básica | **en `main` todavía es la "Tarjeta" vieja con rail lateral.** El rediseño (una columna + el lomo) está construido pero **sin mergear**, en `feat/plantilla-c-basica` — ver §10 |
| **E** | Diurna | nueva — vidrio a caballo del borde de un campo de color |

Además está mergeado el back que acepta las cinco plantillas, y una rama entera de **cierre de
deuda** que arregló, con números medidos:

- **El anillo de foco de teclado.** Usaba el color crudo del club: medido en **1,00:1** en algunos
  clubes, o sea literalmente invisible. Afectaba a A, C y **todo el panel de admin**. Ahora es un
  token de plataforma (`--anillo-foco`). 14 de 30 zonas fallaban el mínimo de 3:1; ahora ninguna.
- **Las tildes de las mayúsculas se recortaban**: `Sol Pádel` renderizaba `SOL PADEL` en B, en E y
  en la nav del panel. En un producto en español eso es la marca del cliente saliendo mal.
- **Los botones del pie** de A y C estaban abajo de AA (C en 4,13:1).
- **La suite e2e se ensuciaba sola**: creaba ~7 reservas por corrida y no limpiaba ninguna, hasta
  que specs ajenas fallaban *por falta de datos y no por código*. Ahora hay un `globalTeardown`.
- **Dos carreras en la disponibilidad** del flujo de reserva: una respuesta vieja repintaba la
  grilla del día que el visitante ya había abandonado. En los tests salía como el intermitente del
  "element was detached from the DOM".
- **El selector de plantillas del panel** tenía la lista escrita a mano con A/B/C y descripciones
  viejas — la E no se podía elegir. Ahora deriva del registry.

### Sin mergear: la rama `feat/plantilla-d-cancha`

18 commits. **Contiene la plantilla descartada, pero también cosas que NO son D y que valen** — ésas
ya salieron a `fix/filo-cta-a11y` (§3-bis).

---

## 3. Qué hacer con la rama de D · lo primero a decidir

**No la borres entera.** Dos de sus commits no son D y arreglan cosas reales — eran tres hasta que se
verificó el tercero, ver (c). **Este trabajo ya está hecho: la rama de rescate existe, ver 3-bis.**

### a) `20f4702` — el filo del CTA. **Esto hay que salvarlo sí o sí.**

El botón de confirmar reserva —el CTA más importante del producto— **no tenía ningún límite
visible**: `border: none` hardcodeado. Medido contra el tablero sobre el que se apoya:

| club | contraste del relleno |
|---|---|
| naranja del **tenant demo real** | **2,07:1** |
| amarillo | **1,32:1** |
| casi blanco | **1,08:1** |

WCAG 1.4.11 pide **3:1** para el límite de un componente. Este commit se lo da a **A, B, C y E**.
Es un arreglo de accesibilidad para las cuatro plantillas que se quedan, y **si se descarta la rama
se pierde**.

Se descubrió construyendo D: ninguno de los dieciséis tokens del contrato `--flow-*` tocaba
`.confirm`. Se agregó el decimoséptimo, `--flow-cta-edge`.

### b) `8cebe75` — una corrección de documentación medida

Los comentarios de `styles.scss` y de B afirmaban que el navegador **sintetiza el ancho** de una
tipografía a la que se le piden ejes que no tiene. Medido en Chrome: **falso**. El ancho no se
sintetiza (delta cero); el **peso** sí (+21% de tinta). O sea que `--display-weight` carga peso real
y `--display-stretch` es un no-op en Chrome.

### c) ~~`16136f6` — cierra una nota vieja en el pie~~ · **NO SE PUEDE RESCATAR, y acá estaba el error**

Se verificó: `16136f6` edita un comentario que vive **adentro del bloque `:host(.d-foot)`** de
`landing-footer.scss`, y ese bloque **no existe en `main`** — lo creó la Task 1 de D. Es D puro. Se
descarta con la rama.

Lo demás (todo lo que toca `shells/d-cancha/`, más el plan y el registro de D) es la plantilla
descartada.

---

## 3-bis · HECHO: la rama de rescate ya existe · `fix/filo-cta-a11y`

**Creada desde `main` (`5f44eea`), 4 commits, árbol limpio. Falta mergearla y falta UNA puerta.**

| commit | qué |
|---|---|
| `9c631ae` | `8cebe75` tal cual — la corrección medida de tipografía |
| `f25653c` | la parte **no-D** de `09aebfa`: el decimoséptimo token en `booking-flow.scss` + las cuatro cáscaras declarándolo en `none`. Los tres archivos de `d-cancha/` se sacaron del pick (chocaban como *modify/delete*, que es lo esperado) |
| `766633c` | `20f4702` — el filo con valor real en A, B, C y E. **Mensaje corregido**: el original decía "A, C y E" y omitía a B, que también se mueve |
| `765a9d5` | barrido de comentarios: decían "las cinco cáscaras", "el filo de la D", "el papel claro de A/C/D". Sin D son cuatro. Sólo comentarios |

**Verificación corrida acá, no heredada de la rama de D:**

| puerta | resultado |
|---|---|
| `npm test` | **219 passed**, 19 archivos ✅ (la rama de D decía 285 con 20: la diferencia es `d-cancha/contraste.spec.ts` y las puertas parametrizadas por cáscara, que pasan de cinco a cuatro) |
| `npm run build` | limpio, **500,03 kB** ✅ — el mismo número que `main` |
| CSS compilado del bundle | **cuatro** declaraciones de `--flow-cta-edge` (una por cáscara, la de B reconocible por su `--paper` sobre `#07090f`) y `.confirm` embarcando `border: var(--flow-cta-edge, none)` en vez de `border: none` ✅ |
| `npx playwright test e2e` | 🔴 **NO SE CORRIÓ.** Debería dar **21** (no 22: D no tiene fila acá) |

**Por qué no se corrió el e2e:** Docker no está levantado y `Start-Service com.docker.service` falla
por falta de elevación, tal cual dice la §6. Hay que arrancarlo desde una PowerShell **como
administrador**, levantar el back en :8095, y recién ahí:

```
npx playwright test e2e
```

Ojo con la trampa de siempre: matar cualquier `ng serve` propio en 4400 antes, porque Playwright
levanta el suyo con `reuseExistingServer: false`.

**Lo que NO se hizo, a propósito:** no se mergeó nada y no se borró `feat/plantilla-d-cancha`. Esa
rama tiene además un commit nuevo, `c4fc6d8`, con el cierre de comentarios que estaba sin commitear
(las notas que `20f4702` volvió falsas, la inversión figura/fondo del club casi blanco dicha con la
palabra correcta, y la tabla del 70/30 con sus dos ejes). Está preservado ahí; nada de eso hace falta
en `main`.

**Una cosa que sigue valiendo y no hay que aflojar:** `contrato-flow.spec.ts` exige que toda cáscara
de `DIR_SHELL` declare todos los tokens que el flujo consume, sin huérfanos. Con D afuera, el token
nuevo lo declaran las cuatro que quedan y el spec queda verde solo. Si se pone rojo, está diciendo
algo real.

---

## 4. Lo que queda por hacer, en orden

La spec de diseño es `padelFront/docs/superpowers/specs/2026-08-08-plantillas-visuales-design.md`.
Sigue siendo válida **salvo por dos cosas**: D, que hay que marcar como descartada, y el "mismo
origen" del §7, que es falso en desarrollo (ver 4-bis).

**Al 2026-08-16, de esta lista sólo quedan abiertas la 4.2 y la 4.4.** Las otras dos están hechas y
sin mergear, en `feat/galeria-plantillas` (ver 4-bis). Se dejan escritas acá, tachadas, porque
explican **por qué** se hicieron y ese porqué sigue valiendo.

**Lo verdaderamente pendiente hoy no es ninguna de las cuatro: es MERGEAR.** Hay dos ramas listas y
verificadas —`fix/filo-cta-a11y` y `feat/galeria-plantillas`— y las dos esperan lo mismo: levantar
Docker y correr el e2e.

### 4.1 · ~~La galería del panel (spec §7)~~ · **HECHA, ver 4-bis**

Hasta el 2026-08-16 el dueño del club elegía su plantilla de un `<select>` de texto: leía "B ·
Nocturna — Oscura, luz de cancha" y tenía que imaginarse el resto. **No veía nada.**

La spec pide reemplazarlo por:

1. Una **grilla de miniaturas tokenizadas** (`<plantilla-thumb>`): HTML chico que usa
   `var(--court)` / `var(--court-2)`, así el club se ve **con sus propios colores** en todas antes
   de elegir. Sin imágenes ni iframes múltiples.
2. Un **preview vivo** del seleccionado: iframe a `/?plantilla=<X>&color=%23RRGGBB` (mismo origen),
   aprovechando los params que ya existen en `landing.ts`. **Arranca en marco de teléfono (390px)**
   con toggle a escritorio, porque el producto se usa mayormente en mobile.

Por qué va primero: las plantillas ya construidas **no se están vendiendo**. El dueño no puede
elegir con criterio lo que no ve. Es la pieza que convierte cuatro plantillas en un catálogo usable.

La selección sigue llamando a `setMarcaPlantilla()`; el guardado y el aviso de cambios sin guardar
ya funcionan.

**Ojo:** la spec habla de *cinco* miniaturas. Con D descartada son **cuatro**. La lista tiene que
salir de `CODIGOS_CON_SHELL` (en `core/landing/plantillas.ts`), que ya deriva de las cáscaras que
existen de verdad — no escribirla a mano, que es un error que ya se cometió tres veces en este
código.

### 4.2 · ~~El rediseño de C (spec §6.1)~~ · **HECHO Y MERGEADO el 2026-08-17** (ver §10)

> *"si me gustaría algún refactor de C, la verdad que no me gusta mucho"* — owner, 2026-08-16.

La spec describe una C más rica que la que existe: **barra inferior con el recap vivo del turno**
como firma, cards apiladas, radios de 20-26px, tono de copy cercano. La C actual no tiene eso — es
la más pobre de las cuatro y se nota al lado de B y E, que se rediseñaron enteras.

La §6.1 es un **contrato de diferenciación C ↔ E** escrito justamente para que no terminen siendo
primas. **Hay que releerlo antes de tocar C**, porque E ya se construyó y ocupó parte del terreno
que la spec le reservaba a C.

**Antes de codear hace falta brainstorming**: "no me gusta mucho" no es una especificación, y la
spec §6.1 se escribió cuando E todavía no existía. Lo primero es acordar qué tiene que ser C hoy.

### 4.3 · ~~La sección de marca en marketing (spec §8)~~ · **HECHA, ver 4-bis**

Sección nueva **"Tu marca · Tu club, con tu cara."** entre `#producto` y `#como-funciona`. En vez de
afirmar que el producto es personalizable, lo demuestra.

El agujero que tapa era medible: `grep -rniE "plantilla|diseñ"` sobre `features/marketing/` daba
**cero resultados**. El producto tenía cuatro diseños construidos y la página que lo vende no los
nombraba.

### 4.4 · ¿Reemplazar a D con otra quinta plantilla?

La spec pedía cinco. Con D descartada quedan cuatro. **Es una decisión de producto, no técnica.**
Cuatro plantillas bien diferenciadas es un catálogo respetable; una quinta forzada sería peor.

Si se decide hacer una quinta, lo que aprendimos con D vale: el concepto tiene que sobrevivir a
mobile. D se leía bien en escritorio y en tablet y **no se leía en teléfono**, y el producto se usa
mayormente en teléfono.

---

## 4-bis · HECHO: la galería del panel y la sección de marketing · `feat/galeria-plantillas`

**Creada desde `main` (`5f44eea`), 11 commits, árbol limpio, sin mergear. Verde de punta a punta.**
Cubre §4.1 y §4.3 de la spec, más una sección de marketing que pidió el owner (§4-ter). Plan
ejecutado:
`padelFront/docs/superpowers/plans/2026-08-15-galeria-plantillas-panel.md`.

**Lo que cambió para el dueño del club:** el `<select>` se fue. Ve cuatro miniaturas con **su** color
y, debajo, su landing real en un iframe con marco de teléfono.

**Lo que cambió para el visitante de marketing:** hay una sección que le deja tocar un color y ver
las cuatro plantillas repintarse.

| pieza | dónde |
|---|---|
| `<plantilla-thumb>` — la miniatura tokenizada, capa 2, sin una sola imagen | `shared/plantilla-thumb/` |
| la grilla de radios que reemplaza al `<select>` | `admin/config/tabs/tab-club/` |
| el preview vivo, 390px con toggle a escritorio | `admin/config/tabs/tab-club/preview-plantilla/` |
| `urlPreviewLanding()` — la función pura del `src` del iframe | `core/landing/preview-url.ts` |
| la sección "Tu marca" | `marketing/marca-demo/` + `marketing.html` |

**La miniatura es una sola y la usan los dos.** Es lo que evita que marketing envejezca: si una
plantilla cambia su silueta, cambia en los dos lados.

### Verificación

| puerta | resultado |
|---|---|
| `npm test` | **230 passed**, 24 archivos ✅ (`main` traía 177) |
| `npm run build` | limpio, **501,67 kB** ✅ (+1,64 kB sobre `main`, con el presupuesto en 550) |
| `npx playwright test e2e` | **23 passed**, tres corridas ✅, con `[teardown] 7 reserva(s) canceladas` (eran 21) |
| revisión visual de las miniaturas × las 6 paletas | ✅ hecha, y encontró un bug (abajo) |
| la sección de marketing a 1280 y a 390 | ✅ mirada, con el swatch verificado en la página real |

**El e2e encontró dos bugs que ningún unit veía, y los dos valen para lo que venga:**

- **El panel no marcaba nada.** El tenant `demo` está guardado en `D` —la descartada, sin cáscara—,
  así que ninguna de las cuatro matcheaba: la galería salía **sin nada seleccionado** mientras la
  landing pública dibujaba la A. El `<select>` viejo tenía el mismo agujero y salía en blanco. Ahora
  se marca `shellDePlantilla(marcaPlantilla())`, que es la verdad de lo que se dibuja, **sin**
  reescribir el valor guardado.
- **El radio no se podía clickear.** `.thumb` declara `position: relative`, o sea que es un elemento
  posicionado y va después en el DOM: se pintaba **encima** del input que cubre la tarjeta. A mano no
  se nota (el `<label>` igual lo activa), pero Playwright reintentaba para siempre. Costó dos
  corridas completas colgadas antes de mirarlo bien. `z-index: 1` en el input.

---

## 4-quater · HECHO: la revisión visual del panel, y lo que encontró

Era la última puerta pendiente de toda la fase, y **encontró tres problemas que ningún test veía**.
Vale como argumento de por qué la revisión visual no es opcional:

- **La galería caía 3+1.** En escritorio la config es de **dos columnas**, así que el bloque de marca
  mide ~490px con la ventana en 1280 y ~680px con la ventana en 768: **un media query mira la
  ventana y se equivoca en los dos casos**. Se resolvió con una *container query* sobre el bloque —
  2×2 de base, las cuatro en fila cuando el bloque da. Cuatro miniaturas quieren 4 o 2 por fila,
  nunca 3.
- **El preview de teléfono se cortaba.** 390px fijos adentro de un panel de ~350 (la config abierta
  en un teléfono), y el `overflow: hidden` del marco le comía la mitad derecha: el dueño veía
  "PADEL HU" y media landing. Ahora `min(390px, 100%)`.
- **El modo escritorio, lo mismo**, y al arreglarlo con pasos de escala mal elegidos se fue al otro
  extremo: 307px de maqueta adentro de un marco de 478. Los pasos están elegidos para que los 1280
  escalados entren justo — aprovecha 94-96% del marco en todos los anchos.

También se ocultaron las **cuatro cards de features en mobile** (pedido del owner): son cuatro frases
genéricas que en un teléfono cuestan cuatro pantallazos antes de llegar a las secciones que sí
muestran algo. Con `display: none` y sin borrar el markup — siguen en el HTML servido (verificado),
así que Google las indexa y en escritorio se ven igual.

---

## 4-ter · HECHO: "Nos adaptamos", la sección que faltaba en marketing

**Pedido del owner el 2026-08-16, y el diagnóstico era correcto:** *"la landing muestra muy poco lo
que realmente hacemos"*. Vendía "reservas online 24/7" y "agenda por cancha" y no decía **nada** de
lo que el producto resuelve. **Un club que cobra seña —o sea la mayoría— no se enteraba de que el
producto la cobra.**

Va en `marketing/adaptable/`, entre `#producto` y `#tu-marca`: lo que el producto **hace** antes de
cómo se ve. Tres bloques, los tres sacados de lo que el panel configura de verdad:

| bloque | qué cuenta | de dónde sale |
|---|---|---|
| Cómo cobrás | sin seña · seña por transferencia · seña por Mercado Pago, **cada uno con el estado en el que nace la reserva** | pestaña "Cobros" |
| Cuánto cobrás | precio general o por cancha, más franjas horarias con % | pestaña "Precios" |
| Tus canchas | los tres materiales de pared y techada/descubierta, con la **misma ilustración** del paso "Elegí cancha" | pestaña "Canchas" + `booking-flow` |

"El estado en el que nace la reserva" es lo que un dueño pregunta primero, así que está separado y
no escondido adentro del párrafo: *confirmada al instante* · *pendiente hasta que confirmás* ·
*se confirma sola cuando el pago entra*.

**Los modos de cobro y los materiales viven en constantes exportadas y sus tests las cuentan.** Si el
producto gana una cuarta forma de cobrar y nadie la suma acá, la landing queda contando de menos —
que es exactamente el modo de falla que dejó a la landing sin mencionar las señas durante meses.

**Una que sólo se ve mirando:** sin los `::before` de reflejo, la cancha de cristal y la mixta se ven
**idénticas**. Tres figuras iguales abajo de un título que dice "tus canchas son tus canchas" no
prometen nada. Las reglas se trajeron de `booking-flow.scss`.

### Se rehízo entera, y el motivo vale para la próxima sección

La primera versión eran **tres bloques con tres lenguajes visuales distintos** —tarjetas con borde de
color, filas tipo tabla, figuras ilustradas—, que se leían como tres mini-secciones pegadas con
cinta. Y quedaba **justo arriba de "Tu marca", que es interactiva**: el visitante toca un color, ve
repintarse las cuatro plantillas, sube los ojos y encuentra tres cajas quietas. La comparación le
jugaba en contra.

Hoy los tres bloques comparten **una sola forma: una fila de opciones y un resultado que cambia** —
la misma lógica de la sección de abajo. Lo que se muestra no es adorno:

| bloque | qué contesta al tocar |
|---|---|
| cobro | el **estado en el que nace la reserva** (confirmada / pendiente), + qué hace el club y qué ve el jugador |
| precio | el número, **calculado** aplicando el % al base — hay test que lo verifica contra la cuenta |
| cancha | la figura se **redibuja** con la pared elegida; el techo es un interruptor aparte |

**La lección de layout, que ya costó tres veces:** en un archivo de componente, una media query
escrita **antes** de la regla base tiene la misma especificidad y **pierde en silencio**. Pasó con las
canchas en fila, con el ancho de la figura, y se repitió incluso después de haberlo documentado. Las
reglas de pantalla angosta van **al final del archivo**. Las tres veces se descubrió midiendo con
`getBoundingClientRect()`, nunca leyendo la hoja ni mirando la captura.

### Lo que la landing sigue sin contar

No es exhaustiva y conviene saberlo antes de decir que está terminada: no menciona el **panel de
turnos** en sí (mover, liberar, confirmar), ni la **reserva manual** que carga el club por teléfono,
ni los **bloqueos** de cancha, ni la **política de cancelación**. Las cuatro cards de `#producto`
siguen siendo genéricas y podrían reescribirse con lo que ahora sí está contado más abajo.

### Tres cosas que aparecieron construyendo, y que valen para lo que sigue

**1. La trampa del apex, que la spec §7 tiene mal.** La spec dice "iframe a `/?plantilla=…` (mismo
origen)". Eso es cierto en producción y **falso en desarrollo**: acá el panel corre en `localhost`
(sin subdominio), y `tenantHostMatch` manda `/` a la landing de **marketing**, no a la del club. Por
eso el `src` sale de una función pura (`urlPreviewLanding`) y no de un string relativo. De paso
apareció que `tenantSubdomain()` leía `environment.baseDomain` de adentro, así que **el camino de
producción era imposible de testear**; ahora las dos funciones lo aceptan por parámetro.

**2. `container-type` no se aplica a sí mismo, y ningún test lo veía.** Estaba declarado en `.thumb`:
sus descendientes lo consultaban bien, pero las `cqi` de las **propias** propiedades de `.thumb` (el
padding de cada silueta) caían al container de más arriba —que no existe— y resolvían contra el
**viewport**. Medido: `padding: 8cqi` daba **102px de padding adentro de una miniatura de 150px**. La
suite entera seguía verde: la miniatura existía, con su atributo, su esquema y su silueta. Lo
encontró **mirar la captura**. Va con puerta sobre la hoja, probada en rojo.

**3. Un radio sin caja no se puede clickear.** Los radios ocultos estaban en `width/height: 0`.
El `<label>` que los envuelve igual los activa, así que a mano funciona — pero **Playwright no puede
tocarlos**, y los dos e2e nuevos habrían fallado apenas levante Docker. Ahora van con `inset: 0`: el
input **es** la superficie del control. Si escribís otro control así, acordate.

---

## 5. Deuda abierta, medida y sin arreglar

Todo esto está medido, no estimado. Los números salen de mediciones en el navegador contra seis
paletas de club (teal de plataforma, naranja del demo, amarillo, casi blanco, casi negro, fucsia).

| qué | dónde | número |
|---|---|---|
| `--flow-soft-ink-accent` usa `--court-deep` | `a-afiche/_tokens.scss`, `c-tarjeta/_tokens.scss` | abajo de AA en **4 de 6** paletas; naranja del demo **2,98:1**. Receta medida que sí funciona: `mix(--court 40%, --ink)`, techo 41,34% |
| El hover del pie de C | `landing-footer.scss`, bloque `:host(.c-foot)` | abajo de AA en 3 de 6; **por debajo del estado normal en 5 de 6** — pasar el mouse *empeora* la legibilidad |
| Los links del pie de A | `landing-footer.scss`, `:host(.pb-foot)` | fallan 4,5:1 en 8 de 24 celdas. **Dos dueños ajenos a esa regla**: el techo de `--ink-on-accent` contra la punta oscura del degradé (3,63 máximo) y `.watermark` metiéndose abajo del pie a 1280 |
| Un club casi blanco invierte figura y fondo | capa 3, `core/branding/` | el arreglo de fondo es un **piso de saturación** cuando el color del club está demasiado cerca del papel. Afecta a cualquier plantilla que use el color como masa |
| Si el visitante cambia la duración mientras las sondas iniciales están en vuelo | `booking.store.ts` | la grilla puede pintar slots de la duración vieja. Se autocorrige al primer click de día |

---

## 6. Cómo levantar el entorno · esto costó horas, no lo re-descubras

### Base y back

MySQL corre en Docker como **`padel-mysql`**, mapeado **3308 → 3306**.

El back **NO arranca con `SPRING_PROFILES_ACTIVE=local` solo**. Los defaults de `application.yml`
son puerto **8080** y MySQL **3306**, y ninguno de los dos sirve acá. Desde `padelBack`:

```
PORT=8095 DB_URL='jdbc:mysql://localhost:3308/padeldb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

Si Docker no responde con `open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file
specified`, el servicio `com.docker.service` está detenido y arrancarlo **pide elevación**:
`Start-Service com.docker.service` en una PowerShell como administrador.

### Front

`npm start -- --port 4400 --host localhost`, o el `launch.json` que ya está en `.claude/`.

### Tenants de desarrollo

Panel en `http://localhost:4400/admin` (host raíz, sin subdominio). Password `padel123` en todos.

| tenant | landing | plantilla | owner |
|---|---|---|---|
| demo | `demo.localhost:4400` | A | `owner@padelhub.com` |
| acepadel | `acepadel.localhost:4400` | A | `owner@acepadel.com` |
| costapadel | `costapadel.localhost:4400` | B | `owner@costapadel.com` |
| urbanpadel | `urbanpadel.localhost:4400` | C | `owner@urbanpadel.com` |
| solpadel | `solpadel.localhost:4400` | E | `owner@solpadel.com` |
| canchapadel | `canchapadel.localhost:4400` | D *(descartada)* | `owner@canchapadel.com` |

`?plantilla=X` y `?color=%23RRGGBB` pisan plantilla y color en cualquier landing — sirve para probar
paletas sin tocar la base.

---

## 7. Trampas del tooling · todas costaron tiempo real

- **`npm test -- --filter <archivo>` corre CERO tests y sale en verde.** El filtro es un regex sobre
  *nombres de test*, no rutas. Es un falso verde. `npx vitest run <path>` dice "no tests" porque se
  saltea el setup de Angular. **Sólo `npm test` pelado sirve.**
- **`npx playwright test` SIEMPRE con el path `e2e`.** Pelado escanea `src/`, `.claude/` y el
  proyecto hermano BarberApp, carga dos copias de `@playwright/test` y corrompe el runner.
- Playwright levanta su propio front en 4400 con `reuseExistingServer: false` → hay que matar
  cualquier `ng serve` propio antes.
- **El harness de comparación visual da CEROS FALSOS**: dos sets de capturas pueden salir del mismo
  bundle viejo y los dos leer 0 px. Antes de creerle a un 0, inyectá 1px en una plantilla ajena y
  confirmá que el diff lo detecta. Para cambios de alcance chico, **diffear el CSS de componentes
  compilado** es más fuerte y es inmune a esa trampa.
- **`color-mix()` serializa en Chromium como `color(srgb …)` con canales 0..1.** Un parseo ingenuo
  con `/\d+/` da luminancias absurdas. Hay que componer sobre canvas.
- El e2e tiene un intermitente conocido en `plataforma.spec.ts`. `retries` está en 0: re-correr una
  spec sola antes de sacar conclusiones.
- **Un banco de pruebas HTML también miente, y de dos maneras que ya se pagaron** (el banco está en
  `.superpowers/sdd/ver-miniaturas.mjs`, se puede reusar):
  - Sass compila `:host([data-esquema='dark'])` **sin las comillas**. Un reemplazo que las espere no
    matchea, el bloque oscuro no se aplica, y la plantilla B se renderiza **en claro** — se lee como
    un bug del producto y no lo es.
  - Si el banco no copia el `box-sizing: border-box` de `styles.scss`, el `aspect-ratio` se aplica a
    la caja de contenido y cada miniatura mide distinto alto por su propio padding. Otro bug
    fantasma.
  Los dos se descubren igual: **midiendo en el navegador** (`getBoundingClientRect`) en vez de
  estimar de la captura. Estimar del PNG ya llevó a reportar un desnivel que no existía.

---

## 8. Lo que funcionó del proceso, y lo que no

Vale para quien retome, porque cambia cuánto tarda lo que sigue.

**Funcionó: medir en vez de opinar.** Cada decisión de color se midió contra las seis paletas y se
reportó el **techo medido**, no un número redondo. Así aparecieron el anillo a 1,00:1, el CTA a
2,07:1 y las tildes recortadas — todos con la suite entera en verde.

**Funcionó: probar cada puerta en rojo.** Un test que nunca se vio fallar no es una puerta. Dos
veces se encontró que un spec que parecía cubrir algo **no lo cubría**, borrando la regla y viendo
que todo seguía verde.

**Funcionó: las puertas se pagaron solas.** La plantilla E estrenó con los dos botones del pie en
**negro puro durante una fase entera**, con la suite en verde, porque nada exigía que una cáscara
declarara su bloque de pie. Cuando D entró al registry, **once tests se pusieron rojos solos** antes
de que existiera un archivo de la cáscara.

**No funcionó: escribir en el plan cómo se rompe algo, sin haberlo roto.** Cinco veces un plan
afirmó un vector de rotura que no rompía nada, y quien lo ejecutaba perdió tiempo descubriéndolo.
Es mejor escribir *"probá esto en rojo y encontrá vos el vector"*.

**No funcionó: un agente revisor por cada tarea, sin distinguir riesgo.** En las tareas visuales el
revisor encontró cosas reales. En las de sólo tests o sólo comentarios, el costo no se justificó.

---

## 9. Dónde está el detalle

- **Spec de diseño:** `padelFront/docs/superpowers/specs/2026-08-08-plantillas-visuales-design.md`
- **Planes ejecutados:** `padelFront/docs/superpowers/plans/`
- **Registros de avance y reportes por tarea:** `padelFront/.superpowers/sdd/` — **está
  gitignoreado**, así que vive sólo en esta máquina. Ahí están todas las tablas de contraste, las
  mediciones y las reviews de rama. Si esta carpeta se pierde, se pierden los números.

---

## 10. La plantilla C · Básica — cerrada y mergeada (2026-08-16/17)

Las nueve tareas están hechas y revisadas, el review de rama también, y todo está en `main` en los
dos repos. Acá va el detalle de **qué se decidió y qué quedó hecho**, para que quien retome no tenga
que reconstruirlo.

### Por qué se rehizo C

**C no era la plantilla que decía ser.** El registry se la ofrecía al dueño del club como
*"C · Tarjeta — Tipo app, para el pulgar"*, y su layout de escritorio eran **dos columnas con un rail
lateral sticky de 280px**. El dueño elegía una cosa y recibía otra. Además nunca se había rediseñado:
62 líneas de hoja contra 255 de A, 212 de B y 166 de E.

Lo que el owner dijo que le molestaba, textual: *"le falta personalidad"*, *"el rail lateral se siente
panel de admin"*, *"el color del club casi no aparece"*. Y sobre la dirección: *"mandaría más a la
minimalista… eso de las cards mucho no me gusta"*.

### Las decisiones del owner · no re-decidir

1. **Las cards apiladas quedan descartadas.** C deja de ser "Tarjeta" y pasa a llamarse **Básica**.
2. **La firma es EL LOMO**: una banda vertical delgada del color del club pegada al borde izquierdo,
   de arriba abajo, como el lomo de un libro. Elegida entre tres opciones mostradas en maquetas (el
   índice, el dato que crece, el lomo).
3. **C pasa a ser la plantilla POR DEFECTO** del producto, el lugar que ocupaba A.
4. **El contenido no se recorta.** "Básica" es sobriedad visual y ser la default, **no** mostrar menos.
5. **El rail se va**: una sola columna, la misma en todos los anchos.

### Las tres decisiones de riesgo, ya tomadas y cerradas en la spec

- **Los clubes que no eligieron plantilla** estaban viendo A. Antes de mover la default se les
  escribió `'A'` explícito en la base (migración `V18__plantilla_explicita.sql`, idempotente), así el
  cambio alcanza **sólo a clubes nuevos** y a nadie se le cambia la página sin haber tocado nada.
- **Los fallbacks del contrato `--flow-*` se quedan en los valores de A.** Protegen a una cáscara que
  se olvidó de declarar un token, que es otro problema distinto de cuál es la default. Sólo se
  corrigió la redacción, que se volvía confusa.
- **El `@default` del dispatcher y `shellDePlantilla()`** sí pasan a C, con sus tests actualizados.

### Lo que quedó construido y revisado (las nueve tareas)

| tarea | qué |
|---|---|
| 1 | la migración `V18` en el back (repo separado, rama `feat/plantilla-default-explicita`) |
| 2 | el registry: C se llama Básica y es la default |
| 3 | el dispatcher: el `@default` dibuja C |
| 4 | la cáscara: una sola columna, el rail afuera, acoplamientos resueltos |
| 5 | **el lomo**, con su piso de contraste medido |
| 6 | las dos deudas medidas de C |
| 7 | el contraste pineado contra la hoja |
| 8 | las puertas e2e (el lomo y la default) — revisada el 2026-08-17, sus tres desvíos verificados |
| 9 | la spec vieja deja de describir a C como la Tarjeta que ya no es |

Más el review de rama (2026-08-17) y sus seis hallazgos, todos arreglados antes del merge.

### El número del lomo, que es el corazón de la plantilla

> **Al 2026-08-17 el lomo son DOS números, no uno.** Lo de abajo describe el extremo superior del
> degradado, que es el que se midió durante la fase. El inferior mezclaba hacia el papel y se borraba
> (1,02:1 con el club casi blanco); hoy va hacia la tinta al 30%, peor caso 6,32, y tiene puerta
> propia en `contraste.spec.ts`. Si vas a tocar el degradado, son dos extremos los que tienen que
> cumplir el piso.

El lomo **no puede dibujarse con el color crudo del club**: con un club casi blanco daría **1,04:1**
contra el papel y desaparecería — y con él desaparece lo único que hace a C ser C. **Es el modo de
falla exacto que hizo descartar la plantilla D.**

Por eso el color va llevado hacia la tinta:

```
--c-lomo-color: color-mix(in srgb, var(--court) 50%, var(--ink));
```

**50% es un techo medido**, no una preferencia: el peor club (casi blanco) da **3,26:1** y a 55% cae
a **2,83**, abajo del piso de 3:1 que pide WCAG 1.4.11 para un elemento gráfico.

**Sutileza que hay que respetar si alguien reproduce la cuenta:** sin redondear los canales de la
mezcla da 3,2668 (→3,27); redondeándolos a 8 bits —que es lo que el navegador pinta de verdad— da
3,2605 (→3,26). **Se pinea 3,26** porque es el color que el visitante ve, y el test usa el mismo
criterio. Si te da 3,27, no está mal: es la versión sin redondeo.

### Tres cosas que encontró el proceso y que ningún test veía

- **Un tripwire infalseable.** El test *"el lomo no se dibuja con el color crudo"* no podía fallar:
  `colorMix()` corría en el cuerpo del `describe` y tiraba antes de que la aserción llegara a
  evaluarse, abortando el módulo entero. Un tripwire que no puede fallar es peor que no tenerlo,
  porque se lee como cobertura. Arreglado: la lectura cruda queda en el `describe`, el parseo se
  movió adentro de cada `it`.
- **Una deuda pagada 1 de 3.** Se arregló el hover de `a` en el pie de C y quedaron
  `.arrep-link:hover` y `.politica-link:hover` con la receta vieja — mismo bloque, misma superficie,
  mismos números. El hueco estaba en el plan, no en la implementación.
- **Un ratio medido con la tinta equivocada.** El script de medición del plan hardcodeaba `#10151f`,
  que es el `--th-ink` de las miniaturas del panel, no el `--ink` de plataforma (`#11162b`). La
  decisión no cambiaba, pero el número anotado en la hoja salía 0,04 alto.

### Acoplamiento frágil que conviene arreglar en algún momento

`core/landing/plantillas.spec.ts` lee `shell.scss` **crudo, sin pelar comentarios** (a diferencia de
`contraste.spec.ts`, que sí los borra antes de parsear). Su regex `--ink\s*:\s*(...)` captura texto de
un **comentario** como si fuera una declaración CSS real: un comentario que escriba `` `--ink:` `` con
los dos puntos pegados rompe ese test sin que nadie entienda por qué. Ya pasó una vez y se esquivó
reformulando el comentario. El arreglo de fondo es que ese spec pele los comentarios como su vecino.
