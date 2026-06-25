package org.metamechanists.odysseia.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.items.OdysseyItemManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BossItemListener implements Listener {

    private final Odysseia plugin;
    private final Map<UUID, Long> scepterCooldowns = new HashMap<>();

    public BossItemListener(Odysseia plugin) {
        this.plugin = plugin;

        // Periodic check for Odin's Helmet every 2 seconds (40 ticks)
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkOdinHelmet, 40L, 40L);
    }

    private void checkOdinHelmet() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemStack helmet = p.getInventory().getHelmet();
            if (helmet != null && helmet.hasItemMeta()) {
                ItemMeta meta = helmet.getItemMeta();
                String type = meta.getPersistentDataContainer().get(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING);
                if ("odin_helmet".equals(type)) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 100, 0, true, false, false));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1, true, false, false));
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damagerEntity = event.getDamager();
        Entity targetEntity = event.getEntity();

        if (!(targetEntity instanceof LivingEntity target)) return;

        // 1. Direct melee hit by player
        if (damagerEntity instanceof Player player) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                String type = meta.getPersistentDataContainer().get(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING);
                if (type != null) {
                    handleCustomMeleeHit(player, target, type);
                }
            }
        }

        // 2. Projectile hit (Odin's Spear Trident)
        if (damagerEntity instanceof Trident trident) {
            if (trident.getPersistentDataContainer().has(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING)) {
                String type = trident.getPersistentDataContainer().get(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING);
                if ("odin_spear".equals(type)) {
                    Location loc = target.getLocation();
                    loc.getWorld().strikeLightning(loc);
                    loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                }
            }
        }
    }

    private void handleCustomMeleeHit(Player player, LivingEntity target, String itemType) {
        Location targetLoc = target.getLocation();

        switch (itemType) {
            case "loki_dagger":
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 2, false, true));
                targetLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, targetLoc.add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
                targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_WITCH_CELEBRATE, 0.8f, 1.2f);
                break;

            case "kratos_blade":
                target.setFireTicks(100);
                // Pull target toward player
                Vector pull = player.getLocation().toVector().subtract(targetLoc.toVector());
                if (pull.lengthSquared() > 0.01) {
                    pull.normalize().multiply(0.8).setY(0.3);
                    target.setVelocity(pull);
                }
                targetLoc.getWorld().spawnParticle(Particle.FLAME, targetLoc.add(0, 1, 0), 20, 0.4, 0.4, 0.4, 0.1);
                targetLoc.getWorld().playSound(targetLoc, Sound.ITEM_FIRECHARGE_USE, 0.8f, 0.9f);
                break;

            case "leviathan_axe":
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 4, false, true));
                targetLoc.getWorld().spawnParticle(Particle.SNOWFLAKE, targetLoc.add(0, 1, 0), 20, 0.4, 0.4, 0.4, 0.05);
                targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.5f);
                break;

            case "odin_spear":
                targetLoc.getWorld().strikeLightning(targetLoc);
                targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                break;
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile proj = event.getEntity();
        if (proj instanceof Trident trident && trident.getShooter() instanceof Player player) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand != null && hand.hasItemMeta()) {
                ItemMeta meta = hand.getItemMeta();
                String type = meta.getPersistentDataContainer().get(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING);
                if ("odin_spear".equals(type)) {
                    trident.getPersistentDataContainer().set(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING, "odin_spear");
                }
            } else {
                ItemStack offHand = player.getInventory().getItemInOffHand();
                if (offHand != null && offHand.hasItemMeta()) {
                    ItemMeta meta = offHand.getItemMeta();
                    String type = meta.getPersistentDataContainer().get(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING);
                    if ("odin_spear".equals(type)) {
                        trident.getPersistentDataContainer().set(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING, "odin_spear");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta()) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemMeta meta = item.getItemMeta();
        String type = meta.getPersistentDataContainer().get(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING);

        if ("loki_scepter".equals(type)) {
            event.setCancelled(true);
            long now = System.currentTimeMillis();
            long lastUse = scepterCooldowns.getOrDefault(player.getUniqueId(), 0L);
            long diff = now - lastUse;

            if (diff < 5000) {
                double remaining = (5000 - diff) / 1000.0;
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&c&l[COOLDOWN] &eEl Cetro de Loki está recargando. Espera &c" + String.format("%.1f", remaining) + "s&e."));
                return;
            }

            scepterCooldowns.put(player.getUniqueId(), now);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.0f);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, 0.8f, 1.1f);

            Snowball ball = player.launchProjectile(Snowball.class);
            ball.setMetadata("loki_magic", new FixedMetadataValue(plugin, true));

            // Green particle trail task
            Bukkit.getScheduler().runTaskTimer(plugin, task -> {
                if (ball.isDead() || !ball.isValid()) {
                    task.cancel();
                    return;
                }
                ball.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, ball.getLocation(), 3, 0.1, 0.1, 0.1, 0.02);
            }, 1L, 1L);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile proj = event.getEntity();
        if (proj instanceof Snowball ball) {
            if (ball.hasMetadata("loki_magic")) {
                Location loc = ball.getLocation();
                loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 35, 1.0, 1.0, 1.0, 0.1);
                loc.getWorld().spawnParticle(Particle.INSTANT_EFFECT, loc, 15, 0.5, 0.5, 0.5, 0.05);
                loc.getWorld().playSound(loc, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.2f);

                if (event.getHitEntity() instanceof LivingEntity victim) {
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 2, false, true));
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1, false, true));
                    victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_BAT_DEATH, 0.8f, 0.5f);
                }
            } else if (ball.hasMetadata("kratos_axe")) {
                Location loc = ball.getLocation();
                loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 30, 0.5, 0.5, 0.5, 0.1);
                loc.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);

                if (event.getHitEntity() instanceof LivingEntity victim) {
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 5, false, true));
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, true));
                    if (ball.getShooter() instanceof org.bukkit.entity.Entity shooter) {
                        victim.damage(10.0, shooter);
                    } else {
                        victim.damage(10.0);
                    }
                }
            }
        }
    }
}
