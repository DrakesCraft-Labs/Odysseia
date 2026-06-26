# 🏛️ Expansión Mítica III — Rediseño profundo de jefes

> **Estado:** 📋 Spec accionable (brief de Jack, 2026-06-26) — pendiente de implementar.
> **Plugin:** Odysseia · **Servidor:** Purpur 1.21.11
> **Regla:** NO romper lo que ya funciona. El sistema de **experiencia (5000 XP) queda igual** (Jack: "está genial, nada que discutir").

Implementar por fases y **compilar/verificar entre cada fase** (un boss roto tumba el plugin entero al cargar).

---

## 1. Drops — el problema #1

**Síntoma (Jack):** "las armas/items no siempre se dropean" + "los encantamientos y los drops no cumplen lo que hacen".

- **Garantizar el drop principal:** hoy `BossManager.onDeath` hace `event.getDrops().clear()` + `addAll(createCustomDrops())`. Si el boss muere por void/fuego/otro plugin, el arma puede perderse. Fijar el drop **soltándolo manualmente** en la ubicación de muerte (`world.dropItemNaturally`) además del evento, para que nunca se pierda.
- **Probabilidad configurable por rareza:** arma legendaria 100%, armadura 100%, materiales/extra con `chance` en `config.yml` (`drops.<boss>.<item>.chance`).
- **Auditar que cada item cumple su lore:** revisar uno por uno que enchant + efecto del listener coincidan con lo que dice el lore. Hoy varios lores prometen efectos que no existen en `BossItemListener` (ver §6).
- **Anti-robo:** el drop del jefe solo lo puede recoger quien hizo más daño (tracking de daño por jugador) o el killer — evita que un random se lleve el arma. Configurable.

## 2. Daño y tankiness — equiparar a InfernalMobs / LeveledMobs

**Síntoma:** "los bosses tienen que hacer mucho daño, así absurdo, comparado con InfernalMobs y LeveledMobs".

- Subir `GENERIC_ATTACK_DAMAGE` base de todos (x2–x3 actual) y escalar con la fase.
- `GENERIC_KNOCKBACK_RESISTANCE = 1.0` en todos (ya en los nuevos; falta en Expansión I).
- Daño de skills revisado hacia arriba; los golpes de fase final deben ser letales si no se esquivan.
- **Escala de daño por nº de jugadores** (más gente = más daño/HP) para que no se zerguee.
- Considerar `GENERIC_ARMOR` alto en los tanques (Tifón, Cerbero, Hidra).

## 3. Sistema de fases (TODOS los bosses)

Hoy solo algunos tienen 1 fase especial. Estandarizar **3 fases** mínimo por boss vía un helper en `OdysseyBoss`:

- **Fase 1 (100–66%):** kit base de skills.
- **Fase 2 (66–33%):** + buff propio (Speed/Strength), nuevas skills, más partículas, frase de chat.
- **Fase 3 (<33%, "furia"):** skills apocalípticas, invulnerabilidad breve al entrar, cambio visual (partículas constantes), diálogo.
- Hook `onPhaseChange(int phase)` en `OdysseyBoss` para centralizar anuncio + efectos + sonido.

## 4. Diálogos en chat (sin spam)

**Síntoma:** "que hablen de vez en cuando, más información, pero que no spameen".

- `BossDialogue` con cooldown global (p. ej. 1 frase cada 20–30s máx) y frases temáticas por boss:
  - Al spawnear, al cambiar de fase, al matar a un jugador, al estar cerca de morir.
- Formato: `§8[§6Mítico§8] §f<NombreBoss>§7: §f"<frase>"`. Pool de frases por boss en una clase o `config.yml`.
- Anuncios de fase y de mecánica (regen, resurrección) ya existen — unificar bajo el mismo sistema con cooldown.

## 5. Más skills / partículas / efectos (TODOS)

- **Efectos de cadena** (Jack lo pidió explícito): skills que encadenan (ej. raíces/cadenas que inmovilizan + tiran del jugador), combos de 2–3 efectos secuenciales con `runTaskLater`.
- **Más pociones:** combinar Wither/Poison/Slowness/Weakness/Mining Fatigue/Darkness/Nausea según el tema del boss.
- **Más efectos de área:** zonas persistentes (charcos, fuego, vacío) que dañan mientras estés dentro (task con duración).
- **Partículas:** subir `count`, usar `DustOptions` temáticos por boss, rastros, auras constantes por fase. (Validar nombres de `Particle` en 1.21.11.)
- Cada boss debería tener **6–8 skills** (hoy 4–6).

## 6. Auditar items/armas — que cumplan su lore + más funcionalidad

Revisar `BossItemListener` y `OdysseyItemManager` item por item:

- **Lanzas reales (1.21.11):** Odin debe llevar una **Lanza** (`Material.*_SPEAR` — verificar enum exacto en Purpur 1.21.11; netherita) en vez de tridente. El tridente "Gungnir" no tiene sentido temático.
  - Implementar **lanzar la lanza como tridente** vía plugin (la spear vanilla NO se lanza sola): al click-derecho, lanzar proyectil custom + rayo al impactar (reusar lógica del `odin_spear` actual).
  - Lo mismo para otras armas que Jack quiera lanzables (ej. "HLB Advanced" lanzable como tridente).
- **Encantamientos que no cumplen:** auditar cada `addEnchant`; niveles absurdos (Sharpness 50/255) a veces no escalan daño real en Purpur — verificar y, si no, sustituir por daño custom en el listener.
- **ExcellentEnchants (plugin instalado):** usar sus encantamientos custom en los drops (verificar API/cómo aplicarlos por NBT o comando). Hay encantamientos nuevos vanilla 1.21.11 también — revisar el enum `Enchantment`.
- Cada arma legendaria debe tener **al menos un poder activo** real en el listener (hoy varias solo tienen stats).

## 7. Nuevo jefe — Coloso del End (Enderman gigante)

**Jack:** "algún jefe que sea un Enderman alto y grande, que sea grande".

- **Entidad:** `ENDERMAN` con `GENERIC_SCALE` alto (2.5–3.0) → enderman colosal. HP ~1500, BarColor `PURPLE`.
- Skills temáticas: teleport masivo, recoger/lanzar bloques, lluvia de enderpearls que teletransportan al jugador, robo de visión (Darkness), invoca endermites/endermen, zona de vacío.
- Drop: arma/armadura del End (ej. "Cetro del Vacío", elytra del End).

## 8. Skins / presentación

- Equipar a todos con armadura temática (color de cuero teñido por boss, o netherita) + arma en mano coherente.
- `GENERIC_SCALE` por boss para tamaños distintivos.
- Nombres con formato y símbolos consistentes; bossbar `style`/`color` por tema.
- (Skins de jugador-cabeza para los humanoides si se quiere — opcional, vía textura de head.)

## 9. Orden sugerido de implementación (fases verificables)

1. **Helpers en `OdysseyBoss`:** sistema de fases (`onPhaseChange`), diálogos con cooldown, daño escalado por jugadores, efectos de área reutilizables, drop garantizado. Compilar.
2. **Drops + items:** garantizar drop, auditar lore↔efecto, lanza de Odin + lanzables, ExcellentEnchants. Compilar + probar en vivo.
3. **Subida de daño/tankiness global.** Compilar.
4. **Rediseño boss por boss** (un commit por boss): 3 fases, 6–8 skills, partículas, diálogos. Compilar + probar **cada uno** antes de seguir.
5. **Nuevo Coloso del End.** Compilar + probar.
6. Documentar y cerrar.

## 10. Verificación previa obligatoria

- Confirmar en Purpur 1.21.11 el **enum exacto** de: lanzas (`Material`), encantamientos nuevos (`Enchantment`), partículas usadas.
- Revisar la **API de ExcellentEnchants** para aplicar sus encantos a items.
- Probar daño real de Sharpness alto vs daño custom antes de decidir el approach de cada arma.

---

### Referencia técnica — lanza 1.21.11
Update "Mounts of Mayhem": la lanza es arma vanilla por tiers (madera→netherita, +copper), melee con jab/charge y alcance extendido; **no** es lanzable de forma vanilla (requiere lógica de plugin). Fuente: minecraft.wiki/w/Spear, minecraft.net 1.21.11.
