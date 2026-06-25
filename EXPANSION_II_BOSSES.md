# 🏛️ Expansión Mítica II — Nuevos Jefes Legendarios

> **Estado:** 📋 Planificado — Próxima expansión de contenido  
> **Plugin:** Odysseia v1.x  
> **Servidor:** Purpur 1.21.1  

---

## 📜 Jefes ya implementados (Expansión I)

| Jefe | Tipo | Arma drop |
|------|------|-----------|
| Circe | Witch | — |
| Polifemo | Giant | — |
| Dios Corrupto | Wither Skeleton | — |
| Thor | Piglin Brute | ⚡ Mjolnir (Mace) |
| Ares | Vindicator | ⚔️ Filo de Ares + 🛡️ Escudo Espartano |
| Hades | Wither Skeleton | ☠️ Guadaña del Inframundo (Sharpness 255) |
| Poseidón | Drowned | 🔱 Tridente de Poseidón |
| Zeus | Wither Skeleton | ⚡ Rayo de Zeus (Mace) |
| Loki | Illusioner | 🗡️ Daga del Engaño + ✦ Cetro de la Ilusión |
| Odín | Stray | ✦ Gungnir + 👁️ Yelmo de la Sabiduría |
| Kratos | Piglin Brute | 🔥 Espadas del Caos + ❄️ Hacha Leviatán |

---

## 🆕 Expansión II — Jefes Planificados

### 🌈 1. Heimdall — Guardián del Bifröst

**Entidad:** `STRAY` | **HP:** 800 | **Daño base:** 18 | **Bossbar:** `WHITE`

> *"Ningún ser pasa el Bifröst sin que yo lo vea. Soy el ojo que nunca duerme, el oído que todo escucha."*

**Tipo de invocación:** `/boss spawn heimdall`

#### Habilidades

| Skill | Descripción |
|-------|-------------|
| `BifrostBeamSkill` | Dispara un haz del arcoíris (ShulkerBullet) que teletransporta al jugador 15 bloques atrás + Ceguera III + Náusea III |
| `RainbowStrikeSkill` | Lluvia de 7 colores: 7 rayos + 7 fuegos artificiales de colores distintos en posiciones aleatorias alrededor del jugador |
| `OmniscienceSkill` | Todos los jugadores en el mapa quedan marcados con Glowing III + partículas doradas mientras Heimdall vive. Nadie puede esconderse |
| `BifrostTeleportSkill` | Se teletransporta al jugador más alejado dejando un rastro de partículas de arcoíris |
| `HornBlastSkill` | Toca el Gjallarhorn: Knockback IV a todos en radio 15 + los lanza por el aire + sonido ELDER_GUARDIAN_CURSE |

#### Drops
- `MACE` — **✦ Gjallarhorn** — Sharpness 10, Knockback 8, Unbreaking 10, Mending
- `ELYTRA` — **✦ Alas del Bifröst** — Protection 10, Unbreaking 10, Mending

---

### 🐍 2. Hidra — Serpiente de Lerna

**Entidad:** `RAVAGER` | **HP:** 1200 | **Daño base:** 25 | **Bossbar:** `GREEN`

> *"Córtame una cabeza... y nacerán dos. Soy eterna. Soy inevitable."*

**Tipo de invocación:** `/boss spawn hidra`

#### Mecánica especial — Regeneración
Al bajar al **66% HP** y al **33% HP**:
- Se cura **300 HP** instantáneamente
- Convoca **2 Hidras mini** (Ravagers 200 HP) que deben morir antes de poder matar a la principal
- Efecto visual de humo verde + sonido de spawn

#### Habilidades

| Skill | Descripción |
|-------|-------------|
| `HydraRegenSkill` | Cada 15s se regenera 50 HP + aplica Poison V (amplifier 4) a todos en radio 15 |
| `VenomBreathSkill` | Escupe veneno en cono de 120° hacia el jugador más cercano. Todos en el cono: Poison V 30s + Wither III 10s + partículas de ácido |
| `TailSwipeSkill` | Coletazo en radio 10: todos vuelan 5 bloques + daño 15 |
| `HeadRushSkill` | Carga en línea recta haciendo 30 de daño a todo lo que toca + Slowness IV 10s al impactado |
| `AcidPoolSkill` | Convierte bloques en radio 5 a Slime Block (charco de ácido) + aplica Poison IV pasivo a quien esté encima |
| `NecroticBiteSkill` | Muerde al jugador más cercano: daño 40 + Wither IV + Weakness V + Slowness V 15s |

#### Drops
- `NETHERITE_SWORD` — **🐍 Colmillo de la Hidra** — Sharpness 15, Fire Aspect 3, Looting 8, Unbreaking 10, Mending
- `TURTLE_HELMET` — **🐍 Escama de la Hidra** — Protection 12, Thorns 5, Unbreaking 10, Mending

---

### 🐕 3. Cerbero — El Guardián del Inframundo

**Entidad:** `RAVAGER` (x3 vinculados) | **HP:** 900 total (300/cabeza) | **Daño base:** 22 | **Bossbar:** `PURPLE`

> *"Las puertas del Hades no se abren para los vivos. Yo me encargo de eso."*

**Tipo de invocación:** `/boss spawn cerbero`

#### Mecánica especial — Tres Cabezas
- Se spawnean **3 Ravagers** simultáneamente que comparten bossbar
- Si muere una cabeza, las otras 2 entran en **furia**: Speed III + Strength V
- El boss solo está "derrotado" cuando las 3 cabezas mueren
- El drop aparece solo cuando cae la última

#### Habilidades

| Skill | Descripción |
|-------|-------------|
| `HellhoundBiteSkill` | Muerde ferozmente al jugador más cercano: daño 25 + Wither V + Poison IV + Weakness III |
| `InfernalHowlSkill` | Aullido: Darkness + Blindness a todos radio 20 + sonido WITHER_SPAWN + invoca 3 Zombie Wolves con nombre `☠ Espectro de Cerbero` |
| `SoulScentSkill` | Rastrea al jugador que intenta huir (el más alejado): teleport instantáneo a él + lo marca con partículas rojas |
| `ThreeHeadFurySkill` | Las 3 cabezas atacan simultáneamente: 3 hits rápidos en 0.5s al jugador más cercano (20 daño c/u) |
| `NetherChainSkill` | Encadena al jugador: Slowness X + Mining Fatigue V durante 8s + partículas de cadenas |
| `GuardianRoarSkill` | Ruge: todos los mobs en radio 30 se agreden a los jugadores + todos los jugadores reciben Fear (Darkness 10s) |

#### Drops
- `NETHERITE_CHESTPLATE` — **🐕 Piel de Cerbero** — Protection 15, Thorns 8, Fire Protection 10, Unbreaking 10, Mending
- `BONE` x32 especiales — **🐕 Hueso del Inframundo**

---

### 🏹 4. Artemisa — Diosa de la Caza

**Entidad:** `SKELETON` | **HP:** 600 | **Daño base:** 15 (distancia) | **Bossbar:** `BLUE`

> *"Ninguna presa escapa de mi arco. Eres solo la próxima."*

**Tipo de invocación:** `/boss spawn artemisa`

#### Habilidades

| Skill | Descripción |
|-------|-------------|
| `MoonArrowSkill` | Dispara 12 flechas en arco 360° que aplican Slowness V + Poison III al impactar |
| `HuntressMarkSkill` | Marca al jugador con más HP: todas las flechas de Artemisa tienen tracking hacia él + brilla con partículas de luna |
| `LunarStrikeSkill` | Invoca la luna: Darkness + Blindness a todos + 20 flechas caen del cielo (desde Y+30) sobre posiciones aleatorias en radio 15 |
| `DianaSprintSkill` | Sprint divino: Speed V al boss durante 5s + invulnerabilidad parcial (Resistencia IV) |
| `WolfPackSkill` | Invoca 5 lobos sagrados de Artemisa (Wolves en modo agresivo con nombre `🐺 Lobo de Diana`) |

#### Drops
- `BOW` — **🌙 Arco Lunar de Artemisa** — Power 10, Infinity, Flame, Punch 5, Unbreaking 10, Mending
- `ARROW` x64 — **🌙 Flecha de Diana** — con lore especial

---

### 🌋 5. Tifón — El Padre de los Monstruos

**Entidad:** `GIANT` | **HP:** 2000 | **Daño base:** 35 | **Bossbar:** `RED`

> *"Soy anterior a los dioses. Soy el caos que los precedió. El Olimpo tiembla ante mi nombre."*

**Tipo de invocación:** `/boss spawn tifon`

#### Habilidades

| Skill | Descripción |
|-------|-------------|
| `DragonBreathSkill` | Escupe fuego en línea recta 20 bloques: bloques en línea se convierten en lava + Fire 30s + daño 20 a todo en el rayo |
| `EarthShatterSkill` | Golpea el suelo: 8 TNT visual sin daño a bloques pero lanza a todos por el aire + daño 30 en radio 12 |
| `HundredHeadsFurySkill` | **Pasiva <50% HP:** ataca 2x más rápido + cada golpe aplica Wither III al objetivo |
| `MonsterCallSkill` | Invoca una ola: 3 Ravagers + 2 Wolves furiosos + 5 Zombies + 5 Skeletons con nametag de monstruos |
| `MountainThrowSkill` | Lanza una roca de Obsidiana (FallingBlock) que explota al impactar el suelo causando 50 de daño |
| `VolcanicEruptionSkill` | **Habilidad apocalíptica:** 30 bloques de lava (FallingBlocks) caen del cielo en radio 15 alrededor del boss durante 3 segundos |

#### Drops
- `NETHERITE_SWORD` — **🌋 Garra de Tifón** — Sharpness 20, Fire Aspect 5, Looting 10, Unbreaking 10, Mending
- `NETHERITE_CHESTPLATE` — **🌋 Coraza del Padre Monstruo** — Protection 20, Thorns 10, Fire Protection 10, Unbreaking 10, Mending
- `TOTEM_OF_UNDYING` x2 con lore especial

---

### 🔥 6. Prometeo — El Titán del Fuego Robado

**Entidad:** `BLAZE` | **HP:** 700 | **Daño base:** 20 | **Bossbar:** `YELLOW`

> *"Robé el fuego a los dioses para dárselo a los mortales. Ahora ese mismo fuego os consumirá."*

**Tipo de invocación:** `/boss spawn prometeo`

#### Mecánica especial — Fénix
- Al llegar al **10% HP** por primera vez: se "revive" con 300 HP, efecto de llamas épico (fuegos artificiales + humo), cura total
- Solo puede morir en la segunda vida

#### Habilidades

| Skill | Descripción |
|-------|-------------|
| `StolenFireSkill` | Lanza 8 Fireballs en todas direcciones + aplica Fire 20s a todos en radio 10 |
| `TitanFlameSkill` | Se rodea de fuego: convierte bloque bajo sus pies en lava + aplica Fire a quien se acerque a 3 bloques |
| `EternalPunishmentSkill` | Maldición: el jugador con más HP pierde 2❤️ cada segundo durante 30s (daño real programado) |
| `PhoenixRiseSkill` | **Especial 10% HP:** Resurrección épica — 300 HP, fuegos artificiales naranjas/rojos, sonido WITHER_SPAWN, invulnerabilidad 3s |
| `CelestialFireballSkill` | Lanza un Ghast Fireball teledirigido al jugador (velocidad ajustada hacia el target) |

#### Drops
- `BLAZE_ROD` — **🔥 Llama Eterna de Prometeo** — Power 10 (trato como arco de fuego), lore épico
- `FIRE_CHARGE` x32 — **🔥 Chispa Robada** — lore especial

---

## 🛠️ Habilidades Extra para Jefes Existentes

### ⚡ Thor — Skills adicionales
| Skill | Descripción |
|-------|-------------|
| `ThunderDomeSkill` | 12 rayos simultáneos en círculo perfecto de radio 8 alrededor del boss |
| `AsgardWrathSkill` | Rayo directo a **cada jugador** del servidor al mismo tiempo |
| `ThunderEmpowerSkill` | Se bufea: Speed III + Strength V + Resistencia III al boss por 20s + partículas de tormenta |

### ⚔️ Ares — Skills adicionales
| Skill | Descripción |
|-------|-------------|
| `WarCrySkill` | Grito de guerra: Weakness IV + Slowness IV a todos en radio 15 + sonido WITHER + partículas de fuego |
| `ArenaOfBloodSkill` | Convierte el suelo en radio 8 en Netherrack ardiendo temporalmente durante 10s |

### ☠️ Hades — Skills adicionales
| Skill | Descripción |
|-------|-------------|
| `PlagueSweepSkill` | 8 cráneos de Wither en todas direcciones + Wither V + Poison V 30s en área |
| `GrimReaperDashSkill` | Teleport detrás del jugador más cercano + 3 hits rápidos (20 daño c/u) + Wither IV |

### 🔱 Poseidón — Skills adicionales
| Skill | Descripción |
|-------|-------------|
| `OceanCurseSkill` | Inunda el área con agua temporal + Mining Fatigue V + Slowness V a todos |
| `AbyssalDarknessSkill` | Ceguera III + Darkness + Náusea III a todos en radio 25 por 15s |
| `KrakenCallSkill` | Invoca 5 Elder Guardians con nombre `✦ Tentáculo del Kraken ✦` |

### ⚡ Zeus — Skills adicionales
| Skill | Descripción |
|-------|-------------|
| `OlympusJudgementSkill` | 40 rayos en espiral en 3 segundos — efecto apocalíptico |
| `DivineSmiteSkill` | Rayo directo a cada jugador + daño 30 + lanzados al aire |

### 🃏 Loki — Skills adicionales
| Skill | Descripción |
|-------|-------------|
| `PsychedelicRealmSkill` | Náusea V + Darkness + sonidos de Note Block aleatorios a todos en radio 20 |
| `TricksterTrapsSkill` | 10 TNT distribuidas en la arena que explotan al pisarlas |

### 👁️ Odín — Skills adicionales
| Skill | Descripción |
|-------|-------------|
| `AllFatherVisionSkill` | Todos los jugadores brillan (Glowing) + partículas doradas — no pueden esconderse |
| `RagnarokSkill` | **Skill de fase final (<20% HP):** rayos masivos + mob wave + buffs extremos — caos total |

### ⛓️ Kratos — Skills adicionales
| Skill | Descripción |
|-------|-------------|
| `SpartanFurySkill` | 5 golpes en 1 segundo al target más cercano (15 daño c/u) |
| `GoWRageSkill` | Modo God of War: invulnerabilidad 5s + todos los golpes hacen 100 de daño |

---

## ⚙️ Notas técnicas de implementación

```
Patrón de implementación por boss:
1. boss/instances/NombreBoss.java    — extiende OdysseyBoss
2. boss/skills/NombreSkill.java      — implementa BossSkill (uno por skill)
3. BossManager.java                  — registro en spawnBoss() + createCustomDrops()
4. BossCommand.java                  — validación + tab-complete
5. OdysseyItemManager.java           — métodos createNombreItem()
```

- **Enchants:** niveles absurdos son válidos (Sharpness 255, Protection 20, etc.) — servidor Purpur los acepta
- **Efectos de pociones:** amplifier 9 = nivel X en pantalla
- **HP de bosses:** via `AttributeInstance.setBaseValue()` en `Attribute.GENERIC_MAX_HEALTH`
- **Partículas masivas:** `world.spawnParticle()` con count alto para efectos épicos
- **Sonidos épicos:** `ENTITY_WITHER_SPAWN`, `ENTITY_ELDER_GUARDIAN_CURSE`, `UI_TOAST_CHALLENGE_COMPLETE`
- **Schedulers:** `Bukkit.getScheduler().runTaskLater()` para efectos en cascada
- **Todos los items:** llevan `Mending I + Unbreaking 10` como mínimo

---

## 📦 Comandos de deploy

```bash
# Compilar
mvn clean package -DskipTests

# Deployar al servidor
cp target/Odysseia-1.0.0-SNAPSHOT.jar Z:/plugins/Odysseia.jar

# Push a GitHub
git add -A && git commit -m "feat: ..." && git push origin main

# Avisar jugadores antes de reiniciar (SIEMPRE)
python send_command.py "say [Reinicio] Reinicio en 2 minutos para nuevos jefes..."
# Esperar 2 minutos
python send_command.py "restart30"
```
