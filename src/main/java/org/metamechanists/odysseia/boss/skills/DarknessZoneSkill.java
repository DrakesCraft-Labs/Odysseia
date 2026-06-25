package org.metamechanists.odysseia.boss.skills;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.metamechanists.odysseia.boss.OdysseyBoss;

public class DarknessZoneSkill implements BossSkill {

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        if (boss.getEntity() == null || boss.getEntity().isDead()) {
            return;
        }

        Location loc = boss.getEntity().getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.5f);
        
        // Spawn dark particles
        loc.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, loc.clone().add(0, 1.0, 0), 100, 5.0, 1.0, 5.0, 0.01);
        loc.getWorld().spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 1.0, 0), 50, 4.0, 1.0, 4.0, 0.05);

        for (Entity entity : boss.getEntity().getNearbyEntities(10.0, 10.0, 10.0)) {
            if (entity instanceof Player p && !p.isDead()) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0)); // 5 seconds blindness
                p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1)); // 5 seconds wither II
                p.sendMessage("§8§l¡Hades desata una Zona de Oscuridad! Sientes tu energía drenarse...");
            }
        }
    }
}
