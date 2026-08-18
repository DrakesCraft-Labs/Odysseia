# 👑 Configuración Integral del Rango Oldschool (Veteranos 5+ Años)

Documento maestro de configuración, permisos de LuckPerms, integración con Essentials, Hikari (ChestShop), S-PlayerWarps y el core **Odysseia**.

---

## 💎 1. Kit Mensual Legendario (`/kit oldschool`)

* **Cooldown**: `30d` (2.592.000 segundos).
* **Economía**: `$100.000` entregados automáticamente a la cuenta Vault del jugador.
* **ProtectionStone**: `1x ProtectionStone VIP` (`viphercules` / radio extendido).
* **Armadura Netherite Mítica (Full Set Soulbound)**:
  * **Casco del Fundador Oldschool**: Protección X, Irrompibilidad VI, Respiración III, Afinidad Acuática I, Reparación I.
  * **Pechera del Fundador Oldschool**: Protección X, Irrompibilidad VI, Espinas III, Reparación I.
  * **Grebas del Fundador Oldschool**: Protección X, Irrompibilidad VI, Reparación I.
  * **Botas Aladas Oldschool**: Protección X, Irrompibilidad VI, Caída de Pluma VIII, Agilidad Acuática III, Reparación I.
* **Armas & Herramientas**:
  * **Espada Reliquia Oldschool**: Filo XI, Botín V, Aspecto Ígneo II, Barrido III, Irrompibilidad VI, Reparación I, Soulbound.
  * **Pico del Fundador Oldschool**: Eficiencia X, Fortuna VI, Irrompibilidad VI, Reparación I.
  * **Hacha Ancestral Oldschool**: Eficiencia X, Filo VIII, Irrompibilidad VI, Reparación I.
  * **Escudo Legendario Oldschool**: Irrompibilidad VI, Reparación I.
* **Consumibles**:
  * `16x` Manzanas Doradas (`GOLDEN_APPLE`)
  * `2x` Tótems de la Inmortalidad (`TOTEM_OF_UNDYING`)
  * `64x` Zanahorias Doradas (`GOLDEN_CARROT`)
  * `16x` Ender Pearls (`ENDER_PEARL`)

---

## ⚡ 2. Efectos Pasivos al Vestir el Set Completo (Aura Odysseia)

Gestionado en tiempo real por `ArmorEffectsListener.java` al detectar `drakes.kit.oldschool` y set completo equipado:
* 🏃 **Velocidad III** (`speed: 3`)
* ❤️ **+4 Corazones Extra de Vida** (`health-boost: 2`)
* 🔥 **Resistencia al Fuego Total** (`fire-resistance: true`)
* 🛡️ **Resistencia I** (`resistance: 1`)
* 🍗 **Saturación Permanente** (`saturation: true`)

---

## 🔐 3. Comandos de LuckPerms para Configurar el Grupo `oldschool`

Ejecutar en la consola del servidor o en un bloque de comandos:

```bash
# 1. Crear el grupo y configurar prefijo
lp creategroup oldschool
lp group oldschool setweight 70
lp group oldschool meta setprefix 70 "&8[&7&lOLDSCHOOL&8] &7"

# 2. Herencia de comandos básicos
lp group oldschool parent add default

# 3. Permiso del Kit y Pasivas de Odysseia
lp group oldschool permission set drakes.kit.oldschool true

# 4. Comandos Utilitarios (Essentials)
lp group oldschool permission set essentials.enderchest true
lp group oldschool permission set essentials.workbench true
lp group oldschool permission set essentials.hat true
lp group oldschool permission set essentials.feed true
lp group oldschool permission set essentials.condense true
lp group oldschool permission set essentials.near true
lp group oldschool permission set essentials.ptime true
lp group oldschool permission set essentials.pweather true

# 5. Límite de 15 Casas (/sethome)
lp group oldschool permission set essentials.sethome.multiple.oldschool true
lp group oldschool permission set essentials.sethome.multiple.15 true

# 6. Límite de 15 Tiendas de Cofres (Hikari / ChestShop)
lp group oldschool permission set chestshop.shop.create true
lp group oldschool permission set chestshop.nofee true
lp group oldschool permission set hikari.shop.limit.15 true
lp group oldschool permission set chestshop.shop.limit.15 true

# 7. Límite de 5 PlayerWarps Públicos (S-PlayerWarps)
lp group oldschool permission set pw.limit.5 true
lp group oldschool permission set playerwarps.limit.5 true
lp group oldschool permission set playerwarps.set true
```

---

## 👑 4. Comando de Entrega Rápida por Staff

Para otorgar todo en un solo paso:
```bash
/esteusuarioesviejo <jugador>
# Alias: /grantoldschool <jugador>, /viejodesde <jugador>
```
Este comando asigna el grupo `oldschool`, el permiso `drakes.kit.oldschool` y entrega de inmediato el kit al jugador (o lo encola si está desconectado).
