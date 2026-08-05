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

    public CosmeticService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "cosmetics.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickAuras, 20L, 10L);
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
                case "lightning" -> world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 8, 0.4, 0.6, 0.4, 0.05);
                case "soul" -> world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 6, 0.3, 0.5, 0.3, 0.02);
                case "sand" -> world.spawnParticle(Particle.FALLING_DUST, loc,
                        8, 0.4, 0.6, 0.4, org.bukkit.Material.SAND.createBlockData());
                case "abyss" -> world.spawnParticle(Particle.BUBBLE_POP, loc, 8, 0.4, 0.6, 0.4, 0.02);
                case "titan" -> world.spawnParticle(Particle.END_ROD, loc, 10, 0.5, 0.8, 0.5, 0.03);
                case "solar" -> world.spawnParticle(Particle.DUST, loc, 10, 0.5, 0.8, 0.5,
                        new Particle.DustOptions(Color.fromRGB(255, 196, 0), 1.5F));
                case "void" -> world.spawnParticle(Particle.REVERSE_PORTAL, loc, 12, 0.5, 0.8, 0.5, 0.06);
                case "caos" -> {
                    world.spawnParticle(Particle.DUST, loc, 10, 0.5, 0.8, 0.5,
                            new Particle.DustOptions(Color.fromRGB(255, 0, 85), 1.5F));
                    world.spawnParticle(Particle.REVERSE_PORTAL, loc, 6, 0.3, 0.5, 0.3, 0.05);
                }
                case "staff" -> world.spawnParticle(Particle.DUST, loc, 8, 0.4, 0.6, 0.4,
                        new Particle.DustOptions(Color.fromRGB(0, 200, 255), 1.2F));
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
            default -> { /* cosmetico retirado del catalogo */ }
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
                world.spawnParticle(Particle.FLASH, loc.clone().add(0, 1, 0), 3);
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
