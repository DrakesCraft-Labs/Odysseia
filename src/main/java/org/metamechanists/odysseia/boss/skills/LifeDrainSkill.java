package org.metamechanists.odysseia.boss.skills;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.metamechanists.odysseia.boss.OdysseyBoss;

public class LifeDrainSkill implements BossSkill {

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        if (boss.getEntity() == null || boss.getEntity().isDead() || target == null || !target.isOnline()) {
            return;
        }

        Location bossLoc = boss.getEntity().getLocation().add(0, 1.5, 0);
        Location targetLoc = target.getLocation().add(0, 1.0, 0);

        bossLoc.getWorld().playSound(bossLoc, Sound.ENTITY_BAT_DEATH, 1.5f, 0.6f);
        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_WITHER_HURT, 1.0f, 1.2f);

        // Spawn particles between target and boss
        double distance = bossLoc.distance(targetLoc);
        int steps = (int) (distance * 2);
        for (int i = 0; i <= steps; i++) {
            double ratio = (double) i / steps;
            double x = targetLoc.getX() + (bossLoc.getX() - targetLoc.getX()) * ratio;
            double y = targetLoc.getY() + (bossLoc.getY() - targetLoc.getY()) * ratio;
            double z = targetLoc.getZ() + (bossLoc.getZ() - targetLoc.getZ()) * ratio;
            Location point = new Location(bossLoc.getWorld(), x, y, z);
            
            // Red particles for health draining
            point.getWorld().spawnParticle(Particle.DUST_PLUME, point, 1, 0, 0, 0, 0);
        }

        // Apply health drainage
        double damage = 6.0; // 3 hearts
        target.damage(damage, boss.getEntity());
        
        double currentHealth = boss.getEntity().getHealth();
        double newHealth = Math.min(boss.getMaxHealth(), currentHealth + damage);
        boss.getEntity().setHealth(newHealth);

        target.sendMessage("§d§l¡Hades drena tu fuerza vital para curarse!");
    }
}
