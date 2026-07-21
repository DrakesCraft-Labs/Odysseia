# Odysseia v1.1.0

<p align="center">
  <img src="odysseia_architecture_horizontal.svg" width="100%" alt="Odysseia Architecture Diagram" />
</p>

<p align="center">
  <strong>Núcleo Mítico, Terror Nocturno y Gestión de Producción para Purpur/Paper 1.21.x</strong><br>
  Bosses Míticos, Monturas Dracónicas, Niebla de Terror, Trolleos de Staff, Economía Slimefun y Protección de Claims.
</p>

---

## 🌟 Visión del Proyecto

**Odysseia** es la suite central que sostiene la experiencia de supervivencia, fantasía y administración en **DrakesCraft**. Unifica en una plataforma de alto rendimiento:

- ⚔️ **Panteón Mítico (17+ Bosses)**: Bosses multi-fase con habilidades complejas, drops legendarios protegidos con *Paper Item Ownership Lock* (`setOwner`) y recompensas de **5,000 XP**.
- 🐉 **Monturas Dracónicas Personalizadas**: Pilotaje 3D fluido de dragones para el Owner (**JackStar6677**) y el Dragón Esmeralda Cute para **Kika**.
- 🌫️ **Niebla Ultra Densa de Terror & Screamers**: Niebla de renderizado ultra densa (~1-2 bloques de visión) activable vía `/niebla` y eventos nocturnos aleatorios (**1 vez por día de Minecraft**).
- 🛡️ **Protección de Terreno Total**: Cancelación de eventos `EntityChangeBlockEvent` para garantizar **0% de daño a construcciones o parcelas en ProtectionStones**.
- 🎭 **Suite de Trolleos Inofensivos para Staff (`/troll`)**: Herramientas divertidas y seguras para moderadores (Fake OP, Fake Crash, Screamers, Anvil caída, Creeper siseante).
- 👻 **Vanish Avanzado (`/vani`)**: Modo invisible con control individual o por objetivos, ráfagas de partículas de portal y sonidos estéticos.
- 🛒 **Integración de Comercio & Slimefun**: Conexión con `DrakesSlimeMarket`, filtrado de ítems "Heavy/Endgame" y soporte de Dusts/Ingots.

---

## 🏛️ Arquitectura Horizontal

```
+--------------------------+    +--------------------------+    +--------------------------+    +--------------------------+
|    1. ADMIN & STAFF HUB  |    |    2. JEFES & RELIQUIAS  |    |    3. EVENTOS DE TERROR  |    |  4. INTEGRACIÓN SERVIDOR |
+--------------------------+    +--------------------------+    +--------------------------+    +--------------------------+
| • Monturas Dracónicas    | -> | • Panteón Mítico (17+)   | -> | • HorrorNightScheduler   | -> | • ProtectionStones Check |
| • Vanish & Target Control|    | • Wither Storm (Story)   |    | • Niebla Ultra Densa     |    | • DrakesSlimeMarket      |
| • Staff Troll Suite      |    | • Dragón Ancestral       |    | • Screamers & Ghost      |    | • DiosesDrakes Bridge    |
| • Tienda & Auto-Kits     |    | • Loot Anti-Robo & XP    |    | • Luna de Sangre System  |    | • LuckPerms & PAPI       |
+--------------------------+    +--------------------------+    +--------------------------+    +--------------------------+
```

---

## 📜 Comandos & Permisos (LuckPerms)

### 👑 Comandos de Creador y Staff

| Comando | Aliases | Permiso | Descripción |
| :--- | :--- | :--- | :--- |
| `/mountdragon` | `/dragon`, `/dragonmontar` | `odysseia.dragon.owner` / `odysseia.dragon.kika` | Invoca y pilota el Dragón del End personalizado. |
| `/niebla <on\|off\|toggle> [jugador\|all]` | `/horrorfog`, `/fog` | `odysseia.horrorfog` | Activa o desactiva la niebla ultra densa de terror. |
| `/troll <subcomando> <jugador>` | N/A | `odysseia.troll` | Ejecuta trolleos inofensivos (`screamer`, `fakeop`, `fakecrash`, `voidfall`, `anvil`, `creeper`, `spiders`, `lightning`). |
| `/vani [on\|off\|toggle] [jugador]` | `/vanish` | `odysseia.vanish` | Entra o saca a un jugador del modo Vanish invisible. |
| `/boss <spawn\|give>` | N/A | `odysseia.boss.admin` | Gestión y spawn de jefes míticos. |
| `/bloodmoon <start\|stop\|status>` | N/A | `odysseia.bloodmoon.admin` | Control del evento Luna de Sangre. |
| `/restart30` | N/A | `drakes.admin` | Reinicio seguro con avisos in-game y guardado de inventarios. |

---

## 🐉 Sistema de Dragones

### 1. Dragón Supremo (JackStar6677)
- **Talla**: Escala `1.4` (Colosal e Imponente).
- **Efectos**: Rastro continuo de fuego azul (`SOUL_FIRE_FLAME`), partículas dracónicas (`DRAGON_BREATH`), varas del End (`END_ROD`) y chispas de fuegos artificiales.
- **Ataques**:
  - **Clic Izquierdo**: Dispara Bolas de Fuego Dracónicas (`DragonFireball`).
  - **Shift + Clic Izquierdo**: Dispara un Rayo de la Tormenta (`lightning`) en la ubicación objetivo.

### 2. Dragón Esmeralda Cute (Kika)
- **Talla**: Escala `0.65` (Adorable y Compacto).
- **Efectos**: Rastro de estrellitas de la suerte (`HAPPY_VILLAGER`), hojas verdes (`COMPOSTER`) y brillo mágico.
- **Ataques**:
  - **Clic Izquierdo**: Ráfaga mágica de fuegos artificiales y destellos verdes.

---

## 🛠️ Compilación & Despliegue

### Requisitos
- **Java**: 21
- **Motor**: Purpur / Paper 1.21.x
- **Build Tool**: Maven 3.x

### Comando de Compilación

```bash
mvn clean package -DskipTests
```

El ejecutable compilado se ubica en `target/Odysseia-1.1.0-SNAPSHOT.jar` y se despliega directamente en `Y:\plugins\Odysseia.jar`.
