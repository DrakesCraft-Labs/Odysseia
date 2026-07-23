package org.metamechanists.odysseia.boss.skills;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.boss.OdysseyBoss;

public final class LokiBlindnessSkill implements BossSkill {

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        if (boss == null || target == null) return;

        Location bossLoc = boss.getEntity().getLocation();
        Location targetLoc = target.getLocation();

        // Calculate location behind player
        Vector direction = targetLoc.getDirection().normalize().multiply(-1.5);
        Location behindLoc = targetLoc.clone().add(direction);
        behindLoc.setDirection(targetLoc.getDirection()); // face same direction

        // Spawn particles at old location
        bossLoc.getWorld().spawnParticle(Particle.PORTAL, bossLoc, 30, 0.5, 1.0, 0.5, 0.1);
        bossLoc.getWorld().playSound(bossLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        // Teleport Loki behind player
        boss.getEntity().teleport(behindLoc);

        // Apply Blindness to player
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 1, false, true));
        target.getWorld().playSound(targetLoc, Sound.ENTITY_WITCH_CELEBRATE, 1.0f, 0.9f);

        // Shoot a burst of magic particles
        behindLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, behindLoc.clone().add(0, 1, 0), 40, 1.0, 0.5, 1.0, 0.05);
        target.damage(8.0, boss.getEntity()); // deal some magic damage
    }
}
