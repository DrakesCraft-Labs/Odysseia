package org.metamechanists.odysseia.boss.skills;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.metamechanists.odysseia.boss.OdysseyBoss;

import java.util.Random;

public class UnderworldPortalSkill implements BossSkill {

    private final Random random = new Random();

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        if (boss.getEntity() == null || boss.getEntity().isDead()) {
            return;
        }

        Location loc = boss.getEntity().getLocation();
        loc.getWorld().playSound(loc, Sound.BLOCK_PORTAL_TRAVEL, 1.2f, 0.8f);
        loc.getWorld().spawnParticle(Particle.PORTAL, loc.clone().add(0, 1.0, 0), 60, 1.0, 1.0, 1.0, 0.1);

        for (int i = 0; i < 3; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 5;
            double offsetZ = (random.nextDouble() - 0.5) * 5;
            Location spawnLoc = loc.clone().add(offsetX, 1.5, offsetZ);

            Vex specter = (Vex) loc.getWorld().spawnEntity(spawnLoc, EntityType.VEX);
            specter.setCustomName("§7§lEspectro del Inframundo");
            specter.setCustomNameVisible(true);
            specter.setRemoveWhenFarAway(true);
            specter.setTarget(target);
            
            // Boost speed/attack slightly
            var speedAttr = specter.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED);
            if (speedAttr != null) {
                speedAttr.setBaseValue(0.4);
            }
        }

        target.sendMessage("§5§l¡Hades abre un portal al Inframundo! Varios espectros emergen...");
    }
}
