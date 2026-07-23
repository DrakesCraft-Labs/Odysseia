<p align="center">
  <img src="https://raw.githubusercontent.com/DrakesCraft-Labs/Odysseia/main/odysseia_banner.svg" width="100%" alt="Odysseia Animated Banner" />
</p>

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

## 📜 Licencia
MIT License © DrakesCraft Labs
