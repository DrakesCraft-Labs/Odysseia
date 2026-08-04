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

## Runtime integrations

- **LuckPerms**: Group and permission management for divine and titan ranks.
- **ProtectionStones**: Land claim integration from 49x49 to 2500x2500 blocks.
- **EssentialsX**: Economy, user management, warp, and 30-day kit delivery.
- **Tebex**: Idempotent purchase delivery engine with SQLite WAL logging.
