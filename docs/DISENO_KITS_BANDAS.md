# Diseño de kits por bandas

Estado: **bandas 3 y 4 implementadas** el 2026-08-04. Bandas 1 y 2 sin tocar.

## El principio

El error original fue definir los Titanes como "5x" — más cantidad de lo mismo. Eso siempre
devalúa los escalones de abajo: si el rango de $45 y el de $280 hacen lo mismo con números
distintos, el del medio deja de tener sentido.

La estructura por bandas resuelve eso dando **un tipo de poder distinto** en cada una, no una
magnitud mayor. La pregunta que cada banda debe responder es *"¿qué hago aquí que no podía hacer
antes?"*, no *"¿cuánto más pego?"*.

| Banda | Rangos | Qué la define | Estado |
|---|---|---|---|
| 1 · Mortales | Hércules, Hestia, Hermes, Hefesto | Vanilla empujado. Empiezas fuerte. | Sin tocar |
| 2 · Olímpicos | Artemisa, Afrodita, Zeus | Encantamientos custom. Haces cosas que otros no. | Sin tocar |
| 3 · Panteones | Thor, Anubis, Poseidón | Slimefun medio. Llegas antes, no gratis. | **Implementada** |
| 4 · Titanes | Japeto, Oceanus, Hiperión, Cronos, Caos | Endgame. Automatizas lo que otros hacen a mano. | **Implementada** |

## La decisión crítica de la banda 4

Si el kit entrega **recursos terminados** (stacks de singularidades, lingotes infinity), mata el
Slimefun que es el corazón del servidor: el jugador compra y deja de jugar.

Si entrega **medios de producción** (máquinas, nodos de Networks, quantum storage), sigue jugando
pero desde mucho más arriba. Igual de absurdo en capacidad, sin romper la progresión.

**Se eligió la segunda.** Chetado en capacidad, no en inventario.

## Lo implementado hoy

Los 8 kits nuevos están en `config.yml` bajo `kits:`, con escalado progresivo:

| Kit | Protección | Irrompible | Vida extra | Daño extra | Dragmas |
|---|---|---|---|---|---|
| thor | 18 | 15 | +4 | +1.5 | 2.000.000 |
| anubis | 19 | 16 | +6 | +2.0 | 2.500.000 |
| poseidon | 20 | 17 | +8 | +2.5 | 3.000.000 |
| titanjapeto | 25 | 20 | +12 | +4.0 | 4.000.000 |
| titanoceanus | 28 | 22 | +16 | +5.0 | 5.000.000 |
| titanhiperion | 31 | 24 | +20 | +6.0 | 6.500.000 |
| titancronos | 35 | 26 | +26 | +7.5 | 8.000.000 |
| titancaos | **40** | 30 | **+34** | **+10.0** | 10.000.000 |

Cada uno trae armadura completa de netherita, cinco herramientas (espada, pico, hacha, pala y
mazo), ambrosía, perlas, tótems y comida. Todo `soulbound`, `unbreakable` y con `hide-flags`
para que el tooltip no sea un muro de texto.

También se añadieron los 8 alias de ProtectionStones que faltaban en `protectionstones.aliases`.
Sin eso, la acción de protección de cada compra habría fallado.

> **Ojo con `protection-alias`:** debe ser la **clave** del mapa `protectionstones.aliases`
> (`thor`), no el alias del bloque en ProtectionStones (`vipthor`). Hay un test que lo valida.

## Lo que falta: Slimefun

El motor **ya soporta** ítems de Slimefun desde hoy (ver abajo), pero los kits todavía **no los
usan**, porque los IDs hay que verificarlos en el servidor y no se pueden inventar.

Para añadirlos, en cualquier entrada de `vanilla-items`:

```yaml
      - slimefun-item: BACKPACK_LARGE   # verificar con /sf search en el juego
        amount: 1
        name: '&6Mochila del Titan'
        soulbound: true
```

**Qué agregar según la banda:**

- **Banda 3 (Panteones):** mochilas grandes, herramientas eléctricas, jetpack, traje hazmat.
  Cosas que aceleran la progresión industrial.
- **Banda 4 (Titanes):** máquinas de InfinityExpansion, nodos de Networks, quantum storage.
  Medios de producción, nunca recursos terminados.

Diferenciación sugerida dentro de los Titanes, aprovechando las habilidades de arma ya programadas:

| Titán | Dominio del kit |
|---|---|
| Japeto | Producción — máquinas base y energía |
| Oceanus | Logística — Networks, transporte, almacenamiento |
| Hiperión | Energía — reactores y generación masiva |
| Cronos | Velocidad — todo lo anterior, acelerado |
| Caos | Todo, sin límite |

## Capacidades nuevas del motor (2026-08-04)

`KitDeliveryService` acepta ahora, en cada entrada de `vanilla-items`:

| Clave | Qué hace |
|---|---|
| `slimefun-item` | ID de Slimefun. Tiene prioridad sobre `material` |
| `attributes` | Modificadores de atributo (`max_health`, `attack_damage`, …) |
| `unbreakable` | El ítem no se gasta |
| `hide-flags` | Oculta encantamientos y atributos del tooltip |

Y `enchantments` acepta **`namespace:clave`**. Antes estaba fijo a `NamespacedKey.minecraft(...)`,
así que los encantamientos de **ExcellentEnchants** nunca se resolvían. Ahora se puede escribir
`excellentenchants:cutter` o solo `cutter`, que también funciona por búsqueda de respaldo.

Slimefun se resuelve **por reflexión**: Odysseia arranca igual sin Slimefun, y un ID inexistente
falla solo en ese ítem, con aviso en el log, en vez de tumbar la entrega completa.

## Lo que NO va en el kit

**Los efectos de poción por rango van en `armor-effects`**, una sección aparte de `config.yml`.
De ahí sale el "Velocidad IV + Health Boost IV + Fuerza II" que Zeus promete en la tienda.
Si se quiere "Resistencia 40" para un Titán, va ahí, no en `vanilla-items`.

## Sobre el precio

Con las bandas definidas, *"¿cuánto vale un Titán?"* tiene respuesta: vale lo que vale saltarse
media curva de Slimefun. Recién ahí se puede comparar contra el mercado, en vez de extrapolar una
escalera aritmética desde Hércules — que fue el error de la propuesta anterior de $59,99 → $279,99.

Los 8 rangos siguen con placeholders `9000001`–`9000008` en `purchases.yml` y como
`purchaseAvailable: false` en la web. **Nada es comprable hasta que existan los paquetes en Tebex.**

## Tests que protegen esto

- `KitCatalogConsistencyTest` — falla si `purchases.yml` promete un kit que `config.yml` no define.
  Es el test que destapó los 8 kits inexistentes.
- `ConfigDefaultsMergeTest` — valida, entre otras cosas, que el `protection-alias` de cada kit
  exista en `protectionstones.aliases`.
