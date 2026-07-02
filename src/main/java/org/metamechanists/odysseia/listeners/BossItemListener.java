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

        // 2. Projectile hit (custom tridents)
        if (damagerEntity instanceof Trident trident) {
            if (trident.getPersistentDataContainer().has(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING)) {
                String type = trident.getPersistentDataContainer().get(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING);
                if ("odin_spear".equals(type)) {
                    Location loc = target.getLocation();
                    loc.getWorld().strikeLightning(loc);
                    loc.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                } else if ("poseidon_trident".equals(type)) {
                    triggerTsunami(target.getLocation(), trident.getShooter() instanceof Entity s ? s : null);
                }
            }
        }

        // 3. Escudo Espartano de Ares — refleja el 20% del daño bloqueado al atacante
        if (targetEntity instanceof Player blocker && blocker.isBlocking() && isHoldingItem(blocker, "ares_shield")) {
            if (damagerEntity instanceof LivingEntity attacker && !attacker.equals(blocker)) {
                double reflected = event.getDamage() * 0.20;
                if (reflected > 0) {
                    attacker.damage(reflected, blocker);
                    Location aLoc = attacker.getLocation();
                    aLoc.getWorld().spawnParticle(Particle.CRIT, aLoc.clone().add(0, 1, 0), 12, 0.3, 0.5, 0.3, 0.1);
                    aLoc.getWorld().playSound(aLoc, Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.7f);
                }
            }
        }
    }

    /** Ola de Poseidón: empuja a todos los enemigos cercanos al punto de impacto. */
    private void triggerTsunami(Location center, Entity source) {
        center.getWorld().spawnParticle(Particle.SPLASH, center, 80, 3.0, 1.0, 3.0, 0.2);
        center.getWorld().spawnParticle(Particle.BUBBLE, center, 60, 3.0, 1.0, 3.0, 0.1);
        center.getWorld().playSound(center, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1.2f, 0.6f);
        for (Entity e : center.getWorld().getNearbyEntities(center, 6, 4, 6)) {
            if (e instanceof LivingEntity victim && !e.equals(source)) {
                Vector push = victim.getLocation().toVector().subtract(center.toVector());
                if (push.lengthSquared() < 0.01) {
                    push = new Vector(0, 1, 0);
                }
                push.normalize().multiply(1.5).setY(0.8);
                victim.setVelocity(push);
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, true));
            }
        }
    }

    /** Comprueba si el jugador sostiene (en cualquier mano) un item custom con el typeId dado. */
    private boolean isHoldingItem(Player player, String typeId) {
        return matchesType(player.getInventory().getItemInMainHand(), typeId)
                || matchesType(player.getInventory().getItemInOffHand(), typeId);
    }

    private boolean matchesType(ItemStack item, String typeId) {
        if (item == null || !item.hasItemMeta()) return false;
        String type = item.getItemMeta().getPersistentDataContainer().get(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING);
        return typeId.equals(type);
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

            case "mjolnir":
                // Furia del Trueno — rayo directo sobre el objetivo
                targetLoc.getWorld().strikeLightning(targetLoc);
                targetLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, targetLoc.clone().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.1);
                targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.1f);
                break;

            case "zeus_mace": {
                // Tormenta Divina — 3 rayos en radio 5 alrededor del objetivo
                for (int i = 0; i < 3; i++) {
                    double ox = (Math.random() - 0.5) * 10;
                    double oz = (Math.random() - 0.5) * 10;
                    targetLoc.getWorld().strikeLightning(targetLoc.clone().add(ox, 0, oz));
                }
                targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f);
                break;
            }

            case "hades_scythe": {
                // Drenaje de Alma — roba 3♥ (6 HP) y los suma a la salud del jugador
                double maxHp = 20.0;
                var maxAttr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
                if (maxAttr != null) {
                    maxHp = maxAttr.getValue();
                }
                player.setHealth(Math.min(maxHp, player.getHealth() + 6.0));
                targetLoc.getWorld().spawnParticle(Particle.SOUL, targetLoc.clone().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.02);
                targetLoc.getWorld().playSound(targetLoc, Sound.PARTICLE_SOUL_ESCAPE, 1.0f, 0.6f);
                break;
            }
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile proj = event.getEntity();
        if (proj instanceof Trident trident && trident.getShooter() instanceof Player player) {
            // Propaga el typeId del tridente custom (mano o secundaria) al proyectil lanzado
            String type = tridentType(player.getInventory().getItemInMainHand());
            if (type == null) {
                type = tridentType(player.getInventory().getItemInOffHand());
            }
            if ("odin_spear".equals(type) || "poseidon_trident".equals(type)) {
                trident.getPersistentDataContainer().set(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING, type);
            }
        }
    }

    private String tridentType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING);
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

        // Tsunami del Tridente de Poseidón al impactar (bloque o entidad)
        if (proj instanceof Trident trident
                && "poseidon_trident".equals(trident.getPersistentDataContainer().get(OdysseyItemManager.ITEM_KEY, PersistentDataType.STRING))) {
            Location loc = event.getHitBlock() != null
                    ? event.getHitBlock().getLocation().add(0.5, 1, 0.5)
                    : trident.getLocation();
            triggerTsunami(loc, trident.getShooter() instanceof Entity s ? s : null);
        }

        if (proj instanceof Snowball ball) {
            if (ball.hasMetadata("loki_magic")) {
                Location loc = ball.getLocation();
                loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 35, 1.0, 1.0, 1.0, 0.1);
                loc.getWorld().spawnParticle(Particle.CRIT, loc, 15, 0.5, 0.5, 0.5, 0.05);
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

    @EventHandler
    public void onEntityDeath(org.bukkit.event.entity.EntityDeathEvent event) {
        // Sed de Sangre del Filo de Ares — cada víctima otorga Fuerza acumulable (hasta V)
        Player killer = event.getEntity().getKiller();
        if (killer == null || !isHoldingItem(killer, "ares_blade")) {
            return;
        }
        int nextAmplifier = 0;
        PotionEffect current = killer.getPotionEffect(PotionEffectType.STRENGTH);
        if (current != null) {
            nextAmplifier = Math.min(current.getAmplifier() + 1, 4); // cap Fuerza V (amplifier 4)
        }
        killer.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 600, nextAmplifier, false, true)); // 30s, refresca
        Location kLoc = killer.getLocation();
        kLoc.getWorld().spawnParticle(Particle.DUST, kLoc.clone().add(0, 1, 0), 15, 0.4, 0.6, 0.4,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(180, 0, 0), 1.5f));
        kLoc.getWorld().playSound(kLoc, Sound.ENTITY_WITHER_HURT, 0.5f, 1.4f);
    }
}
