package org.metamechanists.odysseia.boss.skills;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.boss.OdysseyBoss;

public class MjolnirThrowSkill implements BossSkill {

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        Location bossLoc = boss.getEntity().getLocation().add(0, 1, 0);
        Location targetLoc = target.getLocation().add(0, 1, 0);

        // Projectile toward target
        Vector dir = targetLoc.toVector().subtract(bossLoc.toVector()).normalize().multiply(1.5);

        FallingBlock mjolnir = bossLoc.getWorld().spawnFallingBlock(bossLoc, Material.IRON_BLOCK.createBlockData());
        mjolnir.setVelocity(dir);
        mjolnir.setDropItem(false);
        mjolnir.setHurtEntities(true);
        mjolnir.setDamagePerBlock(3.0f);
        mjolnir.setMaxDamage(6);

        NamespacedKey key = new NamespacedKey(Odysseia.getInstance(), "boss_rock");
        mjolnir.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        // Return projectile (simula bumerang) — lanzado 1s después desde el target hacia el boss
        org.bukkit.Bukkit.getScheduler().runTaskLater(Odysseia.getInstance(), () -> {
            if (target.isDead() || !target.isOnline()) return;
            Location retLoc = target.getLocation().add(0, 1, 0);
            Location bossRetLoc = boss.getEntity().getLocation().add(0, 1, 0);
            Vector retDir = bossRetLoc.toVector().subtract(retLoc.toVector()).normalize().multiply(1.5);

            FallingBlock returnBlock = retLoc.getWorld().spawnFallingBlock(retLoc, Material.IRON_BLOCK.createBlockData());
            returnBlock.setVelocity(retDir);
            returnBlock.setDropItem(false);
            returnBlock.setHurtEntities(true);
            returnBlock.setDamagePerBlock(3.0f);
            returnBlock.setMaxDamage(6);
            returnBlock.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        }, 20L);
    }
}
