package org.metamechanists.odysseia.boss.skills;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.boss.OdysseyBoss;

public class EarthquakeSkill implements BossSkill {

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        if (boss == null) return;

        LivingEntity bossEntity = boss.getEntity();
        Location center = bossEntity.getLocation();

        // Audio-visuals
        center.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.2f, 0.6f);
        center.getWorld().playSound(center, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.5f);
        
        // Circular particle shockwave
        for (int i = 0; i < 360; i += 15) {
            double rad = Math.toRadians(i);
            double x = Math.cos(rad) * 6.0;
            double z = Math.sin(rad) * 6.0;
            Location pLoc = center.clone().add(x, 0.2, z);
            center.getWorld().spawnParticle(Particle.BLOCK, pLoc, 5, 0.2, 0.2, 0.2, 0.1, Bukkit.createBlockData(Material.DIRT));
            center.getWorld().spawnParticle(Particle.CLOUD, pLoc, 3, 0.1, 0.1, 0.1, 0.02);
        }

        // Damage and knockback entities in range
        center.getWorld().getNearbyEntities(center, 8.0, 4.0, 8.0).forEach(entity -> {
            if (entity instanceof LivingEntity living && !living.getUniqueId().equals(bossEntity.getUniqueId())) {
                living.damage(8.0, bossEntity); // 4 hearts
                
                // Apply knockback
                Vector dir = living.getLocation().toVector().subtract(center.toVector());
                double dist = dir.length();
                if (dist > 0.1) {
                    dir.normalize().multiply(1.2);
                    dir.setY(0.5); // Knock upwards
                    living.setVelocity(dir);
                }
            }
        });
    }
}
