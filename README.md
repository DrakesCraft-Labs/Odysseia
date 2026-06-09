# 🌌 Odysseia

<p align="center">
  <img src="logo.svg" width="200" alt="Odysseia Logo" />
</p>

**Odysseia** es un plugin nativo de alto rendimiento para servidores Spigot/Paper (diseñado para Purpur 1.21.1 / Java 21) que consolida y optimiza múltiples funcionalidades de personalización, automatización de eventos, reliquias y moderación creativa en un único núcleo endurecido. 

Consolida y reemplaza 5 scripts antiguos de `Skript` y hereda la lógica de webhooks del plugin `DrakesLabPresence` de forma asíncrona y segura.

---

## 🚀 Características Principales

### 👑 1. Ciclo de Prefijos del Propietario (Owner Cycle)
* **Reemplaza:** `owner_titles_cycle.sk`
* **Hardening:** Elimina el spam de comandos de consola de LuckPerms (`lp group setprefix`) que inundaba los registros de auditoría y base de datos.
* **Integración PAPI & TAB:** Expone placeholders dinámicos en memoria (`%odysseia_owner_prefix%`, `%odysseia_owner_prefix_odiseo%`, `%odysseia_owner_prefix_penelope%`) ideales para integrarse con el plugin **TAB** o chatters visuales. Alterna silenciosamente entre los prefijos configurados cada 30 minutos sólo si el respectivo propietario está online.

### 🛡️ 2. Efectos de Armadura de Rangos (Kits VIP)
* **Reemplaza:** `rank_armor_effects.sk`
* **Efectividad:** Un scheduler liviano en Java revisa los sets de armadura equipados. Si un jugador viste un conjunto completo de Diamante o Netherite y posee el permiso de kit VIP de **EssentialsX**, le otorga una serie de efectos de pociones personalizables.
* **Limpieza Visual:** Las pociones se aplican como efectos ambientales, **ocultando totalmente las partículas y el icono en pantalla** para no saturar el HUD del jugador.

### 🥔 3. Reliquia "Papa de Mar"
* **Reemplaza:** `papa_de_mar.sk`
* **Lógica:** Implementa el consumo de la histórica patata cocida con nombre personalizado (`✦ Papa de mar ✦`). Restaura hambre y saturación configuradas de forma secreta (sin lore invasivo) y emite partículas de brillo.
* **Reparto Automático:** Entrega automáticamente el ítem con `Curse of Vanishing` a todos los jugadores online en un intervalo definido.

### ⚔️ 4. Invocador de Leñador Loco
* **Reemplaza:** `lenador_loco.sk`
* **Jefe Silencioso:** Ejecuta de forma segura e interna la invocación del jefe de **LevelledMobs** mediante comandos de consola internos (`lm summon 1 vindicator <level> atPlayer <player>`) restringiendo el nivel entre 140 y 180.
* **Mantenimiento:** Un scheduler dedicado detecta cualquier entidad llamada `"Leñador Loco"` y refresca sus estados de Resistencia II, Regeneración II, Fuerza I y Velocidad I de forma silenciosa cada 5 segundos.

### 💬 5. ChatGames Automatizado
* **Reemplaza:** `chatgames_random_interval.sk`
* **Funcionamiento:** Planifica el inicio de juegos de chat (`chatgames start`) en un intervalo aleatorio (ej. 15-30 minutos) de forma completamente interna, evitando scripts repetitivos y comprobando siempre que haya al menos un jugador online para no desperdiciar recursos.

### 👻 6. Vanish Creativo (Desvanecimiento Staff)
* **Comando:** `/vanish` (alias `/v`)
* **Estética Premium:** Al desvanecerse o aparecer, el moderador genera una **doble hélice de partículas en espiral** (`PORTAL` y `DRAGON_BREATH`) acompañadas de una bocanada de humo (`CLOUD`), una mini-explosión y efectos de sonido de ilusionista.
* **Higiene de Chat:** Silencia completamente los mensajes de entrada (`PlayerJoinEvent`) y salida (`PlayerQuitEvent`) de los miembros de staff invisibles, ocultándolos de la lista de tabulación y evitando que los usuarios detecten su presencia al reconectarse en vanish.

### ⚡ 7. Moderación Creativa
* **Sanciones Inmersivas:** Al sancionar (Baneo o Kick) a un jugador en tiempo real, el plugin intercepta el evento de desconexión y genera un **rayo visual y silencioso (trueno estético)** junto con una explosión amortiguada y humo denso en la última ubicación del infractor.
* **Monitoreo en Discord:** Envía reportes interactivos por webhook a un canal de Discord específico de moderación incluyendo detalles de la sanción, razón, IP del usuario (en login) y moderador.

### 📡 8. Latidos de Presencia y Eventos (DrakesLabPresence)
* **Presencia Asíncrona:** Envía latidos de estado (`Heartbeat`) del servidor cada 30 minutos a Discord de forma asíncrona sin congelar el hilo principal del juego.
* **Eventos del Servidor:** Monitorea entradas, salidas, muertes y logros.
* **Filtrado Avanzado:** A diferencia del plugin anterior, **ignora recetas y avances internos**, publicando únicamente logros con interfaz de visualización real para el jugador. Además, tiene en cuenta el estado de `/vanish` de los moderadores para no revelar sus movimientos en Discord.

---

## 🛠️ Integraciones y Dependencias

El plugin se acopla como dependencia suave (`softdepend`) a:
* 🟢 **PlaceholderAPI (PAPI):** Necesario para renderizar los placeholders de ciclo de prefijos.
* 🟢 **LuckPerms:** Utilizado para comprobar grupos y permisos.
* 🟢 **EssentialsX:** Utilizado para comprobar los permisos de kits VIP.
* 🟢 **TAB:** Integra los placeholders visuales para mostrar dinámicamente los rangos de los dueños en la lista de jugadores y el chat.

---

## ⚙️ Configuración (`config.yml`)

El archivo de configuración permite personalizar al máximo el comportamiento del plugin:

```yaml
# Habilitar envio de eventos a Discord
discord:
  enabled: true
  webhook-url: "URL_DEL_WEBHOOK"
  webhook-moderation-url: "URL_DEL_WEBHOOK_MODERACION"

# Ciclo de Prefijos del Propietario (Owner)
owner-cycle:
  enabled: true
  interval-minutes: 30
  odiseo:
    prefix-a: "&8✠ &6&lᴏᴅɪsᴇᴏ &8✦ &6&o"
    prefix-b: "&8✠ &4&lᴅᴜᴇñᴏ &8✦ &4"
  penelope:
    prefix-a: "&d✦ &d&lᴘᴇɴᴇʟᴏᴘᴇ &8✦ &d"
    prefix-b: "&d✦ &d&l... &8✦ &d"

# Efectos de Armadura de Rangos
armor-effects:
  refresh-interval-seconds: 90
  effect-duration-seconds: 180
  # Niveles de pocion para Hercules, Hestia, Hermes, Hefesto, Artemisa, Afrodita, Zeus...
  
# Reliquias Personalizadas
papa-de-mar:
  food-restored: 5
  saturation-restored: 8.4
  delivery-interval-minutes: 15

# ChatGames Scheduler
chatgames:
  min-interval: 15
  max-interval: 30

# Presencia y Eventos (DrakesLabPresence)
presence:
  enabled: true
  server-label: "DrakesCraft"
  heartbeat-minutes: 30
  startup-delay-seconds: 60
  events:
    server-startup: true
    server-shutdown: true
    player-join: true
    player-quit: true
    player-death: true
    advancement: true
    rate-limit-per-minute: 12
```

---

## 🛡️ Hardening & Seguridad HTTP
* **Protección SSRF:** El gestor de webhooks bloquea peticiones dirigidas a rangos de IP privadas o locales (`127.0.0.1`, `localhost`, `10.x.x.x`, `192.168.x.x`, `172.16.x.x` a `172.31.x.x`) para evitar ataques de falsificación de peticiones del lado del servidor.
* **Control de Hilos:** Todas las llamadas HTTP se ejecutan de manera completamente asíncrona usando el `HttpClient` de JDK 11, evitando la ralentización de los ticks del servidor.
* **Retry-After Inteligente:** Si Discord devuelve un código de estado `429 (Too Many Requests)`, el plugin calcula el tiempo de espera. Si la espera supera los 15 segundos, el mensaje se descarta automáticamente para evitar acumular llamadas colgadas que saturen la memoria.
