<p align="center">
  <img src="https://raw.githubusercontent.com/DrakesCraft-Labs/Odysseia/main/odysseia_banner.svg" width="100%" alt="Odysseia animated banner" />
</p>

# Odysseia

**The operational Paper/Purpur suite behind DrakesCraft.** Odysseia centralises the server-side systems that need consistent permissions, protected-world behaviour, durable purchase delivery, and clear staff controls.

> Runtime target: Java 21, Paper/Purpur 1.21.11. The shipped Minecraft plugin is the Java Maven artifact. This repository also contains a Rust workspace for research and future services; it is not required to boot the current Bukkit JAR.

<p align="center">
  <strong>Motor Mítico, Sistemas Core, Tienda, Cosméticos, Kits y Administración para Purpur/Paper 1.21.11</strong><br>
  Arquitectura modular desacoplada: Core Odysseia + Módulos Autónomos (DiosesDrakes, DrakesBosses, DrakesArcana).
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Language-Java_21_|_Rust_2021-orange.svg" alt="Java 21 + Rust" />
  <img src="https://img.shields.io/badge/Minecraft-Paper_/_Purpur_1.21.11-brightgreen.svg" alt="Minecraft 1.21.11" />
  <img src="https://img.shields.io/badge/Server-Star_Production-blue.svg" alt="Star Production" />
</p>

## 🏛️ Ecosistema Canónico de Plugins DrakesCraft

Odysseia fue desacoplado en una arquitectura limpia y modular de 4 plugins autónomos:

1. **`Odysseia` (Este Repositorio)**: Motor central de DrakesCraft. Gestiona la pasarela Tebex, entrega de compras, sistema de kits, cosméticos visuales (/cosmeticos), recompensas diarias, guardias de seguridad/automatización, juegos de chat, `/restart30` con guardado preventivo, y avisos de seguridad de inventario al cambiar de mundo/modalidad.
2. **`DiosesDrakes`**: Sistema divino y panteones (Greco-Romano, Nórdico, Céltico, Egipcio, Hindú). Gestiona bendiciones, habilidad de dioses, favor acumulado y anclas de Convergencia pública.
3. **`DrakesBosses`**: Arenas aisladas de Jefes en la dimensión `drakes_bosses`. Cobro de entradas con Dragmas, entrega segura de botín por buzón `/buzon`, e invocación aislada (`/bosswarp`).
4. **`DrakesArcana`**: Sistema elemental (Fuego, Agua, Aire, Tierra, Hielo, Electro). Habilidades PvE, meditación, códice y sintonía espiritual con dioses.

---

## Alcance real de Odysseia

Odysseia no es solamente un plugin de bosses ni un conjunto de comandos. Es el
plano de ejecucion de DrakesCraft: coordina sistemas que deben compartir una
misma identidad de jugador, reglas economicas, seguridad y trazabilidad.

| Dominio | Capacidades activas |
| --- | --- |
| Compras | Catalogo canonico, recepcion Tebex, idempotencia por transaccion, acciones con estado, reintentos, historial, `dry-run`, refund y chargeback. |
| Identidad | Resolucion Java/Bedrock por nombre y UUID, soporte del prefijo Floodgate y entregas online/offline. |
| Rangos y kits | Kits inicial, mensuales y administrativos; entregas pendientes; validacion de catalogo; SFMaster temporal y vigilancia de expiracion. |
| Modalidades | Menu y rutas entre Survival, SkyBlock y OneBlock; bovedas aisladas; bloqueo de almacenamientos y objetos entre modalidades. |
| Economia | Tienda principal, venta de inventario, Papa de Mar y comercio controlado; rate limits y guardias contra automatizacion/exploits. |
| Operacion | Reinicio seguro con cuenta regresiva, guardado preventivo, maintenance window, telemetria a Star y alertas de expiracion VIP. |
| Seguridad | Anti-alt configurable, deteccion AFK/automatizacion, limites de spawners, proteccion de Fast Machines, items corruptos, mochilas y capturas de bosses. |
| Protecciones | Entrega de ProtectionStones, bordes visuales, aliases compatibles y controles de mundo/proteccion. |
| Comunidad | Cosméticos, recompensas diarias, juegos de chat estacionales, filtro de chat, mensajes de muerte y anuncios de compras. |
| Eventos | Luna de Sangre, Horror Night, niebla, meteoritos, dragones privados, trolls inocuos y herramientas del dueño. |
| Compatibilidad | LuckPerms, Vault, Essentials, Slimefun, ProtectionStones, DiscordSRV, WorldwideChat, nLogin, Floodgate y PlaceholderAPI. |

### Purchase Engine

El motor de compras es transaccional, no una lista de comandos Tebex. El
catalogo `purchases.yml` define productos y acciones; SQLite conserva cada
transaccion y cada accion de manera independiente. Una repeticion del mismo
evento no duplica beneficios, una entrega parcial puede reintentarse y
refund/chargeback siguen una politica auditable. Los tests rechazan IDs Tebex
placeholder o duplicados, kits inexistentes y conteos de productos incoherentes.

Tebex cobra. Odysseia valida, decide y entrega. La web refleja el catalogo, pero
no reemplaza al motor de ejecucion.

### Seguridad economica y de inventarios

- `CommerceCommandLimiter` y `CommerceRateLimiter` limitan spam y volumen.
- Los guardias de modalidad impiden mover storages u objetos entre economias.
- SFMaster se controla por rango, tiempo, cantidad, tipo y propiedad del item.
- Los kits administrativos pueden probarse sin entregar dinero, rango o claim.
- Los reinicios abren una ventana de mantenimiento que bloquea interacciones
  peligrosas mientras los plugins persisten sus datos.
- Las auditorias separan errores operativos de intentos de abuso reproducibles.

### Experiencia del jugador y del staff

Los comandos publicos cubren modalidades, kits, recompensas, tienda,
cosmeticos, venta, bordes y compra. El namespace administrativo cubre compras,
entregas pendientes, mantenimiento, SFMaster, alertas VIP y recarga segura.
Los comandos destructivos o de espectaculo (`/meteorito`, `/auradueno`,
`/ultragod`, `/troll`) permanecen detras de permisos explicitos.

## Arquitectura interna

El JAR Java se divide por dominio: `purchase`, `kits`, `modalities`, `vaults`,
`economy`, `listeners`, `integrations`, `restart`, `chatgames`, `cosmetics`,
`dragon`, `events`, `papa` y `services`. Los sistemas de combate aun presentes
son compatibilidad de transicion; la autoridad nueva de encuentros es
DrakesBosses y la progresion divina pertenece a DiosesDrakes.

El workspace Rust contiene siete crates especializados:

- `odysseia-core`: reglas puras de bosses, chat, items, moderacion y calculos.
- `odysseia-store`: primitivas del dominio comercial.
- `odysseia-automation`: logica de automatizacion.
- `odysseia-horror`: reglas de eventos de terror.
- `odysseia-telemetry`: contratos de observabilidad.
- `odysseia-ffi`: frontera nativa para Java.
- `odysseia-server`: proceso experimental independiente.

El servidor Bukkit funciona sin estos binarios nativos. El fallback Java sigue
siendo la autoridad y evita que una biblioteca faltante impida arrancar.

## Calidad y verificaciones

La suite cubre Purchase Engine, catalogo Tebex, identidad Bedrock, kits,
modalidades, comercio, automatizacion, anti-alt, reinicios, cosméticos,
chatgames, dragones, protecciones y compatibilidad de particulas. La validacion
normal es:

```bash
mvn clean package
cargo test --workspace
```

Compilar no despliega. Los cambios de `config.yml` productivo se fusionan de
forma quirurgica y los JAR se activan solamente en una ventana de reinicio.

## Runtime integrations

- **LuckPerms**: Group and permission management for divine and titan ranks.
- **ProtectionStones**: Land claim integration from 49x49 to 2500x2500 blocks.
- **EssentialsX**: Economy, user management, warp, and 30-day kit delivery.
- **Tebex**: Idempotent purchase delivery engine with SQLite WAL logging.
