# Odysseia Engine v1.1.0 🦀☕

<p align="center">
  <strong>Motor Mítico, Noche de Terror, Sistema de Bosses y Gestión para Purpur/Paper 1.21.11</strong><br>
  Reescrito en <strong>Rust (Edición 2021 Workspace)</strong> para máximo rendimiento + Binding nativo Java 21 (FFM / Project Panama).
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Language-Rust_2021_|_Java_21-orange.svg" alt="Rust + Java 21" />
  <img src="https://img.shields.io/badge/Minecraft-Paper_/_Purpur_1.21.11-brightgreen.svg" alt="Minecraft 1.21.11" />
  <img src="https://img.shields.io/badge/Server-Star_Production-blue.svg" alt="Star Production" />
</p>

---

## 📌 Ramas del Repositorio

- **`main` (Rama Principal - Rust)**: Motor nativo en Rust con microservicio en Tokio + Axum para **Star** y binding FFI (`.dll` / `.so`) para Purpur 1.21.11.
- **`JAVA` (Rama Baseline Java)**: Versión original 100% Java del plugin Odysseia para Purpur.

---

## 🌟 Visión del Proyecto

**Odysseia** es la suite central que sostiene la experiencia de supervivencia, fantasía y administración en **DrakesCraft**. Unifica en una plataforma de alto rendimiento:

- ⚔️ **Panteón Mítico (20 Bosses)**: Bosses multi-fase con habilidades complejas (Zeus, Poseidón, Anubis, Hades, Fenrir, Kraken, Wither Storm Story Mode, Dragón Ancestral de 3,500 HP), renacer cinematográfico, drops legendarios protegidos con *Paper Item Ownership Lock* (`setOwner`) y recompensas de **5,000 XP**.
- 🐉 **Monturas Dracónicas Personalizadas**: Pilotaje 3D fluido de dragones para el Owner (**JackStar6677**) y el Dragón Esmeralda Cute para **Kika** (`KikaStar704`), con velocidad configurable de 1x a 20x (`/dragon speed`), control de estamina en Rust y alientos elementales (`/dragon aliento`).
- 🌫️ **Niebla Ultra Densa de Terror & Screamers**: Niebla de renderizado ultra densa (~1-2 bloques de visión) activable vía `/niebla` y eventos nocturnos aleatorios (**1 vez por día de Minecraft**) con screamers, espectros y susurros.
- 🩸 **Evento Luna de Sangre (BloodMoon)**: Sistema de noche sangrienta controlada en Rust con spawns especiales (2.5x) y multiplicador de daño mob (1.8x).
- 🛡️ **Protección de Terreno & Anti-Reloj**: Motor de indexación espacial en Rust (`odysseia-automation`) para detectar relojes rápidos de Redstone sin causar pausas de Garbage Collector, respetando parcelas de ProtectionStones y bloques Slimefun.
- 🎭 **Suite de Trolleos Inofensivos para Staff (`/troll`)**: Herramientas divertidas y seguras para moderadores (Fake OP, Fake Crash, Screamers, Anvil caída, Creeper siseante, Rayos, Arañas, VoidFall).
- 👻 **Vanish Avanzado (`/vani`)**: Modo invisible con control individual o por objetivos, ráfagas de partículas de portal y sonidos estéticos.
- 🛒 **Integración de Comercio & Slimefun**: Conexión con `DrakesSlimeMarket`, catálogo nativo de Dusts de Slimefun (`IRON_DUST`, `GOLD_DUST`, etc.) y entrega asíncrona de compras Tebex.

---

## 🏛️ Arquitectura del Sistema (Rust 2021 Workspace)

```
+--------------------------+    +--------------------------+    +--------------------------+    +--------------------------+
|  1. ODYSSEIA-CORE (Rust) |    | 2. ODYSSEIA-AUTOMATION   |    | 3. ODYSSEIA-HORROR       |    | 4. ODYSSEIA-SERVER / FFI |
+--------------------------+    +--------------------------+    +--------------------------+    +--------------------------+
| • 20 Bosses & Combat     | -> | • Spatial Hash Grid      | -> | • HorrorNightEngine      | -> | • Java 21 FFM Binding    |
| • Dragon Flight Policy   |    | • Redstone Clock Guard   |    | • BloodMoonManager       |    | • Tokio + Axum Microservice|
| • ChatFilterEngine       |    | • Slimefun Protection    |    | • Screamers & Atmosphere |    | • Star Telemetry Engine  |
+--------------------------+    +--------------------------+    +--------------------------+    +--------------------------+
```

### Crates del Workspace:

| Crate | Descripción |
| :--- | :--- |
| `crates/odysseia-core` | Los 20 Bosses, perfiles de combate, matemática 3D, monturas de dragón y filtro de chat. |
| `crates/odysseia-automation` | Guardián de Redstone, Anti-reloj espacial y filtro de protección Slimefun. |
| `crates/odysseia-horror` | Noche de terror, efectos atmosféricos del Wither Storm y Luna de Sangre. |
| `crates/odysseia-store` | Catálogo de productos (polvos Slimefun), motor de compras y entregas. |
| `crates/odysseia-telemetry` | Formateador de TPS/RAM y reporte para DiscordSRV. |
| `crates/odysseia-ffi` | Librería nativa C-ABI (`.dll` / `.so`) invocable desde Java 21 via **Project Panama / FFM API**. |
| `crates/odysseia-server` | Daemon / Microservicio independiente en Tokio + Axum corriendo en el puerto 8080. |

---

## 📜 Comandos & Tabla de Permisos (LuckPerms)

El plugin Odysseia asigna los permisos respetando el rango inicial por defecto **`polis`** para usuarios comunes y roles superiores para el Staff.

### 👑 Comandos de Staff y Creador

| Comando | Aliases | Permiso | Grupo LP Recomendado | Descripción |
| :--- | :--- | :--- | :--- | :--- |
| `/dragon [subcomando]` | `/mountdragon`, `/dragonmontar` | `odysseia.dragon.owner` / `odysseia.dragon.kika` | Owner / KikaStar704 | Invoca y pilota el Dragón con WASD en 3D. |
| `/dragon speed <1-20>` | `/dragon velocidad` | `odysseia.dragon.owner` / `odysseia.dragon.kika` | Owner / KikaStar704 | Ajusta la velocidad de vuelo del dragón (hasta 20x hiper-velocidad). |
| `/dragon aliento <tipo>` | `/dragon breath` | `odysseia.dragon.owner` / `odysseia.dragon.kika` | Owner / KikaStar704 | Cambia el aliento elemental (`fuego`, `rayos`, `arboles`, `estrellas`, `hielo`, `vacío`). |
| `/niebla <on\|off\|toggle> [jugador\|all]` | `/horrorfog`, `/fog` | `odysseia.horrorfog` | `mod` / `admin` | Activa o desactiva la niebla ultra densa de terror (visión 1-2 chunks). |
| `/troll <subcomando> <jugador>` | N/A | `odysseia.troll` | `mod` / `admin` | Ejecuta trolleos (`screamer`, `fakeop`, `fakecrash`, `voidfall`, `anvil`, `creeper`, `spiders`, `lightning`). |
| `/vani [on\|off\|toggle] [jugador]` | `/vanish` | `odysseia.vanish` | `mod` / `admin` | Entra o saca a un jugador objetivo del modo Vanish invisible. |
| `/boss <spawn\|give>` | N/A | `odysseia.boss.admin` | `admin` / `owner` | Gestión, spawn manual y entrega de huevos de jefes míticos. |
| `/bloodmoon <start\|stop\|status>` | N/A | `odysseia.bloodmoon.admin` | `admin` / `owner` | Control del evento Luna de Sangre. |
| `/restart30` | N/A | `drakes.admin` | `admin` / `owner` | Reinicio seguro con avisos in-game y guardado de datos. |

### 👤 Comandos de Jugador (Rango `polis` y superiores)

| Comando | Permiso | Grupo LP | Descripción |
| :--- | :--- | :--- | :--- |
| `/kit inicial` | `odysseia.kit.use`, `odysseia.kit.inicial` | `polis` (Default) | Reclama el kit inicial de bienvenida del servidor. |

---

## 🐉 Detalles de las Monturas Dracónicas

### 1. Dragón Supremo (JackStar6677)
- **Talla**: Escala `1.4` (Colosal e Imponente).
- **Estela de Partículas**: Fuego azul (`SOUL_FIRE_FLAME`), aliento de dragón (`DRAGON_BREATH`), varas del End (`END_ROD`) y chispas de fuegos artificiales.
- **Control de Vuelo**: WASD fluido en 3D siguiendo la dirección de la mirada con estamina calculada en Rust.
- **Comandos**: `/dragon speed <1-20>` (velocidad ajustable) y `/dragon aliento <tipo>`.

### 2. Dragón Esmeralda Cute (KikaStar704)
- **Talla**: Escala `0.65` (Adorable y Compacto).
- **Estela de Partículas**: Estrellitas de la suerte (`HAPPY_VILLAGER`), composter de hojas verdes (`COMPOSTER`) y destellos mágicos.
- **Modo Especial Árboles**: Al seleccionar `/dragon aliento arboles`, cada clic izquierdo hace brotar árboles aleatorios (Roble, Abedul, Jungla, Acacia, Cerezo, Azalea) en el bloque que apunte su mirada.

---

## 🛠️ Compilación & Carga Nativa (Java 21)

### 1. Compilar el Workspace de Rust
```bash
cargo build --workspace --release
```

### 2. Ejecutar el Servidor Autónomo en Star / Linux (Docker)
```bash
cargo run --bin odysseia-server
```

### 3. Cargar la Librería Nativa en Purpur 1.21.11 (FFM API)
Java 21 utiliza la **Foreign Function & Memory API (FFM)** para vincular `odysseia_ffi.dll` (Windows) o `libodysseia_ffi.so` (Linux Star) directamente sin pausas de Garbage Collector:

```java
SymbolLookup lookup = SymbolLookup.libraryLookup(Path.of("libodysseia_ffi.so"), Arena.global());
MethodHandle isNatural = Linker.nativeLinker().downcallHandle(
    lookup.find("odysseia_is_boss_natural").get(),
    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
);
```

---

## 📜 Licencia
MIT License © DrakesCraft Labs
