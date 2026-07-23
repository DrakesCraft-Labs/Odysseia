# 🦀 Odysseia Engine (Rust Edition)

> **Motor de Alto Rendimiento para Servidores Paper / Purpur 1.21.11 & Servidor Star**  
> Desarrollado por **DrakesCraft Labs** (Jack)

---

## 📌 Ramas del Repositorio

- `main` **(Rama Principal - Rust)**: Motor nativo en Rust con microservicio para **Star** (Tokio + Axum) y binding FFI (`.dll` / `.so`) para Purpur 1.21.11.
- `JAVA` **(Rama Baseline Java)**: Versión original 100% Java del plugin Odysseia para Purpur.

---

## 🚀 Arquitectura Modular (Workspace)

| Crate / Módulo | Descripción |
| :--- | :--- |
| `crates/odysseia-core` | Los 20 Bosses, perfiles de combate, matemática 3D, monturas de dragón y filtro de chat. |
| `crates/odysseia-automation` | Guardián de Redstone, Anti-reloj espacial y filtro de protección Slimefun. |
| `crates/odysseia-horror` | Noche de terror, efectos atmosféricos del Wither Storm y Luna de Sangre. |
| `crates/odysseia-store` | Catálogo de productos (polvos Slimefun), motor de compras y entregas. |
| `crates/odysseia-telemetry` | Formateador de TPS/RAM y reporte para DiscordSRV. |
| `crates/odysseia-ffi` | Librería nativa C-ABI (`.dll` / `.so`) invocable desde Java 21 via **Project Panama / FFM API**. |
| `crates/odysseia-server` | Daemon / Microservicio independiente en Tokio + Axum corriendo en el puerto 8080. |

---

## 🛠️ Compilación y Uso

### 1. Compilar todo el Workspace en Rust
```bash
cargo build --workspace --release
```

### 2. Ejecutar el Servidor Autónomo en Star / Linux (Docker)
```bash
cargo run --bin odysseia-server
```

### 3. Cargar la Librería Nativa en Purpur (Java 21)
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
