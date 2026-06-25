package org.metamechanists.odysseia.boss.skills;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pillager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.metamechanists.odysseia.boss.OdysseyBoss;

import java.util.Random;

public class SpartanSummonSkill implements BossSkill {

    private final Random random = new Random();

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        if (boss.getEntity() == null || boss.getEntity().isDead()) {
            return;
        }

        Location loc = boss.getEntity().getLocation();
        loc.getWorld().playSound(loc, Sound.EVENT_RAID_HORN, 1.5f, 1.0f);
        loc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 30, 1.0, 1.0, 1.0, 0.05);

        int count = 2 + random.nextInt(2); // Spawns 2-3 Spartans
        for (int i = 0; i < count; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 6;
            double offsetZ = (random.nextDouble() - 0.5) * 6;
            Location spawnLoc = loc.clone().add(offsetX, 0, offsetZ);

            Pillager spartan = (Pillager) loc.getWorld().spawnEntity(spawnLoc, EntityType.PILLAGER);
            spartan.setCustomName("§c§lSoldado Espartano");
            spartan.setCustomNameVisible(true);
            spartan.setRemoveWhenFarAway(true);
            spartan.setTarget(target);

            if (spartan.getEquipment() != null) {
                // Equip with shield and iron sword
                spartan.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                spartan.getEquipment().setItemInMainHandDropChance(0.0f);
                spartan.getEquipment().setItemInOffHand(new ItemStack(Material.SHIELD));
                spartan.getEquipment().setItemInOffHandDropChance(0.0f);
            }
        }

        target.sendMessage("§c§l¡Ares ha invocado Soldados Espartanos para defenderlo!");
    }
}
