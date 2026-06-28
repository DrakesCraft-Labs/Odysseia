package org.metamechanists.odysseia.boss.skills;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.boss.OdysseyBoss;

public class ThrowRockSkill implements BossSkill {

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        if (boss == null || target == null || !target.isOnline()) {
            return;
        }

        LivingEntity bossEntity = boss.getEntity();
        Location bossEye = bossEntity.getEyeLocation();
        Location targetLoc = target.getLocation();

        // Calculate direction vector
        Vector direction = targetLoc.toVector().subtract(bossEye.toVector());
        double distance = direction.length();
        if (distance < 1) return;

        direction.normalize().multiply(1.2);
        direction.setY(direction.getY() + 0.25); // Slightly arc upwards

        Location spawnLoc = bossEye.clone().add(bossEye.getDirection().multiply(1.5));
        FallingBlock rock = bossEntity.getWorld().spawnFallingBlock(spawnLoc, Bukkit.createBlockData(Material.COBBLESTONE));
        rock.setDropItem(false);
        rock.setHurtEntities(false);
        rock.setVelocity(direction);

        // Add a tag to prevent any placement in case the task fails
        rock.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(Odysseia.getInstance(), "boss_rock"),
                org.bukkit.persistence.PersistentDataType.BYTE,
                (byte) 1
        );

        // Track the rock
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;
                if (ticks > 100 || rock.isDead() || !rock.isValid() || rock.isOnGround()) {
                    explodeRock(rock, bossEntity);
                    cancel();
                    return;
                }

                // Particle trail
                rock.getWorld().spawnParticle(Particle.CRIT, rock.getLocation(), 3, 0.1, 0.1, 0.1, 0.01);

                // Check collision with nearby players
                for (Entity nearby : rock.getNearbyEntities(1.5, 1.5, 1.5)) {
                    if (nearby instanceof Player p && !p.getUniqueId().equals(bossEntity.getUniqueId())) {
                        explodeRock(rock, bossEntity);
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(Odysseia.getInstance(), 1L, 2L);
    }

    private void explodeRock(FallingBlock rock, LivingEntity bossEntity) {
        if (rock == null) return;
        Location loc = rock.getLocation();
        rock.remove();

        // Effects
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 1);
        loc.getWorld().spawnParticle(Particle.BLOCK, loc, 40, 0.5, 0.5, 0.5, 0.1, Bukkit.createBlockData(Material.COBBLESTONE));
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
        loc.getWorld().playSound(loc, Sound.BLOCK_STONE_BREAK, 1.2f, 0.6f);

        // Damage players in range
        loc.getWorld().getNearbyEntities(loc, 4.0, 4.0, 4.0).forEach(entity -> {
            if (entity instanceof LivingEntity living && !living.getUniqueId().equals(bossEntity.getUniqueId())) {
                // Check if it's not another boss
                if (!living.getPersistentDataContainer().has(new org.bukkit.NamespacedKey(Odysseia.getInstance(), "boss_type"), org.bukkit.persistence.PersistentDataType.STRING)) {
                    living.damage(12.0, bossEntity); // 6 hearts
                    // Apply velocity blast away
                    Vector push = living.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(0.8);
                    push.setY(0.4);
                    living.setVelocity(push);
                }
            }
        });
    }
}
