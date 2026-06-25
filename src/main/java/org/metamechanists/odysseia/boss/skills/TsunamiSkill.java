package org.metamechanists.odysseia.boss.skills;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.boss.OdysseyBoss;

public class TsunamiSkill implements BossSkill {

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        if (boss.getEntity() == null || boss.getEntity().isDead() || target == null || !target.isOnline()) {
            return;
        }

        Location bossLoc = boss.getEntity().getLocation();
        Location targetLoc = target.getLocation();
        
        Vector direction = targetLoc.toVector().subtract(bossLoc.toVector());
        if (direction.lengthSquared() > 0) {
            direction.normalize();
        } else {
            direction = new Vector(1, 0, 0);
        }

        bossLoc.getWorld().playSound(bossLoc, Sound.ITEM_BUCKET_EMPTY, 1.5f, 0.5f);
        bossLoc.getWorld().playSound(bossLoc, Sound.ENTITY_PLAYER_SPLASH, 1.5f, 0.5f);

        // Project the tsunami wave forward in steps
        for (int i = 1; i <= 8; i++) {
            final double dist = i * 1.5;
            final Vector dirCopy = direction.clone();
            org.bukkit.Bukkit.getScheduler().runTaskLater(org.metamechanists.odysseia.Odysseia.getInstance(), () -> {
                if (boss.getEntity() == null || boss.getEntity().isDead()) return;

                Location waveCenter = boss.getEntity().getLocation().add(dirCopy.multiply(dist)).add(0, 0.5, 0);
                
                // Spawn splash particles in a line perpendicular to the direction
                Vector perp = new Vector(-dirCopy.getZ(), 0, dirCopy.getX()).normalize();
                for (double offset = -3.0; offset <= 3.0; offset += 0.5) {
                    Location partLoc = waveCenter.clone().add(perp.clone().multiply(offset));
                    partLoc.getWorld().spawnParticle(Particle.SPLASH, partLoc, 5, 0.2, 0.5, 0.2, 0.1);
                    partLoc.getWorld().spawnParticle(Particle.FALLING_WATER, partLoc, 3, 0.1, 0.3, 0.1, 0.05);
                }

                // Check collisions with players in this wave segment
                for (Entity entity : waveCenter.getWorld().getNearbyEntities(waveCenter, 2.0, 2.0, 2.0)) {
                    if (entity instanceof Player p && !p.isDead()) {
                        p.damage(5.0, boss.getEntity()); // 2.5 hearts
                        p.setVelocity(dirCopy.clone().multiply(1.5).setY(0.5));
                        p.sendMessage("§9§l¡El Tsunami de Poseidón te ha golpeado!");
                    }
                }
            }, i * 2L);
        }
    }
}
