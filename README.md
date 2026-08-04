<p align="center">
  <img src="https://raw.githubusercontent.com/DrakesCraft-Labs/Odysseia/main/odysseia_banner.svg" width="100%" alt="Odysseia Animated Banner" />
</p>

# Odysseia Engine v1.1.0 🦀☕

<p align="center">
  <strong>Motor Mítico, Sistemas Core, Tienda, Kits y Administración para Purpur/Paper 1.21.11</strong><br>
  Arquitectura modular desacoplada: Core Odysseia + Módulos Autónomos (DiosesDrakes, DrakesBosses, DrakesArcana).
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Language-Java_21_|_Rust_2021-orange.svg" alt="Java 21 + Rust" />
  <img src="https://img.shields.io/badge/Minecraft-Paper_/_Purpur_1.21.11-brightgreen.svg" alt="Minecraft 1.21.11" />
  <img src="https://img.shields.io/badge/Server-Star_Production-blue.svg" alt="Star Production" />
</p>

---

## 🏛️ Ecosistema Canónico de Plugins DrakesCraft

Odysseia fue desacoplado en una arquitectura limpia y modular de 4 plugins autónomos:

1. **`Odysseia` (Este Repositorio)**: Motor central de DrakesCraft. Gestiona la pasarela Tebex, entrega de compras, sistema de kits, recompensas diarias, guardias de seguridad/automatización, juegos de chat, `/restart30` con guardado preventivo, y avisos de seguridad de inventario al cambiar de mundo/modalidad.
2. **`DiosesDrakes`**: Sistema divino y panteones (Greco-Romano, Nórdico, Céltico, Egipcio, Hindú). Gestiona bendiciones, habilidad de dioses, favor acumulado y anclas de Convergencia pública.
3. **`DrakesBosses`**: Arenas aisladas de Jefes en la dimensión `drakes_bosses`. Cobro de entradas con Dragmas, entrega segura de botín por buzón `/buzon`, e invocación aislada (`/bosswarp`).
4. **`DrakesArcana`**: Sistema elemental (Fuego, Agua, Aire, Tierra, Hielo, Electro). Habilidades PvE, meditación, códice y sintonía espiritual con dioses.

---

## 📌 Ramas del Repositorio

- **`main` (Rama Principal - Rust)**: Motor nativo en Rust con microservicio en Tokio + Axum para **Star** y binding FFI (`.dll` / `.so`) para Purpur 1.21.11.
- **`JAVA` (Rama Baseline Java)**: Versión original 100% Java del plugin Odysseia para Purpur.

---

## 🏛️ Estructura del Repositorio (Rust Workspace)

```text
Odysseia/
├── Cargo.toml                       # Configuration del Workspace Rust 2021
├── odysseia-core/                   # Lógica central del juego, Jefes y Políticas
│   └── src/
│       ├── lib.rs
│       ├── boss/                    # Definiciones y perfil de combate de Jefes
│       ├── horrorfog/               # Motor de niebla de terror y cordura
│       └── moderation/              # Filtros de chat y guardia de automatización
├── odysseia-ffi/                    # Binding C FFI para Java 21 (FFM / Panama)
│   └── src/lib.rs
├── odysseia-server/                 # Servidor autónomo de alto rendimiento (Tokio + Axum)
│   └── src/main.rs
├── src/main/java/                   # Implementación Java 21 (Paper / Purpur 1.21.11)
│   └── org/metamechanists/odysseia/
├── pom.xml                          # Maven build script para el JAR Bukkit/Paper
└── odysseia_banner.svg              # Banner animado vectorial SVG
```

---

## 🚀 Compilación y Ejecución

### 1. Compilar el Workspace Completo en Rust
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

## 📜 License
MIT License © DrakesCraft Labs
