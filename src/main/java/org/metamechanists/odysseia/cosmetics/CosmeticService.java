package org.metamechanists.odysseia.cosmetics;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Servicio de Cosméticos Visuales (Auras, Rastros y Efectos de Muerte) para Rangos VIP y Titanes. */
public final class CosmeticService implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, String> activeAuras = new HashMap<>();
    private final Map<UUID, String> activeTrails = new HashMap<>();
    private final Map<UUID, String> activeDeathEffects = new HashMap<>();

    public CosmeticService(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Bucle síncrono para renderizado de auras (10 ticks)
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickAuras, 20L, 10L);
    }

    public void setAura(Player player, String aura) {
        if (aura == null || aura.isEmpty() || aura.equalsIgnoreCase("none")) {
            activeAuras.remove(player.getUniqueId());
            player.sendMessage("§c[Cosméticos] Aura desactivada.");
        } else {
            activeAuras.put(player.getUniqueId(), aura.toLowerCase());
            player.sendMessage("§a[Cosméticos] Aura §e" + aura + " §aactivada.");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7F, 1.5F);
        }
    }

    public void setTrail(Player player, String trail) {
        if (trail == null || trail.isEmpty() || trail.equalsIgnoreCase("none")) {
            activeTrails.remove(player.getUniqueId());
            player.sendMessage("§c[Cosméticos] Rastro desactivado.");
        } else {
            activeTrails.put(player.getUniqueId(), trail.toLowerCase());
            player.sendMessage("§a[Cosméticos] Rastro §e" + trail + " §aactivado.");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7F, 1.5F);
        }
    }

    public void setDeathEffect(Player player, String effect) {
        if (effect == null || effect.isEmpty() || effect.equalsIgnoreCase("none")) {
            activeDeathEffects.remove(player.getUniqueId());
            player.sendMessage("§c[Cosméticos] Efecto de muerte desactivado.");
        } else {
            activeDeathEffects.put(player.getUniqueId(), effect.toLowerCase());
            player.sendMessage("§a[Cosméticos] Efecto de muerte §e" + effect + " §aactivado.");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7F, 1.5F);
        }
    }

    private void tickAuras() {
        for (Map.Entry<UUID, String> entry : activeAuras.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || !p.isOnline()) continue;

            Location loc = p.getLocation().add(0, 1.0, 0);
            switch (entry.getValue()) {
                case "flame" -> loc.getWorld().spawnParticle(Particle.FLAME, loc, 5, 0.3, 0.5, 0.3, 0.02);
                case "lightning" -> loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 8, 0.4, 0.6, 0.4, 0.05);
                case "soul" -> loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 6, 0.3, 0.5, 0.3, 0.02);
                case "water" -> loc.getWorld().spawnParticle(Particle.DRIPPING_WATER, loc, 8, 0.4, 0.6, 0.4, 0.01);
                case "titan" -> loc.getWorld().spawnParticle(Particle.END_ROD, loc, 10, 0.5, 0.8, 0.5, 0.03);
                case "caos" -> {
                    loc.getWorld().spawnParticle(Particle.DUST, loc, 10, 0.5, 0.8, 0.5, new Particle.DustOptions(Color.fromRGB(255, 0, 85), 1.5F));
                    loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc, 6, 0.3, 0.5, 0.3, 0.05);
                }
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        Player p = event.getPlayer();
        String trail = activeTrails.get(p.getUniqueId());
        if (trail == null) return;

        Location loc = p.getLocation().add(0, 0.1, 0);
        switch (trail) {
            case "sparkle" -> loc.getWorld().spawnParticle(Particle.CRIT, loc, 3, 0.2, 0.1, 0.2, 0.01);
            case "heart" -> loc.getWorld().spawnParticle(Particle.HEART, loc, 1, 0.2, 0.2, 0.2, 0.01);
            // DRAGON_BREATH exige dato Float en Paper 1.21.11; va por la capa de compatibilidad.
            case "dragon" -> org.metamechanists.odysseia.util.ParticleCompat.spawnDragonBreath(
                    loc.getWorld(), loc, 4, 0.2, 0.1, 0.2, 0.01, 0.01f);
            case "portal" -> loc.getWorld().spawnParticle(Particle.PORTAL, loc, 6, 0.2, 0.1, 0.2, 0.05);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        String effect = activeDeathEffects.get(p.getUniqueId());
        if (effect == null) return;

        Location loc = p.getLocation();
        switch (effect) {
            case "lightning" -> loc.getWorld().strikeLightningEffect(loc);
            case "totem" -> loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.add(0, 1, 0), 50, 0.5, 0.8, 0.5, 0.2);
            case "explosion" -> {
                loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8F, 1.0F);
            }
        }
    }
}
