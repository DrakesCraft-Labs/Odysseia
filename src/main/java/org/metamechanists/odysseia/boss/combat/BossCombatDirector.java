package org.metamechanists.odysseia.boss.combat;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.boss.OdysseyBoss;
import org.metamechanists.odysseia.boss.combat.BossCombatProfile.AttackFamily;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Runs bounded, telegraphed combat mechanics shared by every mythic boss. */
public final class BossCombatDirector {
    private static final Particle.DustOptions AERIAL_DUST = new Particle.DustOptions(Color.fromRGB(92, 182, 255), 1.45F);
    private static final Particle.DustOptions GROUND_DUST = new Particle.DustOptions(Color.fromRGB(255, 118, 42), 1.55F);
    private static final Particle.DustOptions RANGED_DUST = new Particle.DustOptions(Color.fromRGB(174, 74, 255), 1.45F);

    private final Odysseia plugin;
    private final Map<UUID, Long> nextAttackAt = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> rotations = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> pendingAttacks = new ConcurrentHashMap<>();

    public BossCombatDirector(Odysseia plugin) {
        this.plugin = plugin;
    }

    /** Selects one contextual attack while allowing at most one pending impact per boss. */
    public void tick(OdysseyBoss boss) {
        if (!plugin.getConfig().getBoolean("boss-balance.combat-director.enabled", true)) {
            return;
        }
        LivingEntity entity = boss.getEntity();
        UUID bossId = entity.getUniqueId();
        if (!isAlive(entity) || pendingAttacks.containsKey(bossId)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < nextAttackAt.getOrDefault(bossId, 0L)) {
            return;
        }

        double targetRange = Math.clamp(plugin.getConfig().getDouble(
                "boss-balance.combat-director.target-range", 44.0D), 16.0D, 72.0D);
        Player target = boss.nearestCombatTarget(targetRange);
        if (target == null) {
            return;
        }

        int rotation = rotations.merge(bossId, 1, Integer::sum) - 1;
        AttackFamily family = chooseFamily(boss, target, rotation);
        launch(boss, target, family, rotation);

        long baseCooldown = Math.clamp(plugin.getConfig().getLong(
                "boss-balance.combat-director.cooldown-seconds", 9L), 5L, 20L);
        long phaseReduction = Math.max(0, boss.getCurrentPhase() - 1) * 1000L;
        nextAttackAt.put(bossId, now + Math.max(4500L, baseCooldown * 1000L - phaseReduction));
    }

    public void cleanup(UUID bossId) {
        BukkitTask task = pendingAttacks.remove(bossId);
        if (task != null) {
            task.cancel();
        }
        nextAttackAt.remove(bossId);
        rotations.remove(bossId);
    }

    public void shutdown() {
        pendingAttacks.values().forEach(BukkitTask::cancel);
        pendingAttacks.clear();
        nextAttackAt.clear();
        rotations.clear();
    }

    private AttackFamily chooseFamily(OdysseyBoss boss, Player target, int rotation) {
        BossCombatProfile profile = BossCombatProfile.forBoss(boss.getId());
        List<AttackFamily> candidates = new ArrayList<>(profile.families());
        candidates.sort(Comparator.comparingInt(Enum::ordinal));
        double distanceSquared = target.getLocation().distanceSquared(boss.getEntity().getLocation());
        if (distanceSquared <= 64.0D && candidates.contains(AttackFamily.GROUND)) {
            return AttackFamily.GROUND;
        }
        if (distanceSquared >= 225.0D && candidates.contains(AttackFamily.RANGED)) {
            return AttackFamily.RANGED;
        }
        return candidates.get(Math.floorMod(rotation, candidates.size()));
    }

    private void launch(OdysseyBoss boss, Player target, AttackFamily family, int rotation) {
        int variant = Math.floorMod(rotation / 2, 3);
        switch (family) {
            case AERIAL -> {
                if (variant == 0) starfall(boss, target);
                else if (variant == 1) gravityWell(boss, target);
                else airSlam(boss, target);
            }
            case GROUND -> {
                if (variant == 0) warStomp(boss);
                else if (variant == 1) vortexPull(boss, target);
                else shieldBash(boss, target);
            }
            case RANGED -> {
                if (variant == 0) chainLightning(boss, target);
                else if (variant == 1) spiritBeam(boss, target);
                else arcaneMissiles(boss, target);
            }
        }
    }

    private void starfall(OdysseyBoss boss, Player target) {
        Location impact = target.getLocation().clone();
        telegraph(boss, "Lluvia estelar", impact, AERIAL_DUST, Sound.BLOCK_BEACON_POWER_SELECT, () -> {
            World world = impact.getWorld();
            world.spawnParticle(Particle.END_ROD, impact.clone().add(0, 8, 0), 70, 3.5, 5.5, 3.5, 0.08);
            world.spawnParticle(Particle.EXPLOSION, impact, 4, 2.0, 0.5, 2.0, 0.0);
            world.playSound(impact, Sound.ENTITY_GENERIC_EXPLODE, 1.2F, 1.4F);
            damagePlayers(boss, impact, 5.5D, 13.0D, player -> player.setVelocity(player.getVelocity().setY(0.75D)));
        });
    }

    private void gravityWell(OdysseyBoss boss, Player target) {
        Location center = target.getLocation().clone();
        telegraph(boss, "Pozo gravitatorio", center, AERIAL_DUST, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, () -> {
            center.getWorld().spawnParticle(Particle.PORTAL, center.clone().add(0, 1, 0), 90, 3.0, 1.2, 3.0, 0.15);
            for (Player player : playersNear(center, 9.0D)) {
                Vector pull = center.toVector().subtract(player.getLocation().toVector());
                if (pull.lengthSquared() > 0.05D) {
                    player.setVelocity(pull.normalize().multiply(1.05D).setY(0.22D));
                }
                player.damage(9.0D, boss.getEntity());
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, 2));
            }
        });
    }

    private void airSlam(OdysseyBoss boss, Player target) {
        Location impact = target.getLocation().clone();
        telegraph(boss, "Impacto celeste", impact, AERIAL_DUST, Sound.ENTITY_BREEZE_CHARGE, () -> {
            impact.getWorld().spawnParticle(Particle.GUST_EMITTER_LARGE, impact, 3, 1.5, 0.2, 1.5, 0.0);
            impact.getWorld().playSound(impact, Sound.ENTITY_BREEZE_WIND_BURST, 1.5F, 0.65F);
            damagePlayers(boss, impact, 6.0D, 11.0D, player -> {
                Vector away = player.getLocation().toVector().subtract(impact.toVector());
                if (away.lengthSquared() < 0.01D) away = new Vector(0.1D, 0, 0.1D);
                player.setVelocity(away.normalize().multiply(0.9D).setY(1.0D));
            });
        });
    }

    private void warStomp(OdysseyBoss boss) {
        Location center = boss.getEntity().getLocation().clone();
        telegraph(boss, "Pisotón de guerra", center, GROUND_DUST, Sound.ENTITY_RAVAGER_ROAR, () -> {
            center.getWorld().spawnParticle(Particle.BLOCK, center, 80, 4.0, 0.25, 4.0, 0.1,
                    center.clone().add(0, -1, 0).getBlock().getBlockData());
            center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.4F, 0.6F);
            damagePlayers(boss, center, 8.0D, 12.0D, player -> player.setVelocity(
                    player.getVelocity().add(new Vector(0, 0.8D, 0))));
        });
    }

    private void vortexPull(OdysseyBoss boss, Player target) {
        Location center = boss.getEntity().getLocation().clone();
        telegraph(boss, "Vórtice de cadenas", center, GROUND_DUST, Sound.BLOCK_CHAIN_PLACE, () -> {
            center.getWorld().spawnParticle(Particle.WITCH, center.clone().add(0, 1, 0), 65, 4.5, 1.0, 4.5, 0.12);
            for (Player player : playersNear(center, 10.0D)) {
                Vector pull = center.toVector().subtract(player.getLocation().toVector());
                if (pull.lengthSquared() > 0.05D) {
                    player.setVelocity(pull.normalize().multiply(1.2D).setY(0.18D));
                }
                player.damage(player.equals(target) ? 10.0D : 7.0D, boss.getEntity());
            }
        });
    }

    private void shieldBash(OdysseyBoss boss, Player target) {
        Location targetLocation = target.getLocation().clone();
        telegraph(boss, "Embestida de escudo", targetLocation, GROUND_DUST, Sound.ITEM_SHIELD_BLOCK, () -> {
            if (!target.isOnline() || target.getWorld() != boss.getEntity().getWorld()) return;
            Vector away = target.getLocation().toVector().subtract(boss.getEntity().getLocation().toVector());
            if (away.lengthSquared() < 0.01D) away = target.getLocation().getDirection();
            target.setVelocity(away.normalize().multiply(1.6D).setY(0.55D));
            target.damage(15.0D, boss.getEntity());
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1));
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 30, 0.4, 0.7, 0.4, 0.15);
        });
    }

    private void chainLightning(OdysseyBoss boss, Player target) {
        Location impact = target.getLocation().clone();
        telegraph(boss, "Relámpago encadenado", impact, RANGED_DUST, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, () -> {
            List<Player> victims = playersNear(impact, 12.0D).stream().limit(4).toList();
            Location previous = boss.getEntity().getEyeLocation();
            for (Player victim : victims) {
                drawLine(previous, victim.getEyeLocation(), Particle.ELECTRIC_SPARK, null);
                victim.damage(10.0D, boss.getEntity());
                previous = victim.getEyeLocation();
            }
            impact.getWorld().playSound(impact, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.3F, 1.1F);
        });
    }

    private void spiritBeam(OdysseyBoss boss, Player target) {
        Location snapshot = target.getEyeLocation().clone();
        telegraph(boss, "Haz espiritual", snapshot, RANGED_DUST, Sound.ENTITY_EVOKER_PREPARE_ATTACK, () -> {
            Location origin = boss.getEntity().getEyeLocation();
            drawLine(origin, snapshot, Particle.SOUL_FIRE_FLAME, RANGED_DUST);
            damagePlayers(boss, snapshot, 2.4D, 14.0D,
                    player -> player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1)));
            snapshot.getWorld().playSound(snapshot, Sound.ENTITY_WITHER_SHOOT, 1.1F, 1.35F);
        });
    }

    private void arcaneMissiles(OdysseyBoss boss, Player target) {
        telegraph(boss, "Misiles arcanos", target.getLocation(), RANGED_DUST, Sound.ENTITY_ILLUSIONER_CAST_SPELL, () -> {
            List<Player> victims = playersNear(boss.getEntity().getLocation(), 32.0D).stream().limit(3).toList();
            for (Player victim : victims) {
                drawLine(boss.getEntity().getEyeLocation(), victim.getEyeLocation(), Particle.END_ROD, RANGED_DUST);
                victim.damage(9.0D, boss.getEntity());
            }
            if (victims.isEmpty() && target.isOnline()) {
                target.damage(9.0D, boss.getEntity());
            }
        });
    }

    private void telegraph(OdysseyBoss boss, String attackName, Location center,
                           Particle.DustOptions dust, Sound sound, Runnable impact) {
        if (center.getWorld() != boss.getEntity().getWorld()) return;
        boss.announceAttack(attackName);
        drawRing(center, 4.0D, dust);
        center.getWorld().playSound(center, sound, 0.9F, 0.85F);
        long delay = Math.clamp(plugin.getConfig().getLong(
                "boss-balance.combat-director.telegraph-ticks", 24L), 12L, 50L);
        scheduleImpact(boss, delay, impact);
    }

    private void scheduleImpact(OdysseyBoss boss, long delay, Runnable impact) {
        UUID bossId = boss.getEntity().getUniqueId();
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pendingAttacks.remove(bossId);
            if (!isAlive(boss.getEntity())) return;
            try {
                impact.run();
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("[BossDirector] Ataque de " + boss.getId()
                        + " cancelado: " + exception.getMessage());
            }
        }, delay);
        BukkitTask previous = pendingAttacks.put(bossId, task);
        if (previous != null) previous.cancel();
    }

    private void damagePlayers(OdysseyBoss boss, Location center, double radius, double damage,
                               java.util.function.Consumer<Player> effect) {
        for (Player player : playersNear(center, radius)) {
            player.damage(damage, boss.getEntity());
            effect.accept(player);
        }
    }

    private List<Player> playersNear(Location center, double radius) {
        double radiusSquared = radius * radius;
        return center.getWorld().getPlayers().stream()
                .filter(player -> player.isOnline() && !player.isDead())
                .filter(player -> player.getGameMode() != org.bukkit.GameMode.CREATIVE)
                .filter(player -> player.getGameMode() != org.bukkit.GameMode.SPECTATOR)
                .filter(player -> player.getLocation().distanceSquared(center) <= radiusSquared)
                .sorted(Comparator.comparingDouble(player -> player.getLocation().distanceSquared(center)))
                .toList();
    }

    private void drawRing(Location center, double radius, Particle.DustOptions dust) {
        for (int point = 0; point < 24; point++) {
            double angle = Math.PI * 2.0D * point / 24.0D;
            Location particle = center.clone().add(Math.cos(angle) * radius, 0.15D, Math.sin(angle) * radius);
            center.getWorld().spawnParticle(Particle.DUST, particle, 1, 0, 0, 0, 0, dust);
        }
    }

    private void drawLine(Location start, Location end, Particle particle, Particle.DustOptions dust) {
        if (start.getWorld() != end.getWorld()) return;
        Vector path = end.toVector().subtract(start.toVector());
        double length = Math.min(40.0D, path.length());
        if (length < 0.05D) return;
        Vector step = path.normalize().multiply(0.65D);
        Location cursor = start.clone();
        for (double travelled = 0; travelled <= length; travelled += 0.65D) {
            if (dust == null) {
                start.getWorld().spawnParticle(particle, cursor, 1, 0, 0, 0, 0);
            } else {
                start.getWorld().spawnParticle(Particle.DUST, cursor, 1, 0, 0, 0, 0, dust);
                start.getWorld().spawnParticle(particle, cursor, 1, 0, 0, 0, 0);
            }
            cursor.add(step);
        }
    }

    private boolean isAlive(LivingEntity entity) {
        return entity != null && entity.isValid() && !entity.isDead();
    }
}
