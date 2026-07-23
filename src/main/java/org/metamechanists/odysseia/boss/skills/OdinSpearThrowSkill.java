package org.metamechanists.odysseia.boss.skills;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.boss.OdysseyBoss;

public final class OdinSpearThrowSkill implements BossSkill {

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        if (boss == null || target == null) return;

        Location eyeLoc = boss.getEntity().getEyeLocation();
        Vector direction = target.getEyeLocation().toVector().subtract(eyeLoc.toVector());

        if (direction.lengthSquared() > 0.01) {
            direction.normalize();
        } else {
            direction = new Vector(0, -1, 0);
        }

        eyeLoc.getWorld().playSound(eyeLoc, Sound.ITEM_TRIDENT_THROW, 1.2f, 0.8f);

        // Spawn thrown trident representing Gungnir
        Trident trident = boss.getEntity().launchProjectile(Trident.class);
        trident.setVelocity(direction.multiply(1.8));

        // Tag it so it strikes lightning on hit
        NamespacedKey key = new NamespacedKey(Odysseia.getInstance(), "odin_spear");
        trident.getPersistentDataContainer().set(key, PersistentDataType.STRING, "odin_spear");
    }
}
