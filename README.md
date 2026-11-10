# AdelitaGOAT — Sistema de Apartado y Pago en Efectivo para Puestos Universitarios

Aplicación móvil Android (**Kotlin + Jetpack Compose**) y API Backend (**Ktor + PostgreSQL**) orientada a estudiantes y vendedores de puestos de comida ubicados frente a la **Universidad Politécnica de Chiapas (UP Chiapas)**. Permite apartar alimentos desde el celular, recibir un folio único del pedido (`#A-XXXX`), evitar filas en recesos cortos y pagar **en efectivo al recoger en mostrador** (`EFECTIVO_AL_RECOGER`).

**Despliegue escolar (10 de noviembre de 2026):** [API pública](https://adelitagoat-api.onrender.com/api/health) en Render, PostgreSQL en [Supabase](https://supabase.com/dashboard/project/fhodeskjjpjtllcapdzw) y [APK de demostración descargable](https://github.com/251205-Yann/AdelitaGoat/releases/download/v1.0.0-escolar/AdelitaGOAT-public-api-debug.apk) compilado con esa URL HTTPS. La base contiene categorías de referencia y no contiene usuarios, puestos ni pedidos ficticios. El [informe técnico en PDF](output/pdf/Informe_tecnico_AdelitaGOAT.pdf) y su [fuente editable](docs/informe-tecnico.md) están en este repositorio.

---

## 1. Estructura del Proyecto y Entregables

| Ruta | Descripción |
|---|---|
| `docs/requirements.md` | Matriz de trazabilidad de requerimientos (Pantallas, API, Reglas de Negocio) y registro de decisiones ante ambigüedades. |
| `docs/http-contract.md` | Contrato HTTP detallado de la API (métodos, rutas, autenticación, payloads JSON y códigos de error). |
| `Dockerfile` / `.dockerignore` | Construcción multi-stage con JDK 21 (`gradle:8.11.1-jdk21` → `eclipse-temurin:21-jre-jammy`) lista para **Render Free** sin requerir Docker instalado localmente. Incluye también `server/Dockerfile` y `render.yaml`. |
| `docker-compose.yml` | Orquestación local opcional de PostgreSQL 16 con volumen persistente y healthcheck (sin secretos reales comprometidos). |
| `server/` | Backend en **Kotlin + Ktor 3 + PostgreSQL + HikariCP + Flyway + BCrypt**, configurado para **Supabase Session Pooler** y suite de pruebas JUnit 5. |
| `android/` | Proyecto Android Studio (**Kotlin + Jetpack Compose + Material 3 + Navigation + ViewModels**) conectado de punta a punta a la API (`API_BASE_URL` parametrizable). |

---

## 2. Requisitos del Entorno

- **JDK:** Java 21 LTS (p. ej. Eclipse Temurin 21, ubicado localmente en `C:\Users\danie\.jdks\temurin-21`).
- **Android Studio:** Ladybug / Meerkat o superior con **Android SDK 35** (`ANDROID_HOME=C:\Users\danie\AppData\Local\Android\Sdk`).
- **Base de datos:**
  - **Con Docker:** Docker Desktop / Docker Engine para levantar el contenedor `postgres:16-alpine` mediante `docker-compose.yml`.
  - **Sin Docker (Fallback automático de desarrollo local):** Si el servicio PostgreSQL externo en `localhost:5432` no está activo en la máquina anfitriona, `DatabaseFactory` activa automáticamente una base de datos local persistente en modo compatibilidad PostgreSQL (`./server/build/data/adelitagoat_pg`) y ejecuta las migraciones SQL. Los datos demo se cargan sólo con `SEED_DEMO_DATA=true`.

---

## 3. Comandos Exactos de Arranque

### 3.1. Base de Datos PostgreSQL (Opcional si se cuenta con Docker)

Desde la raíz del proyecto (`C:\Users\danie\Downloads\yandelApp`):

```powershell
docker compose up -d
```

Variables de entorno soportadas por el servidor (`server/src/main/resources/application.conf` y `DatabaseFactory.kt`):
- `DB_URL` (por defecto local: `jdbc:postgresql://localhost:5432/adelitagoat`; en producción Supabase: `jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require`)
- `DB_USER` (por defecto local: `adelita_dev`; en Supabase Session Pooler: `postgres.<project_ref>`)
- `DB_PASSWORD` (por defecto local: `adelita_local_dev_only`; en producción: contraseña de base de datos de Supabase)
- `DB_ALLOW_EMBEDDED_FALLBACK` (por defecto local: `true`; en producción/Dockerfile: `false` para exigir PostgreSQL real y fallar rápido si no hay conexión)
- `SEED_DEMO_DATA` (por defecto: `false` — base vacía con únicamente las 6 categorías de referencia; usar `true` sólo en desarrollo local para cargar datos ficticios de demostración)
- `DB_MAX_POOL_SIZE` (por defecto: `4` — pool HikariCP pequeño optimizado para Supabase Session Pooler y Render Free)
- `PORT` (por defecto: `8080`)

### 3.2. Iniciar el Servidor Backend Ktor (`server/`)

#### Modo Normal (por defecto — Base limpia sin usuarios ni puestos ficticios)

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\temurin-21"
cd C:\Users\danie\Downloads\yandelApp\server
.\gradlew.bat run
```

#### Modo Demo Optativo (con cuentas `.example.invalid`, puestos y pedidos de prueba)

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\temurin-21"
$env:SEED_DEMO_DATA = "true"
cd C:\Users\danie\Downloads\yandelApp\server
.\gradlew.bat run
```

El servidor quedará escuchando en `http://0.0.0.0:8080`. Puedes verificar su estado en:

```powershell
curl.exe http://localhost:8080/api/health
```

### 3.3. Ejecutar Pruebas Automatizadas del Backend

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\temurin-21"
cd C:\Users\danie\Downloads\yandelApp\server
.\gradlew.bat test
```

### 3.4. Compilar y Probar la Aplicación Android (`android/`)

#### Compilación Normal (por defecto — sin botones de cuentas demo en la UI)

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\temurin-21"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
cd C:\Users\danie\Downloads\yandelApp\android
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

#### Compilación con Botones Demo de Desarrollo (optativo)

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\temurin-21"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
cd C:\Users\danie\Downloads\yandelApp\android
.\gradlew.bat :app:assembleDebug -PENABLE_DEMO_ACCOUNTS=true
```

También puedes definir la URL base del API (p. ej. un endpoint HTTPS remoto) en tiempo de compilación con `-PAPI_BASE_URL=https://tu-dominio.example/api`.

El APK compilado se genera en:
`android/app/build/outputs/apk/debug/app-debug.apk`

---

## 4. Configuración en Android Studio y Emulador (`10.0.2.2`)

1. Abre **Android Studio** y selecciona **Open** → `C:\Users\danie\Downloads\yandelApp\android`.
2. En **Settings → Build, Execution, Deployment → Build Tools → Gradle**, verifica que **Gradle JVM** apunte a **JDK 21** (`C:\Users\danie\.jdks\temurin-21`).
3. Asegúrate de que el backend Ktor (`server`) esté corriendo en el puerto `8080`.
4. **Conectividad desde el Emulador Android:**
   - La app está preconfigurada con `DEFAULT_BASE_URL = "http://10.0.2.2:8080"`, que es el alias de red especial del emulador de Android para alcanzar el `localhost` de la máquina anfitriona.
   - Si ejecutas la app en un **dispositivo físico** conectado a la misma red Wi-Fi o contra un servidor remoto HTTPS, toca el botón **API** en la esquina superior derecha de la pantalla de inicio de sesión (o en la pestaña **Perfil**) y cambia la URL.
5. **AVD local disponible (`Medium_Phone_API_37.0`):**
   - El entorno local cuenta con el AVD funcional `Medium_Phone_API_37.0` (`emulator.exe -list-avds`).
   - Para iniciarlo e instalar el APK desde PowerShell:
     ```powershell
     & "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd Medium_Phone_API_37.0
     & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" wait-for-device
     & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "C:\Users\danie\Downloads\yandelApp\android\app\build\outputs\apk\debug\app-debug.apk"
     ```

---

## 5. Flujo Completo desde Base de Datos Vacía (Modo Normal vs. Modo Demo)

### 5.1. Recorrido en Modo Normal (`SEED_DEMO_DATA=false`, por defecto)

1. **Arranque inicial sin datos ficticios:**
   - Al iniciar el backend por primera vez con `SEED_DEMO_DATA=false` (valor por defecto), Flyway crea el esquema y `SeedData.seedReferenceCategories` registra únicamente las **6 categorías de referencia** (`Tacos`, `Tortas`, `Hamburguesas`, `Quesadillas`, `Bebidas`, `Snacks`).
   - Las tablas `users`, `stalls`, `products` y `orders` inician con **0 registros**.
   - Si un cliente abre la app antes de que existan puestos, `CustomerHomeScreen` muestra el estado vacío *"Aún no hay puestos registrados"* con botón **Actualizar catálogo**.
2. **Registro de Vendedor y Alta de Puesto Real:**
   - El vendedor se registra en `AuthScreen` seleccionando **Vendedor del Puesto**.
   - En el panel de vendedor (**Pedidos** o **Menú**), la app muestra el estado *"Aún no has registrado tu puesto"* con el botón **Registrar mi puesto**.
   - El vendedor completa el formulario (`StallFormDialog`) con los datos reales de su negocio: **nombre**, **descripción**, **ubicación textual concreta**, **categoría**, **tiempo estimado de preparación (minutos)** y estado **abierto/cerrado** (`POST /api/vendor/stalls`).
   - En cualquier momento puede modificar los datos de su puesto o abrirlo/cerrarlo desde la tarjeta superior del panel (`PUT /api/vendor/stalls/{stallId}`).
3. **Carga de Menú y Consulta por el Cliente:**
   - Una vez creado el puesto, el vendedor agrega sus platillos y bebidas reales en la pestaña **Menú** (`POST /api/vendor/stalls/{stallId}/products`) y controla su disponibilidad (`PATCH /api/vendor/products/{productId}/availability`).
   - El cliente toca **Actualizar** en la pantalla de inicio (`GET /api/stalls`), visualiza el puesto recién registrado y su menú real (`GET /api/stalls/{stallId}`), realiza su apartado (`POST /api/orders`) y recibe su comprobante con folio.

### 5.2. Despliegue Público en Render Free + PostgreSQL en Supabase + APK con HTTPS

El repositorio incluye un [`Dockerfile`](Dockerfile) multi-stage (JDK 21 → JRE 21), [`.dockerignore`](.dockerignore) y [`render.yaml`](render.yaml) para que **Render construya la imagen en la nube** directamente desde tu repositorio Git, **sin necesidad de tener Docker instalado en tu PC**.

#### Paso 1: Obtener datos del Session Pooler en Supabase (IPv4, Puerto `5432`, `sslmode=require`)

> **Importante:** Render Free utiliza salida de red **IPv4**. En Supabase Free, la conexión directa (`db.<ref>.supabase.co`) es sólo IPv6, por lo que **debes usar el Session Pooler (puerto `5432`)**, el cual soporta IPv4 y es totalmente compatible con migraciones Flyway (`V1__init_schema.sql`) y sentencias preparadas JDBC.

1. En tu proyecto de **Supabase**, haz clic en el botón **Connect** (barra superior) → pestaña **Connection String** / **JDBC**.
2. Selecciona **Method: Session pooler** (verifica que el puerto sea **`5432`**, no `6543`).
3. Identifica tus tres valores de conexión. El despliegue escolar usa una cuenta exclusiva `adelita_app` y un esquema privado `adelita`; no necesita la contraseña administrativa `postgres`:
   - **Host del pooler:** `aws-0-<region>.pooler.supabase.com` (por ejemplo, `aws-0-us-east-1.pooler.supabase.com`)
   - **Usuario (`DB_USER`):** `adelita_app.fhodeskjjpjtllcapdzw`
   - **Contraseña (`DB_PASSWORD`):** contraseña del usuario exclusivo, almacenada sólo en las variables privadas de Render y en el archivo local ignorado `.env.production.local`.
4. Construye el `DB_URL` en formato JDBC con TLS obligatorio (`sslmode=require`):
   ```text
   jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require&currentSchema=adelita
   ```
   *(Nota: `DatabaseFactory.normalizePostgresJdbcUrl` también añade automáticamente `?sslmode=require` si se omite en un host remoto y convierte prefijos `postgresql://` a `jdbc:postgresql://`, además de enmascarar cualquier secreto en logs).*

#### Paso 2: Configurar el Web Service en Render Free (Docker)

1. El código está publicado en [251205-Yann/AdelitaGoat](https://github.com/251205-Yann/AdelitaGoat) sin secretos ni datos demo activos.
2. En **Render Dashboard**, selecciona **New +** → **Web Service** y conecta tu repositorio.
3. Configura el servicio con estos parámetros:
   - **Runtime / Environment:** `Docker`
   - **Region:** `Virginia`, próxima al proyecto Supabase de `us-east-1`.
   - **Root Directory:** *(déjalo vacío para usar el `Dockerfile` de la raíz; o escribe `server` si prefieres usar `server/Dockerfile`)*
   - **Instance Type:** `Free`
   - **Health Check Path (Advanced):** `/api/health`
4. En la sección **Environment Variables** de Render, registra exactamente las siguientes variables:

| Variable | Valor en Render | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require&currentSchema=adelita` | Cadena JDBC del **Session Pooler** (IPv4, puerto `5432`, TLS y esquema privado). |
| `DB_USER` | `adelita_app.fhodeskjjpjtllcapdzw` | Usuario exclusivo de la aplicación. |
| `DB_PASSWORD` | Valor secreto guardado en Render | Contraseña del usuario exclusivo; nunca se publica en GitHub ni en el APK. |
| `DB_ALLOW_EMBEDDED_FALLBACK` | `false` | Desactiva cualquier base embebida/local; exige conexión real a Supabase. |
| `SEED_DEMO_DATA` | `false` | Garantiza que **no** se carguen usuarios, puestos ni pedidos demo. |
| `DB_MAX_POOL_SIZE` | `4` | Pool HikariCP pequeño (`maxPoolSize=4`, `minimumIdle=1`) ideal para Session Pooler en plan gratuito. |
| `PORT` | `8080` | Puerto interno en el que escucha Ktor dentro del contenedor. |

5. Al iniciar el despliegue, Render valida las migraciones Flyway sobre Supabase y verifica las **6 categorías de referencia**.
6. Verifica que el backend público esté en línea y conectado a Supabase ejecutando desde PowerShell:
   ```powershell
   curl.exe https://adelitagoat-api.onrender.com/api/health
   curl.exe https://adelitagoat-api.onrender.com/api/categories
   ```
   El endpoint `/api/health` responderá `{"status":"UP","databaseMode":"POSTGRESQL_SUPABASE_OR_REMOTE"}`.

#### Paso 3: Generar el APK de Android Conectado a la URL Pública HTTPS

Con la URL pública de Render, compila el APK inyectando `API_BASE_URL` y manteniendo deshabilitados los botones demo (`ENABLE_DEMO_ACCOUNTS=false`):

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\temurin-21"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
cd C:\Users\danie\Downloads\yandelApp\android
.\gradlew.bat :app:assembleDebug -PAPI_BASE_URL="https://adelitagoat-api.onrender.com" -PENABLE_DEMO_ACCOUNTS=false
```

- El APK listo para instalar en dispositivos físicos o emulador se generará en:
  `C:\Users\danie\Downloads\yandelApp\android\app\build\outputs\apk\debug\app-debug.apk`
- **Cambio rápido de URL sin recompilar:** Si ya tienes el APK instalado y deseas apuntarlo a otra URL, toca el botón **API** en la esquina superior derecha de la pantalla de inicio de sesión (o en la pestaña **Perfil**), ingresa la nueva URL HTTPS y guarda.

### 5.3. Modo Demo Optativo (`.example.invalid`)

- Si se define explícitamente `SEED_DEMO_DATA=true` en el backend, se cargan de forma idempotente cuentas de prueba con el dominio reservado RFC 2606 `.example.invalid` para no aparentar identidad institucional real:

| Rol | Nombre | Correo (`.example.invalid`) | Contraseña | Datos Demo Asociados |
|---|---|---|---|---|
| **CLIENTE** | Cliente Demo Uno | `cliente1.demo@example.invalid` | `Cliente123!` | Favorito inicial, pedido `#A-1042` (`ENTREGADO`, listo para calificar) y pedido `#A-1043` (`RECIBIDO`). |
| **CLIENTE** | Cliente Demo Dos | `cliente2.demo@example.invalid` | `Cliente123!` | Pedido `#A-1041` (`ENTREGADO` y ya calificado con 5 estrellas). |
| **VENDEDOR** | Vendedor Demo Uno | `vendedor1.demo@example.invalid` | `Vendedor123!` | Puestos demo **AdelitaGOAT - Tacos y Tortas** y **Tacos El Sol**. |
| **VENDEDOR** | Vendedor Demo Dos | `vendedor2.demo@example.invalid` | `Vendedor123!` | Puesto demo **Jugos y Antojitos Doña Meche**. |

> **Nota de desarrollo:** Los botones rápidos **Cliente Demo** y **Vendedor Demo** en `AuthScreen` están desactivados por defecto (`ENABLE_DEMO_ACCOUNTS=false`) y sólo se incluyen si compilas el APK con `-PENABLE_DEMO_ACCOUNTS=true`.

---

## 6. Decisiones de Diseño y Resolución de Conflictos entre Fuentes

1. **Pago exclusivamente en efectivo al recoger (`EFECTIVO_AL_RECOGER`):**
   - Aunque los wireframes iniciales de baja fidelidad del PDF incluían opciones de tarjeta/Apple Pay/Google Pay, el diseño final en alta fidelidad de Figma (`Untitled.png`) y `IMPLEMENTATION_BRIEF.md` establecen que el pago se realiza **en efectivo al recoger en mostrador**.
   - Cuando el vendedor marca un pedido como `ENTREGADO`, el servidor registra transaccionalmente `paymentStatus = "COBRADO_EN_EFECTIVO"`, `deliveredAt` y `paidAt`. Nunca se simula un cobro digital inexistente.
2. **Precios en centavos enteros y recálculo en servidor:**
   - Todos los precios se almacenan y procesan como `BIGINT` en centavos (`price_cents`, `unit_price_cents`, `subtotal_cents`, `total_cents`). El servidor ignora cualquier monto calculado por el cliente, verifica disponibilidad y pertenencia a un único puesto, y congela el precio histórico del producto en `order_items` al momento de confirmar el apartado.
3. **Protección contra duplicados (`Idempotency-Key`):**
   - `POST /api/orders` exige clave de idempotencia (`Idempotency-Key` por header o campo JSON) con restricción `UNIQUE (customer_id, idempotency_key)` en base de datos. Si el usuario presiona dos veces o reintenta por fallo de red, el servidor devuelve el pedido original sin duplicarlo.
4. **Ubicación fija de campus:**
   - Al no definirse coordenadas GPS ni permisos de ubicación en las fuentes, se utiliza la referencia del campus `"Frente a la Universidad Politécnica de Chiapas"` mostrada en Figma.

---

## 7. Extensiones Fuera del MVP (Pendientes para Futuras Versiones)

Conforme a la sección *Fuera del MVP* de `IMPLEMENTATION_BRIEF.md`, las siguientes características requieren credenciales o infraestructura externa y quedan documentadas como trabajo futuro (sin mostrar botones falsos en la interfaz):

1. **Inicio de sesión social (Google Sign-In / Apple ID):** Requiere configuración de proyecto OAuth 2.0 en Google Cloud Console / Apple Developer y certificados SHA-1 de firma.
2. **Notificaciones Push remotas (Firebase Cloud Messaging):** Actualmente el seguimiento del estado del pedido se consulta en tiempo real desde la pantalla **Pedidos / Estado del pedido** de la app contra la API.
3. **Pasarela de pagos digitales (Stripe / MercadoPago / OpenPay):** Fuera del alcance del MVP presencial en efectivo.
4. **Geolocalización GPS dinámica y mapas interactivos:** Requiere claves de Google Maps SDK y coordenadas geográficas reales por comercio.
