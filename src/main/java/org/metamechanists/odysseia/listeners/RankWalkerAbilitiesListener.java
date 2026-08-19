package org.metamechanists.odysseia.listeners;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.Odysseia;

import java.util.*;

/**
 * Habilidades divinas de desplazamiento (Caminar en Lava, Caminar en Agua y Pasos Elementales)
 * Activas únicamente cuando el jugador tiene equipada su armadura de rango.
 */
public final class RankWalkerAbilitiesListener implements Listener {

    private final Odysseia plugin;
    private final Map<UUID, Long> lastSoundTime = new HashMap<>();

    public RankWalkerAbilitiesListener(Odysseia plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) return;

        boolean hasHestia = player.hasPermission("drakes.kit.hestia") && isWearingFullArmor(player);
        boolean hasHefesto = player.hasPermission("drakes.kit.hefesto") && isWearingFullArmor(player);
        boolean hasHermes = player.hasPermission("drakes.kit.hermes") && isWearingFullArmor(player);
        boolean hasZeus = player.hasPermission("drakes.kit.zeus") && isWearingFullArmor(player);
        boolean hasOceanus = (player.hasPermission("drakes.kit.oceanus") || player.hasPermission("drakes.kit.poseidon") || player.hasPermission("drakes.kit.afrodita")) && isWearingFullArmor(player);

        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        Block under = loc.getBlock().getRelative(BlockFace.DOWN);
        Block current = loc.getBlock();

        // 1. CAMINAR EN LAVA (Hestia y Hefesto)
        if (hasHestia || hasHefesto) {
            if (under.getType() == Material.LAVA || current.getType() == Material.LAVA) {
                // Impulso y sustentación sobre la superficie de lava
                if (player.getVelocity().getY() < 0 && !player.isSneaking()) {
                    Vector v = player.getVelocity();
                    v.setY(0.08);
                    player.setVelocity(v);
                }

                // Inmunidad y efectos de fuego sagrado
                player.setFireTicks(0);
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 40, 0, false, false, false));

                // Partículas espectaculares de fuego sagrado
                world.spawnParticle(Particle.FLAME, loc.clone().add(0, 0.1, 0), 4, 0.25, 0.05, 0.25, 0.02);
                world.spawnParticle(Particle.LAVA, loc.clone().add(0, 0.1, 0), 1, 0.2, 0.05, 0.2, 0);
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 0.1, 0), 2, 0.2, 0.05, 0.2, 0.01);

                playThrottledSound(player, Sound.BLOCK_LAVA_EXTINGUISH, 0.35f, 1.4f);
            }
        }

        // 2. CAMINAR EN AGUA (Oceanus / Poseidón / Afrodita)
        if (hasOceanus) {
            if (under.getType() == Material.WATER || current.getType() == Material.WATER) {
                if (player.getVelocity().getY() < 0 && !player.isSneaking()) {
                    Vector v = player.getVelocity();
                    v.setY(0.08);
                    player.setVelocity(v);
                }

                player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 40, 0, false, false, false));
                world.spawnParticle(Particle.SPLASH, loc.clone().add(0, 0.1, 0), 6, 0.3, 0.1, 0.3, 0.05);
                world.spawnParticle(Particle.BUBBLE_POP, loc.clone().add(0, 0.1, 0), 4, 0.2, 0.1, 0.2, 0.02);

                playThrottledSound(player, Sound.ENTITY_BOAT_PADDLE_WATER, 0.4f, 1.2f);
            }
        }

        // 3. PASO LIGERO DEL VIENTO (Hermes)
        if (hasHermes) {
            world.spawnParticle(Particle.CLOUD, loc.clone().add(0, 0.05, 0), 2, 0.15, 0.05, 0.15, 0.01);
            world.spawnParticle(Particle.SWEEP_ATTACK, loc.clone().add(0, 0.1, 0), 1, 0.1, 0.05, 0.1, 0);
        }

        // 4. PASO DEL TRUENO Y RELÁMPAGO (Zeus)
        if (hasZeus) {
            world.spawnParticle(Particle.ELECTRIC_SPARK, loc.clone().add(0, 0.1, 0), 3, 0.2, 0.1, 0.2, 0.05);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLavaDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.LAVA || event.getCause() == EntityDamageEvent.DamageCause.FIRE 
                || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
            boolean hasHestia = player.hasPermission("drakes.kit.hestia") && isWearingFullArmor(player);
            boolean hasHefesto = player.hasPermission("drakes.kit.hefesto") && isWearingFullArmor(player);
            if (hasHestia || hasHefesto) {
                event.setCancelled(true);
                player.setFireTicks(0);
            }
        }
    }

    private boolean isWearingFullArmor(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack h = inv.getHelmet();
        ItemStack c = inv.getChestplate();
        ItemStack l = inv.getLeggings();
        ItemStack b = inv.getBoots();
        return h != null && !h.getType().isAir()
                && c != null && !c.getType().isAir()
                && l != null && !l.getType().isAir()
                && b != null && !b.getType().isAir();
    }

    private void playThrottledSound(Player player, Sound sound, float volume, float pitch) {
        long now = System.currentTimeMillis();
        long last = lastSoundTime.getOrDefault(player.getUniqueId(), 0L);
        if (now - last > 450L) {
            lastSoundTime.put(player.getUniqueId(), now);
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }
}
