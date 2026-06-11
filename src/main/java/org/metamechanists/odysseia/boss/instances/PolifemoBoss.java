package org.metamechanists.odysseia.boss.instances;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.metamechanists.odysseia.boss.OdysseyBoss;
import org.metamechanists.odysseia.boss.skills.BossSkill;
import org.metamechanists.odysseia.boss.skills.EarthquakeSkill;
import org.metamechanists.odysseia.boss.skills.ThrowRockSkill;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class PolifemoBoss extends OdysseyBoss {

    private final List<BossSkill> skills = new ArrayList<>();
    private final Random random = new Random();
    private static final String CYCLOPS_HEAD_BASE64 = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzE0MjBhOWFlNDk0MDFmNTY1MjY2MmFhZjRmMzlmZGY2N2RlMjM3OWFhYzJmODJkY2JlNGNkMzE4ZDgifX19";

    public PolifemoBoss(LivingEntity entity) {
        super(entity, "polifemo", "§6§lPolifemo §7§l- §6El Cíclope", 1500.0, BarColor.RED, BarStyle.SEGMENTED_12);
        
        // Equip Cyclops head
        ItemStack cyclopsHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) cyclopsHead.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6Cabeza de Cíclope");
            PlayerProfile profile = (PlayerProfile) Bukkit.createProfile(UUID.randomUUID());
            profile.setProperty(new ProfileProperty("textures", CYCLOPS_HEAD_BASE64));
            meta.setPlayerProfile(profile);
            cyclopsHead.setItemMeta(meta);
        }
        
        if (entity.getEquipment() != null) {
            entity.getEquipment().setHelmet(cyclopsHead);
            entity.getEquipment().setHelmetDropChance(0.0f);
            
            // Equip netherite axe as a club
            entity.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_AXE));
            entity.getEquipment().setItemInMainHandDropChance(0.0f);
        }

        skills.add(new ThrowRockSkill());
        skills.add(new EarthquakeSkill());
    }

    @Override
    public void executeSkillsRotation() {
        if (entity == null || entity.isDead()) return;

        Player target = getNearestPlayer(25);
        if (target != null) {
            BossSkill skill = skills.get(random.nextInt(skills.size()));
            skill.execute(this, target);
        }
    }

    public void updatePathfinding() {
        if (entity == null || entity.isDead()) return;
        
        Player target = getNearestPlayer(40);
        if (target != null && entity instanceof Mob mob) {
            mob.setTarget(target);
            mob.getPathfinder().moveTo(target.getLocation(), 1.25);
        }
    }

    private Player getNearestPlayer(double radius) {
        Player target = null;
        double nearestDist = Double.MAX_VALUE;
        for (org.bukkit.entity.Entity e : entity.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof Player p && p.isOnline() && !p.isDead()) {
                double dist = p.getLocation().distance(entity.getLocation());
                if (dist < nearestDist) {
                    nearestDist = dist;
                    target = p;
                }
            }
        }
        return target;
    }
}
