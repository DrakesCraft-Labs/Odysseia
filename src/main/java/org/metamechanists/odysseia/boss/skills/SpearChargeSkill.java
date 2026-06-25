package org.metamechanists.odysseia.boss.skills;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.boss.OdysseyBoss;

public class SpearChargeSkill implements BossSkill {

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        if (boss.getEntity() == null || boss.getEntity().isDead() || target == null || !target.isOnline()) {
            return;
        }

        Location bossLoc = boss.getEntity().getLocation();
        Location targetLoc = target.getLocation();
        Vector direction = targetLoc.toVector().subtract(bossLoc.toVector());
        if (direction.lengthSquared() > 0) {
            direction.normalize().multiply(1.8);
        } else {
            direction = new Vector(0, 0, 0);
        }
        
        // Launch boss towards target
        boss.getEntity().setVelocity(direction.setY(0.2));
        bossLoc.getWorld().playSound(bossLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.5f);

        // Check for impact after 8 ticks
        org.bukkit.Bukkit.getScheduler().runTaskLater(Odysseia.getInstance(), () -> {
            if (boss.getEntity() == null || boss.getEntity().isDead()) return;
            Location currentLoc = boss.getEntity().getLocation();
            currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.5f, 1.2f);
            
            for (Entity nearby : boss.getEntity().getNearbyEntities(3.0, 3.0, 3.0)) {
                if (nearby instanceof Player p && !p.isDead()) {
                    p.damage(8.0, boss.getEntity()); // 4 hearts
                    p.setVelocity(p.getLocation().toVector().subtract(currentLoc.toVector()).normalize().multiply(1.2).setY(0.4));
                    p.sendMessage("§c§l¡Ares te ha embestido con su Lanza!");
                }
            }
        }, 8L);
    }
}
