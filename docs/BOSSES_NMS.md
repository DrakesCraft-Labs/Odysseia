# Jefes: por qué NO usan NMS (todavía)

## Estado

**Los 22 jefes funcionan sin una sola línea de NMS.** `BossSpectacle`, `BossFaction` y
`BossDisguise` son Bukkit puro más LibsDisguises por reflexión.

Fue una decisión, no una limitación. NMS **sí está disponible**: BuildTools quedó instalado en
WSL el 2026-08-05 y el Spigot remapeado de 1.21.11 vive en el Maven local, que es lo que permitió
compilar el fork de ExcellentEnchants.

```bash
java -jar ~/buildtools/BuildTools.jar --rev 1.21.11 --remapped   # ya ejecutado
# artefacto: ~/.m2/.../spigot-1.21.11-R0.2-SNAPSHOT-remapped-mojang.jar
```

## Por qué se evitó

| Con Bukkit | Con NMS |
|---|---|
| El posicionamiento empuja la entidad | Habría que registrar `Goal` en el cerebro |
| El temblor mueve mínimamente la vista | Paquetes crudos de posición |
| **Sobrevive a las actualizaciones de Paper** | **Un módulo nuevo por versión de MC** |

Con 22 jefes, cada actualización de Minecraft con NMS significa revisar 22 comportamientos.
ExcellentEnchants lo ilustra: tiene `mc_1_21_8`, `mc_1_21_10`, `mc_1_21_11` y `mc_26_1_2`.

Jack se comprometió a quedarse en 1.21.11 "al menos dos años", lo que debilita ese argumento —
pero la versión Bukkit ya entrega el 80% del efecto sin deuda.

## Qué aportaría NMS que hoy no se puede

Si se retoma, esto es lo que **de verdad** requiere NMS y no tiene equivalente en Bukkit:

1. **`Goal` personalizados en el cerebro.** Hoy el posicionamiento es un empujón por tick; con
   NMS sería comportamiento real: rodear, flanquear, cubrirse. Es el salto más grande.
2. **Entidades compuestas.** La Hidra con cabezas que se rompen una a una, cada una con su hitbox
   y su vida. Hoy solo se puede fingir.
3. **Cambios de mundo efímeros.** Poseidón inundando la arena por capas sin tocar bloques reales,
   enviando cambios de sección solo al cliente. Con la API cada bloque es un evento.

## Si se retoma

**Un jefe piloto**, no los 22. Cerbero o Hidra son los que más ganan con comportamiento real.
La IA se prueba peleando, no leyendo código: sin combate real no hay forma de saber si funciona.

El punto de extensión ya existe: `OdysseyBoss.arquetipo()` decide cómo se mueve cada jefe. Una
implementación NMS puede engancharse ahí sin tocar los 22 archivos, igual que se hizo con el
disfraz.
