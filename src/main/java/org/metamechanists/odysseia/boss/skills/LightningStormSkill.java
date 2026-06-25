package org.metamechanists.odysseia.boss.skills;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.metamechanists.odysseia.boss.OdysseyBoss;

import java.util.Random;

public class LightningStormSkill implements BossSkill {

    private final Random random = new Random();

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        if (boss.getEntity() == null || boss.getEntity().isDead() || target == null || !target.isOnline()) {
            return;
        }

        Location targetLoc = target.getLocation();
        target.sendMessage("§e§l¡Se desata una tormenta eléctrica divina en tu posición!");
        
        // Strike 6 lightnings over 1.5 seconds (30 ticks)
        for (int i = 0; i < 6; i++) {
            long delay = i * 5L;
            org.bukkit.Bukkit.getScheduler().runTaskLater(org.metamechanists.odysseia.Odysseia.getInstance(), () -> {
                if (target.isOnline() && !target.isDead()) {
                    double offsetX = (random.nextDouble() - 0.5) * 12;
                    double offsetZ = (random.nextDouble() - 0.5) * 12;
                    Location strikeLoc = target.getLocation().add(offsetX, 0, offsetZ);
                    strikeLoc.getWorld().strikeLightning(strikeLoc);
                }
            }, delay);
        }

        // Deal flat damage to target and apply slowness
        target.damage(12.0, boss.getEntity()); // 6 hearts
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2)); // 4s slowness 3
    }
}
