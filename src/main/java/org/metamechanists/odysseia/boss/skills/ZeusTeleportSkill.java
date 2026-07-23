package org.metamechanists.odysseia.boss.skills;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.boss.OdysseyBoss;

public class ZeusTeleportSkill implements BossSkill {

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        if (boss.getEntity() == null || boss.getEntity().isDead() || target == null || !target.isOnline()) {
            return;
        }

        Location targetLoc = target.getLocation();
        Vector behindDir = targetLoc.getDirection().normalize().multiply(-1.5);
        Location teleLoc = targetLoc.clone().add(behindDir);
        
        // Ensure teleport location height is aligned
        teleLoc.setY(targetLoc.getY());

        Location origin = boss.getEntity().getLocation();
        origin.getWorld().playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);

        // Teleport
        boss.getEntity().teleport(teleLoc);
        
        teleLoc.getWorld().playSound(teleLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
        teleLoc.getWorld().strikeLightning(teleLoc);

        target.damage(6.0, boss.getEntity()); // 3 hearts from the lightning impact
        target.sendMessage("§e§l¡Zeus se ha teletransportado detrás de ti con un estallido de rayos!");
    }
}
