package org.metamechanists.odysseia.cosmetics;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Cosmeticos visuales (auras, rastros y efectos de muerte) para rangos VIP y staff.
 *
 * Dos cosas que faltaban y hacian que no se sintieran una compra:
 *   - la seleccion vivia solo en memoria, asi que cada reinicio dejaba a todos sin cosmetico;
 *   - no se revalidaba el permiso, asi que un rango vencido seguia luciendo su aura.
 */
public final class CosmeticService implements Listener {

    /** Permiso base: sin el, el comando no esta disponible. Se da a VIP y staff. */
    public static final String USE = "drakes.cosmetics.use";

    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Map<UUID, String> activeAuras = new HashMap<>();
    private final Map<UUID, String> activeTrails = new HashMap<>();
    private final Map<UUID, String> activeDeathEffects = new HashMap<>();
    /** Reloj de las formas animadas: alas que baten, orbitas que giran, colas que ondulan. */
    private long fase;

    /** Dorado y morado, los colores de la casa. */
    private static final Color DORADO = Color.fromRGB(255, 196, 0);
    private static final Color MORADO = Color.fromRGB(163, 53, 238);
    private static final Color BLANCO_CALIDO = Color.fromRGB(255, 245, 200);
    private static final Color ABISAL = Color.fromRGB(0, 180, 220);
    private static final Color CARMESI = Color.fromRGB(255, 40, 60);
    private static final Color HIELO = Color.fromRGB(170, 235, 255);

    /**
     * Puntos que dibuja cada ala.
     *
     * Es multiplo de las plumas por fila, de modo que todas las filas salen completas y el borde
     * inferior del ala no queda escalonado.
     */
    private static final int PUNTOS_ALA = 40;

    public CosmeticService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "cosmetics.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        // Cada 4 ticks (5 veces/s) en vez de cada 10: un ala que bate a 2 fps se ve a saltos.
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickAuras, 20L, 4L);
    }

    public void setAura(Player player, String aura) {
        apply(player, "aura", aura, activeAuras, "Aura");
    }

    public void setTrail(Player player, String trail) {
        apply(player, "trail", trail, activeTrails, "Rastro");
    }

    public void setDeathEffect(Player player, String effect) {
        apply(player, "death", effect, activeDeathEffects, "Efecto de muerte");
    }

    private void apply(Player player, String tipo, String valor, Map<UUID, String> target, String etiqueta) {
        UUID uuid = player.getUniqueId();
        if (valor == null || valor.isEmpty() || valor.equalsIgnoreCase("none")) {
            target.remove(uuid);
            data.set(uuid + "." + tipo, null);
            save();
            player.sendMessage("§c[Cosméticos] " + etiqueta + " desactivado.");
            return;
        }
        target.put(uuid, valor.toLowerCase());
        data.set(uuid + "." + tipo, valor.toLowerCase());
        save();
        player.sendMessage("§a[Cosméticos] " + etiqueta + " §e" + valor + " §aactivado.");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7F, 1.5F);
    }

    /** Restaura lo guardado, descartando lo que el jugador ya no tenga permiso de usar. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        restore(player, "aura", activeAuras);
        restore(player, "trail", activeTrails);
        restore(player, "death", activeDeathEffects);
        if (!player.hasPermission(USE)) {
            activeAuras.remove(uuid);
            activeTrails.remove(uuid);
            activeDeathEffects.remove(uuid);
        }
    }

    private void restore(Player player, String tipo, Map<UUID, String> target) {
        String saved = data.getString(player.getUniqueId() + "." + tipo);
        if (saved == null) return;
        if (player.hasPermission("drakes.cosmetics." + tipo + "." + saved)) {
            target.put(player.getUniqueId(), saved);
        } else {
            // El rango vencio: se apaga solo en vez de quedar luciendo algo que ya no compro.
            data.set(player.getUniqueId() + "." + tipo, null);
            save();
        }
    }

    /** Sin esto los mapas crecen con cada jugador que pasa por el servidor. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        activeAuras.remove(uuid);
        activeTrails.remove(uuid);
        activeDeathEffects.remove(uuid);
    }

    private void tickAuras() {
        fase++;

        for (Map.Entry<UUID, String> entry : Map.copyOf(activeAuras).entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || !p.isOnline()) continue;
            if (!p.hasPermission(USE)) {
                activeAuras.remove(entry.getKey());
                continue;
            }

            Location loc = p.getLocation().add(0, 1.0, 0);
            var world = loc.getWorld();
            switch (entry.getValue()) {
                case "flame" -> world.spawnParticle(Particle.FLAME, loc, 5, 0.3, 0.5, 0.3, 0.02);
                case "sparkle" -> world.spawnParticle(Particle.END_ROD, loc, 4, 0.3, 0.5, 0.3, 0.01);
                case "water" -> world.spawnParticle(Particle.DRIPPING_WATER, loc, 8, 0.4, 0.6, 0.4, 0.01);
                case "ember" -> world.spawnParticle(Particle.LAVA, loc, 2, 0.3, 0.4, 0.3, 0.01);
                case "forest" -> world.spawnParticle(Particle.COMPOSTER, loc, 6, 0.4, 0.5, 0.4, 0.02);
                case "heart" -> world.spawnParticle(Particle.HEART, loc, 2, 0.4, 0.5, 0.4, 0.01);
                case "lightning" -> {
                    dibujar(p, CosmeticShapes.vortice(fase, 3, 5, 0.82D, 2.15D),
                            BLANCO_CALIDO, 0.62F);
                    world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 6, 0.4, 0.7, 0.4, 0.06);
                }
                case "soul" -> world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 6, 0.3, 0.5, 0.3, 0.02);
                case "sand" -> world.spawnParticle(Particle.FALLING_DUST, loc,
                        8, 0.4, 0.6, 0.4, org.bukkit.Material.SAND.createBlockData());
                case "abyss" -> world.spawnParticle(Particle.BUBBLE_POP, loc, 8, 0.4, 0.6, 0.4, 0.02);
                case "titan" -> world.spawnParticle(Particle.END_ROD, loc, 10, 0.5, 0.8, 0.5, 0.03);
                case "solar" -> {
                    dibujar(p, CosmeticShapes.anilloInclinado(fase, 20, 0.82D, 1.25D, 0.22D),
                            DORADO, 1.05F);
                    dibujar(p, CosmeticShapes.orbita(-fase * 1.4D, 4, 1.02D), BLANCO_CALIDO, 0.65F);
                    world.spawnParticle(Particle.SMALL_FLAME, loc, 3, 0.25, 0.55, 0.25, 0.01);
                }
                case "void" -> {
                    dibujar(p, CosmeticShapes.espiral(-fase * 1.3D, 18, 0.75D, 2.15D),
                            Color.fromRGB(55, 15, 90), 0.9F);
                    world.spawnParticle(Particle.REVERSE_PORTAL, loc, 7, 0.35, 0.65, 0.35, 0.08);
                }
                case "singularidad" -> {
                    // Espiral de particulas, sin entidades Display ni coste de pathfinding.
                    dibujar(p, CosmeticShapes.espiral(fase * 1.45D, 20, 0.9D, 2.35D), MORADO, 1.05F);
                    dibujar(p, CosmeticShapes.orbita(-fase * 2.1D, 4, 1.15D),
                            Color.fromRGB(18, 8, 42), 1.25F);
                }
                case "caos" -> {
                    dibujar(p, CosmeticShapes.anilloInclinado(fase * 2, 18, 1.0D, 1.05D, 0.55D),
                            Color.fromRGB(255, 0, 85), 1.05F);
                    dibujar(p, CosmeticShapes.anilloInclinado(-fase * 1.4D, 14, 0.7D, 1.35D, -0.4D),
                            MORADO, 0.75F);
                    world.spawnParticle(Particle.REVERSE_PORTAL, loc, 4, 0.3, 0.5, 0.3, 0.05);
                }
                case "staff" -> {
                    dibujar(p, CosmeticShapes.corona(fase, 8, 0.4D, 2.3D), ABISAL, 0.75F);
                    dibujar(p, CosmeticShapes.orbita(fase * 1.5D, 4, 0.9D), BLANCO_CALIDO, 0.62F);
                }
                // ── Cosmeticos con forma ──────────────────────────────
                // PUNTOS_ALA reparte la membrana; con menos se veia un trazo y no un ala.
                case "alas" -> dibujarAlas(p, DORADO, BLANCO_CALIDO);
                case "alas_moradas" -> dibujarAlas(p, MORADO, Color.fromRGB(225, 170, 255));
                case "alas_abisales" -> {
                    dibujarAlas(p, ABISAL, HIELO);
                    world.spawnParticle(Particle.DRIPPING_WATER, loc, 3, 0.6, 0.5, 0.4, 0.0);
                }
                case "alas_infernales" -> {
                    dibujarAlas(p, CARMESI, Color.fromRGB(255, 155, 35));
                    world.spawnParticle(Particle.SMALL_FLAME, loc, 4, 0.6, 0.5, 0.4, 0.01);
                }
                case "alas_glaciares" -> {
                    dibujarAlas(p, HIELO, Color.WHITE);
                    world.spawnParticle(Particle.SNOWFLAKE, loc, 3, 0.6, 0.5, 0.4, 0.01);
                }
                case "alas_solares" -> {
                    dibujarAlas(p, DORADO, Color.WHITE);
                    dibujar(p, CosmeticShapes.huesosAlas(yaw(p), fase + 4, 8),
                            Color.fromRGB(255, 110, 20), 0.7F);
                }
                case "corona_dorada" -> dibujar(p, CosmeticShapes.corona(fase, 8, 0.36D, 2.25D), DORADO, 0.75F);
                case "corona_abisal" -> dibujar(p, CosmeticShapes.corona(fase, 8, 0.36D, 2.25D), ABISAL, 0.75F);
                case "saturno" -> {
                    dibujar(p, CosmeticShapes.anilloInclinado(fase, 26, 1.25D, 1.15D, 0.45D), DORADO, 0.8F);
                    dibujar(p, CosmeticShapes.anilloInclinado(fase, 14, 0.95D, 1.15D, 0.34D), BLANCO_CALIDO, 0.55F);
                }
                case "voragine" -> {
                    dibujar(p, CosmeticShapes.vortice(fase, 4, 7, 1.2D, 1.9D), ABISAL, 0.85F);
                    world.spawnParticle(Particle.BUBBLE_POP, p.getLocation(), 4, 0.6, 0.1, 0.6, 0.0);
                }
                case "tempestad" -> {
                    dibujar(p, CosmeticShapes.vortice(fase, 3, 6, 1.1D, 2.1D), BLANCO_CALIDO, 0.8F);
                    world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 6, 0.5, 0.7, 0.5, 0.06);
                }
                case "constelacion" -> {
                    // Anillo lento arriba y luciernagas debajo: se lee como un cielo propio.
                    dibujar(p, CosmeticShapes.anilloInclinado(-fase, 18, 1.05D, 1.95D, 0.3D), BLANCO_CALIDO, 0.7F);
                    dibujar(p, CosmeticShapes.orbita(fase, 5, 0.8D), DORADO, 0.6F);
                }
                case "halo" -> dibujar(p, CosmeticShapes.halo(0.45D, 2.35D, 14), DORADO, 0.9F);
                case "halo_morado" -> dibujar(p, CosmeticShapes.halo(0.45D, 2.35D, 14), MORADO, 0.9F);
                case "cola" -> dibujar(p, CosmeticShapes.cola(yaw(p), fase, 10), MORADO, 0.9F);
                case "orbita" -> dibujar(p, CosmeticShapes.orbita(fase, 5, 0.9D), BLANCO_CALIDO, 0.8F);
                case "orbita_dorada" -> dibujar(p, CosmeticShapes.orbita(fase, 5, 0.9D), DORADO, 0.8F);
                case "star" -> {
                    // La marca de la casa: espiral morada que asciende con destellos dorados.
                    dibujar(p, CosmeticShapes.espiral(fase, 18, 0.8D, 2.2D), MORADO, 1.0F);
                    dibujar(p, CosmeticShapes.orbita(fase * 1.4D, 3, 1.1D), DORADO, 0.7F);
                }
                case "papa" -> {
                    // Halo dorado y luciernagas: se reconoce de lejos, que es todo el punto.
                    dibujar(p, CosmeticShapes.halo(0.5D, 2.4D, 16), DORADO, 1.1F);
                    dibujar(p, CosmeticShapes.orbita(fase, 6, 1.0D), DORADO, 0.8F);
                }
                default -> { /* cosmetico retirado del catalogo: no se dibuja nada */ }
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        Player p = event.getPlayer();
        String trail = activeTrails.get(p.getUniqueId());
        if (trail == null) return;
        if (!p.hasPermission(USE)) {
            activeTrails.remove(p.getUniqueId());
            return;
        }

        Location loc = p.getLocation().add(0, 0.1, 0);
        var world = loc.getWorld();
        switch (trail) {
            case "sparkle" -> world.spawnParticle(Particle.CRIT, loc, 3, 0.2, 0.1, 0.2, 0.01);
            case "heart" -> world.spawnParticle(Particle.HEART, loc, 1, 0.2, 0.2, 0.2, 0.01);
            case "note" -> world.spawnParticle(Particle.NOTE, loc, 1, 0.2, 0.2, 0.2, 1.0);
            case "lava" -> world.spawnParticle(Particle.LAVA, loc, 1, 0.2, 0.1, 0.2, 0.01);
            case "leaf" -> world.spawnParticle(Particle.COMPOSTER, loc, 3, 0.2, 0.1, 0.2, 0.01);
            case "cloud" -> world.spawnParticle(Particle.CLOUD, loc, 3, 0.2, 0.1, 0.2, 0.01);
            case "portal" -> world.spawnParticle(Particle.PORTAL, loc, 6, 0.2, 0.1, 0.2, 0.05);
            // DRAGON_BREATH exige dato Float en Paper 1.21.11; va por la capa de compatibilidad.
            case "dragon" -> org.metamechanists.odysseia.util.ParticleCompat.spawnDragonBreath(
                    world, loc, 4, 0.2, 0.1, 0.2, 0.01, 0.01f);
            case "snow" -> world.spawnParticle(Particle.SNOWFLAKE, loc, 3, 0.2, 0.1, 0.2, 0.01);
            case "bubble" -> world.spawnParticle(Particle.BUBBLE_POP, loc, 4, 0.2, 0.1, 0.2, 0.01);
            case "rune" -> world.spawnParticle(Particle.ENCHANT, loc, 6, 0.2, 0.3, 0.2, 0.5);
            case "staff" -> world.spawnParticle(Particle.DUST, loc, 3, 0.2, 0.1, 0.2,
                    new Particle.DustOptions(Color.fromRGB(0, 200, 255), 1.0F));
            case "star" -> {
                world.spawnParticle(Particle.DUST, loc, 4, 0.2, 0.1, 0.2,
                        new Particle.DustOptions(MORADO, 1.0F));
                world.spawnParticle(Particle.DUST, loc, 2, 0.15, 0.05, 0.15,
                        new Particle.DustOptions(DORADO, 0.7F));
            }
            case "dorado" -> world.spawnParticle(Particle.DUST, loc, 4, 0.2, 0.1, 0.2,
                    new Particle.DustOptions(DORADO, 1.0F));
            case "morado" -> world.spawnParticle(Particle.DUST, loc, 4, 0.2, 0.1, 0.2,
                    new Particle.DustOptions(MORADO, 1.0F));
            case "residuo" -> {
                world.spawnParticle(Particle.DUST, loc, 5, 0.28, 0.12, 0.28,
                        new Particle.DustOptions(MORADO, 1.1F));
                world.spawnParticle(Particle.REVERSE_PORTAL, loc, 2, 0.18, 0.08, 0.18, 0.01);
            }
            case "plumas" -> world.spawnParticle(Particle.END_ROD, loc, 2, 0.15, 0.05, 0.15, 0.005);
            default -> { /* cosmetico retirado del catalogo */ }
        }
    }

    /** Hacia donde mira el jugador, en radianes, para orientar alas y colas. */
    private static double yaw(Player player) {
        // Bukkit: yaw 0 mira +Z y yaw 90 mira -X. La rotación local necesita el mismo signo;
        // negarlo reflejaba la espalda y llevaba alas/cola al frente al mirar este u oeste.
        return Math.toRadians(player.getLocation().getYaw());
    }

    /** Superficie coloreada y borde claro: da volumen y conserva la silueta en cualquier fondo. */
    private void dibujarAlas(Player player, Color membrana, Color borde) {
        double orientacion = yaw(player);
        dibujar(player, CosmeticShapes.alas(orientacion, fase, PUNTOS_ALA), membrana, 1.0F);
        dibujar(player, CosmeticShapes.huesosAlas(orientacion, fase, 10), borde, 0.72F);
    }

    /**
     * Pinta una forma alrededor del jugador.
     *
     * Se usa DUST con conteo 0 y velocidad 0 para que cada particula caiga **exactamente** donde
     * dice la geometria. Con conteo mayor Minecraft las dispersa al azar y la forma se deshace.
     */
    private void dibujar(Player player, java.util.List<org.bukkit.util.Vector> puntos,
                         Color color, float tamano) {
        Location base = player.getLocation();
        var world = base.getWorld();
        if (world == null) return;
        var opciones = new Particle.DustOptions(color, tamano);
        for (var punto : puntos) {
            world.spawnParticle(Particle.DUST,
                    base.getX() + punto.getX(), base.getY() + punto.getY(), base.getZ() + punto.getZ(),
                    1, 0, 0, 0, 0, opciones);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        String effect = activeDeathEffects.get(p.getUniqueId());
        if (effect == null || !p.hasPermission(USE)) return;

        Location loc = p.getLocation();
        var world = loc.getWorld();
        switch (effect) {
            case "smoke" -> world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, loc.clone().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.05);
            case "lightning" -> world.strikeLightningEffect(loc);
            case "totem" -> world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0, 1, 0), 50, 0.5, 0.8, 0.5, 0.2);
            case "souls" -> world.spawnParticle(Particle.SOUL, loc.clone().add(0, 1, 0), 40, 0.5, 0.8, 0.5, 0.08);
            case "implosion" -> {
                world.spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0, 1, 0), 80, 0.1, 0.1, 0.1, 1.2);
                world.playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 0.8F, 0.6F);
            }
            case "explosion" -> {
                world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
                world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8F, 1.0F);
            }
            case "staff" -> {
                // FLASH exige un Color en 1.21.11; va por la capa de compatibilidad.
                org.metamechanists.odysseia.util.ParticleCompat.spawnFlash(world, loc.clone().add(0, 1, 0), 3);
                world.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.2F);
            }
            default -> { /* cosmetico retirado del catalogo */ }
        }
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "[ERROR] No se pudo guardar cosmetics.yml", exception);
        }
    }
}
