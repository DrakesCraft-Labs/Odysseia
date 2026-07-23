package org.metamechanists.odysseia.boss.skills;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.metamechanists.odysseia.boss.OdysseyBoss;

public class DivineShieldSkill implements BossSkill {

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        if (boss.getEntity() == null || boss.getEntity().isDead()) {
            return;
        }

        Location loc = boss.getEntity().getLocation();
        loc.getWorld().playSound(loc, Sound.ITEM_SHIELD_BLOCK, 1.5f, 0.5f);
        loc.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, 1.0f, 0.8f);

        // Apply Resistance 5 for 8 seconds (160 ticks)
        boss.getEntity().addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 160, 4));

        // Spawn shield particles over 8 seconds
        for (int i = 0; i < 8; i++) {
            long delay = i * 20L;
            org.bukkit.Bukkit.getScheduler().runTaskLater(org.metamechanists.odysseia.Odysseia.getInstance(), () -> {
                if (boss.getEntity() == null || boss.getEntity().isDead()) return;
                Location currentLoc = boss.getEntity().getLocation().add(0, 1.0, 0);
                
                // Draw a golden particle sphere or rings
                currentLoc.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, currentLoc, 20, 0.6, 0.8, 0.6, 0.02);
                // INSTANT_EFFECT maps to a data-bearing spell particle on 1.21.11.
                // END_ROD keeps the shield readable without requiring particle data.
                currentLoc.getWorld().spawnParticle(Particle.END_ROD, currentLoc, 10, 0.5, 0.8, 0.5, 0.05);
            }, delay);
        }

        target.sendMessage("§e§l¡Zeus invoca su Escudo Divino! Es temporalmente inmune al daño físico.");
    }
}
