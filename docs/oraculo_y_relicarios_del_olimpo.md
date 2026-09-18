# 🏛️ El Oráculo & Relicarios del Olimpo
## Especificación Arquitectónica y Diseño Canónico para DrakesCraft (1.21.x)

> **Estado**: Propuesta Arquitectónica Aprobada / Pendiente de Implementación  
> **Módulos Afectados**: `DrakesCrates` (Motor Virtual), `Odysseia` (Lore, Panteón y Recompensas Mitológicas), `drakescraft-web` (Catálogo y Tienda)  
> **Filosofía**: Cero bloques físicos en spawn · Llaves 100% virtuales inmateriales · Animación astral en GUI · Pity System anti-frustración · Lore mitológico integrado

---

## 1. Visión y Fundamentos del Sistema

En los servidores convencionales de Minecraft, los sistemas de *crates* se basan en cofres o bloques físicos colocados en el Spawn. Esta aproximación tradicional acarrea múltiples desventajas técnicas y de inmersión:
1. **Saturación del Spawn**: Multitudes de jugadores empujándose alrededor de cofres estáticos, bloqueando visibilidad y generando caídas de FPS.
2. **Fallas de Duplicación y Pérdida**: Llaves físicas como ítems en inventario vulnerables a bugs de caída de ítems al suelo, caídas de servidor o exploits con tolvas/mochilas.
3. **Ruptura de Inmersión**: Cajas cuadradas con texturas discordantes que chocan con la arquitectura del Spawn y el panteón clásico del servidor.

### La Solución: "El Oráculo de Delfos & Los Relicarios del Olimpo"
* **Acceso Astral Universal**: El jugador no necesita buscar un cofre físico. Escribiendo `/oraculo` (o `/relicarios`, `/crates`), el jugador consulta las visiones del Oráculo en un santuario interactivo tipo menú gráfico (GUI).
* **Ofrendas Virtuales (Llaves Inmateriales)**: Las llaves no son ítems físicos de inventario, sino balances numéricos atómicos almacenados en base de datos (`player_virtual_keys`). No ocupan ranuras de inventario ni se pueden perder.
* **Transacciones Atómicas y Blindaje**: Sistema transaccional ACID. Si el servidor se reinicia o el jugador se desconecta en medio de una animación de ruleta, el resultado se calcula previamente y se almacena en depósito seguro de reclamo (`/reclamar` o entrega diferida al reingreso).

---

## 2. Tipología de Relicarios y Tablas de Recompensas

Cada relicario corresponde a una deidad o titán del panteón de DrakesCraft, con su propia temática de drops balanceados para las economías de las 5 modalidades:

| Relicario | Ofrenda Requerida (Llave) | Origen de Obtención | Recompensas Principales |
| :--- | :--- | :--- | :--- |
| **Relicario Menor de Hércules** | *Esencia Hercúlea* | Misiones diarias, rachas `/daily`, votos al servidor (`/vote`). | Dragmas (⛁), herramientas de combate de hierro/diamante, bloques de protección pequeños (31×31), comidas gourmet de ExoticGarden. |
| **Relicario Sacro de Hestia** | *Llama Sagrada* | Canjes del PapaTrueque (`/papatrueque`), eventos de fin de semana, logros de juego. | Recursos y aleaciones avanzadas de Slimefun, cheques bancarios de Dragmas, pases temporales de incremento de hogares. |
| **Relicario Alado de Hermes** | *Pluma Celestial* | Drops raros de jefes mitológicos (`/dioses`), cajas de eventos o tienda web. | Pase temporal de vuelo (`/fly` por 24h / 7d para no-VIPs), cupones de descuento en QuickShop, cosméticos de rastro (*Plumas* y *Aliento*). |
| **Relicario de la Forja de Hefesto** | *Lingote de Vulcano* | Superación de mazmorras de Slimefun, paquetes de soporte, artesanía superior. | Componentes de alta gama de *InfinityExpansion* (matrices de singularidad, generadores de void), picos con auto-smelt y duplicación de menas. |
| **Relicario Supremo de los Titanes** | *Lágrima del Caos* | Eventos anuales masivos, derrotas del Dragón Primordial o tienda web exclusiva. | Armas y armaduras con *trim Silence* y encantamientos divinos exclusivos (*Colapso del Vacío*, *Detención Temporal de Cronos*, *Ira de Zeus*), auras 3D legendarias exclusivas (*Caos*, *Titán*), títulos permanentes. |

---

## 3. Experiencia de Usuario: El Altar del Oráculo (GUI)

### 3.1. Navegación en el Menú
Al escribir `/oraculo`:
* Se abre una interfaz de 6 filas (54 ranuras) con marco de paneles de cristal tintado en patrones azul marino y dorado celestial.
* Cada Relicario se representa con un ítem temático central y un lore que detalla:
  * Número de Ofrendas (Llaves) disponibles del jugador para ese relicario.
  * Porcentaje de probabilidades de cada categoría (Común, Raro, Épico, Legendario, Divino).
  * Progreso del contador de *Gracia Divina* (Pity Counter).
  * Clic Izquierdo: Invocar al Oráculo (Apertura individual).
  * Shift + Clic Izquierdo: Vaticinio Rápido (Apertura masiva de hasta 10 ofrendas a la vez).
  * Clic Derecho: Ver la Bóveda del Vaticinio (Vista previa de todos los premios posibles).

### 3.2. Secuencia de Animación Astral (Ruleta en GUI)
Cuando el jugador inicia una apertura individual, la GUI se transforma en una ruleta horizontal de 9 casillas:
1. **Fase I · Rito de Invocación (0.0s - 2.5s)**:
   * La hilera central rota a gran velocidad con sonidos de campanas astrales (`BLOCK_NOTE_BLOCK_CHIME` en escala ascendente).
2. **Fase II · Desaceleración del Destino (2.5s - 4.2s)**:
   * El carrusel reduce gradualmente su velocidad. Cada movimiento produce un sonido de engranaje mecánico celestial (`UI_BUTTON_CLICK` con pitch decreciente).
3. **Fase III · Epifanía Divina (4.2s - 5.0s)**:
   * La casilla central se detiene. Se detiene el sonido, suena un trueno cósmico amortiguado (`ENTITY_LIGHTNING_BOLT_THUNDER`) y el objeto victorioso se ilumina con partículas de estrellas (`FIREWORK_ROCKET_BLAST` y `END_ROD`).
   * Si la recompensa es de categoría Legendaria o Divina, se envía un anuncio respetuoso en el chat global:  
     `✦ [El Oráculo] ¡El jugador {player} ha recibido el favor de los Titanes obteniendo {item}! ✦`

---

## 4. Sistema Antifrustración (Pity System: "Gracia Divina")

Para garantizar equidad y proteger a los jugadores de la mala racha matemática:
* Cada relicario mantiene un contador individual de **Gracia Divina** por jugador.
* **Mecánica**:
  * Si un jugador realiza tiradas consecutivas sin obtener una recompensa de categoría *Legendaria* o *Divina*, cada tirada fallida aumenta la probabilidad de recompensa máxima en un **+1.5%**.
  * **Garantía Absoluta (Hard Pity)**: Al llegar a **40 tiradas consecutivas** sin premio máximo, la tirada número 41 tiene una probabilidad forzada del **100%** de otorgar una recompensa Legendaria/Divina.
  * Una vez obtenida la recompensa máxima, el contador de Gracia Divina se reinicia a 0.

---

## 5. Arquitectura Técnica de Implementación

### 5.1. Estructura de Clases (`DrakesCrates`)
```
me.jackstar.drakescrates/
├── oracle/
│   ├── OracleManager.java          # Controlador central del oráculo y transacciones
│   ├── VirtualKeyRepository.java   # Persistencia SQL/SQLite de llaves virtuales
│   ├── OracleGuiService.java       # Renderizado del altar y animaciones en GUI
│   ├── PitySystemManager.java      # Lógica de Gracia Divina y garantías progresivas
│   └── models/
│       ├── Relicary.java           # Definición de relicario, pesos y requerimientos
│       ├── OracleReward.java       # Recompensa, comandos de ejecución y visuales
│       └── VirtualKey.java         # Modelo de ofrenda virtual por UUID
└── commands/
    ├── OraculoCommand.java         # /oraculo, /relicarios
    └── OraculoAdminCommand.java    # /oraculo givekey, reload, viewpity
```

### 5.2. Persistencia en Base de Datos
```sql
CREATE TABLE IF NOT EXISTS drakes_oracle_keys (
    uuid VARCHAR(36) NOT NULL,
    relicary_id VARCHAR(32) NOT NULL,
    balance INT NOT NULL DEFAULT 0,
    PRIMARY KEY (uuid, relicary_id)
);

CREATE TABLE IF NOT EXISTS drakes_oracle_pity (
    uuid VARCHAR(36) NOT NULL,
    relicary_id VARCHAR(32) NOT NULL,
    fail_streak INT NOT NULL DEFAULT 0,
    PRIMARY KEY (uuid, relicary_id)
);

CREATE TABLE IF NOT EXISTS drakes_oracle_pending_claims (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    uuid VARCHAR(36) NOT NULL,
    reward_id VARCHAR(64) NOT NULL,
    commands_json TEXT NOT NULL,
    claimed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 5.3. Comandos de Consola y PlaceholderAPI
* **Comandos Administrativos**:
  * `/oraculo givekey <jugador> <relicario> <cantidad>`: Otorga llaves virtuales (compatible con consola y entregas de Tebex sin requerir jugador online).
  * `/oraculo takekey <jugador> <relicario> <cantidad>`: Retira llaves de balance.
  * `/oraculo setpity <jugador> <relicario> <valor>`: Ajuste manual para soporte y pruebas.
  * `/oraculo reload`: Recarga en caliente de tablas YAML sin reiniciar el servidor.
* **Placeholders para TAB y Scoreboard**:
  * `%drakescrates_oracle_keys_<relicario>%` · Retorna el saldo numérico de ofrendas.
  * `%drakescrates_oracle_pity_<relicario>%` · Retorna las tiradas acumuladas para la próxima garantía.

---

## 6. Hoja de Ruta de Desarrollo

1. **Fase 1 (Aprobación & Configuración)**: Validar las tablas de drops con la economía actual del servidor (Slimefun, Dragmas y Odysseia).
2. **Fase 2 (Motor de GUI & Llaves Virtuales)**: Implementar el repositorio de base de datos SQLite y el menú gráfico `/oraculo`.
3. **Fase 3 (Animación Astral & Pity)**: Desarrollar la máquina de estados de animación de 5 segundos con fallback por desconexión y cálculo seguro.
4. **Fase 4 (Despliegue & Integración Tebex)**: Configurar los paquetes en la tienda web (`web.drakescraft.cl/store.html`) conectando los comandos `/oraculo givekey`.
