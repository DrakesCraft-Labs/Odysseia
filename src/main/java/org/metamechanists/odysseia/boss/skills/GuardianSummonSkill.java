package org.metamechanists.odysseia.boss.skills;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.metamechanists.odysseia.boss.OdysseyBoss;

import java.util.Random;

public class GuardianSummonSkill implements BossSkill {

    private final Random random = new Random();

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        if (boss.getEntity() == null || boss.getEntity().isDead()) {
            return;
        }

        Location loc = boss.getEntity().getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.5f, 0.5f);
        loc.getWorld().spawnParticle(Particle.GLOW, loc, 40, 1.0, 1.0, 1.0, 0.1);

        for (int i = 0; i < 2; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 5;
            double offsetZ = (random.nextDouble() - 0.5) * 5;
            Location spawnLoc = loc.clone().add(offsetX, 0, offsetZ);

            Drowned guardian = (Drowned) loc.getWorld().spawnEntity(spawnLoc, EntityType.DROWNED);
            guardian.setCustomName("§9§lGuardián de las Profundidades");
            guardian.setCustomNameVisible(true);
            guardian.setRemoveWhenFarAway(true);
            guardian.setTarget(target);

            if (guardian.getEquipment() != null) {
                // Equip Trident
                guardian.getEquipment().setItemInMainHand(new ItemStack(Material.TRIDENT));
                guardian.getEquipment().setItemInMainHandDropChance(0.0f);
            }
        }

        target.sendMessage("§9§l¡Poseidón invoca Guardianes de las Profundidades!");
    }
}
