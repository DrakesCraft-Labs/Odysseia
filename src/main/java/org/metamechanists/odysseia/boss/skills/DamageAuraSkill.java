package org.metamechanists.odysseia.boss.skills;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.boss.OdysseyBoss;

public class DamageAuraSkill implements BossSkill {

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        if (boss.getEntity() == null || boss.getEntity().isDead()) {
            return;
        }

        Location loc = boss.getEntity().getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, 1.5f, 0.8f);

        // Run damage ticks for 4 seconds (80 ticks), every 20 ticks
        for (int i = 0; i < 4; i++) {
            long delay = i * 20L;
            org.bukkit.Bukkit.getScheduler().runTaskLater(Odysseia.getInstance(), () -> {
                if (boss.getEntity() == null || boss.getEntity().isDead()) return;

                Location currentLoc = boss.getEntity().getLocation();
                // Spawn circle particles
                double radius = 5.0;
                for (int angle = 0; angle < 360; angle += 15) {
                    double rad = Math.toRadians(angle);
                    double x = radius * Math.cos(rad);
                    double z = radius * Math.sin(rad);
                    currentLoc.getWorld().spawnParticle(Particle.FLAME, currentLoc.clone().add(x, 0.2, z), 1, 0, 0, 0, 0);
                }

                currentLoc.getWorld().spawnParticle(Particle.LAVA, currentLoc.clone().add(0, 1.0, 0), 10, 0.5, 0.5, 0.5, 0.05);

                for (Entity entity : boss.getEntity().getNearbyEntities(radius, radius, radius)) {
                    if (entity instanceof Player p && !p.isDead()) {
                        p.damage(3.0, boss.getEntity()); // 1.5 hearts
                        p.setFireTicks(40); // 2 seconds on fire
                        p.sendMessage("§c§l¡El Aura de Daño de Ares te quema!");
                    }
                }
            }, delay);
        }
    }
}
