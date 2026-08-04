# Paquetes pendientes de crear en Tebex

Estado al **2026-08-04**. Fuente de verdad de lo que hoy se vende: Headless API de Tebex
(`https://headless.tebex.io/api/accounts/<token>/categories?includePackages=1`), 25 paquetes vivos.

---

## 1. URGENTE: paquetes que se cobran y NO se entregan

Tebex ya vende estos tres, pero el `purchases.yml` **de producción** no los conoce, así que
`PurchaseEngine` no puede mapear el webhook y el jugador paga sin recibir nada.

| ID Tebex | Paquete | Precio |
|---|---|---|
| 7596916 | Coloso del Nether 501x501 | $19.99 |
| 7596920 | Coloso del End 501x501 | $24.99 |
| 7596924 | Dominio de Atlas 1001x1001 | $49.99 |

**Ya corregido en el repo** (`protection_nether_colossus`, `protection_end_colossus`,
`protection_atlas` con sus IDs reales). Se arregla solo al desplegar el `purchases.yml` nuevo.
Hasta entonces, conviene ocultar esos tres paquetes en Tebex o revisar si alguien ya los compró.

---

## 2. Paquetes por crear

Estos 8 rangos existen en el catálogo de Odysseia con **IDs placeholder 9000001–9000008**.
Al crear cada uno en el panel de Tebex hay que copiar el ID real a `purchases.yml` y cambiar
`verification` a `VERIFIED_PRODUCTION`.

### Escalera de precios propuesta

La línea actual sube ~1.4× por escalón y termina en Zeus $44.99. Los Titanes se anunciaron como
**5x más potentes**, así que tienen que quedar claramente por encima. Propuesta:

| Placeholder | Producto | Grupo LuckPerms | Precio propuesto | Isla (rango) |
|---|---|---|---|---|
| 9000001 | Thor | `thor` | $59.99 | 280x280 |
| 9000002 | Anubis | `anubis` | $74.99 | 310x310 |
| 9000003 | Poseidón | `poseidon` | $94.99 | 340x340 |
| 9000004 | Titán Japeto | `titanjapeto` | $119.99 | 400x400 |
| 9000005 | Titán Oceanus | `titanoceanus` | $149.99 | 450x450 |
| 9000006 | Titán Hiperión | `titanhiperion` | $179.99 | 500x500 |
| 9000007 | Titán Cronos | `titancronos` | $219.99 | 550x550 |
| 9000008 | Titán Caos | `titancaos` | $279.99 | 600x600 |

> La web muestra los precios como CLP con la convención `clp = usd * 1000`
> (`catalog/store-catalog.js`). Hay que agregar ambos campos a cada producto nuevo.

### Configuración de cada paquete en Tebex

Todos los rangos son **pases manuales de 30 días, sin renovación automática**, igual que la línea
actual. Eso ya está reflejado en `purchases.yml` (`LUCKPERMS_TEMPORARY`, `duration: 30d`).

En el panel, para cada uno:

1. Categoría: **Rangos Divinos** (Thor, Anubis, Poseidón) o una nueva **Titanes Primordiales**.
2. Tipo: pago único, sin suscripción.
3. Comando al comprar: la entrega la hace Odysseia por su motor de compras, no Tebex.
   No agregar comandos en el panel salvo que se quiera un fallback.
4. Copiar el ID del paquete creado y reemplazar el placeholder en `purchases.yml`.

---

## 3. Contradicciones a resolver antes de publicar

Detectadas entre lo anunciado en Discord, lo que dice la web y lo que entrega el servidor:

- El anuncio de Discord promete territorios de hasta **2.500 x 2.500** para los Titanes.
  La sección "Próximamente" de `store.html` dice **64x64** para el Titán Cronos.
  Hay que fijar un número y que sea el mismo en los tres lados.
- El anuncio promete `/pv 10` para Titanes. Con las bóvedas por modalidad, la cantidad se
  controla con el permiso `odysseia.bovedas.<n>`; hay que asignarlo por rango.
- La sección "Próximamente" pone **todos** los Titanes al mismo precio ($14.990 CLP), que además
  es **un tercio** de lo que cuesta Zeus. Reemplazar por la escalera de arriba.

---

## 4. Los 23 IDs reales, para referencia

7510343 Hércules · 7510348 Hestia · 7510349 Hermes · 7510356 Hefesto · 7510357 Artemisa ·
7510358 Afrodita · 7510359 Zeus · 7510361 Rol Minero · 7510363 Rol Cazador ·
7510364 Rol Constructor · 7510365 Rol Leñador · 7510366 Rol Alquimista · 7510367 Rol Nómada ·
7510368 Kit Hermes · 7510369 Kit Zeus · 7510370 Protección 177x177 · 7596916 Coloso Nether ·
7596920 Coloso End · 7596924 Dominio de Atlas · 7510372 Economía Premium ·
7510373 Saco de Dragmas · 7510374 Cofre de Dragmas · 7510375 Ánfora de Dragmas ·
7545828 Pase SFMaster 1h · 7545831 Pase SFMaster 24h

`protection_481` (7510371) **ya no se vende en Tebex** y en la web está marcado "En revisión"
por la diferencia entre el tamaño vendido y la protección real. Sigue en el catálogo de Odysseia
para poder honrar compras antiguas.
