package org.metamechanists.odysseia.boss.skills;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.boss.OdysseyBoss;

import java.util.Random;

public final class OdinSkyLightningSkill implements BossSkill {

    private final Random random = new Random();

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        if (boss == null || target == null) return;

        Location targetLoc = target.getLocation();
        targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.5f, 0.8f);

        // Warning particles
        targetLoc.getWorld().spawnParticle(Particle.EXPLOSION, targetLoc, 20, 3.0, 0.5, 3.0, 0.05);

        // Strike 5 bolts around player with a slight delay
        for (int i = 0; i < 5; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = 1 + random.nextDouble() * 4;
            Location strikeLoc = targetLoc.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            strikeLoc.setY(strikeLoc.getWorld().getHighestBlockYAt(strikeLoc));

            Bukkit.getScheduler().runTaskLater(Odysseia.getInstance(), () -> {
                if (strikeLoc.getWorld() != null) {
                    strikeLoc.getWorld().strikeLightning(strikeLoc);
                }
            }, i * 4L); // 0.2s, 0.4s, 0.6s, etc.
        }
    }
}
