package org.metamechanists.odysseia.boss.instances;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.metamechanists.odysseia.boss.OdysseyBoss;
import org.metamechanists.odysseia.boss.skills.BossSkill;
import org.metamechanists.odysseia.boss.skills.OdinRavensSkill;
import org.metamechanists.odysseia.boss.skills.OdinSkyLightningSkill;
import org.metamechanists.odysseia.boss.skills.OdinSpearThrowSkill;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class OdinBoss extends OdysseyBoss {

    private final List<BossSkill> skills = new ArrayList<>();
    private final Random random = new Random();
    private static final String ODIN_HEAD_BASE64 = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTYxMjkyNjcyMjcyNmEyNGQzY2I0MTIwYWUzNjJhN2Y0MmUxODljNGQyZjQ1YjhlODBlNzUzZTdiMTE0M2RkNyJ9fX0=";

    public OdinBoss(LivingEntity entity) {
        super(entity, "odin", "§e§lOdín §7§l- §ePadre de Todo", 1800.0, BarColor.BLUE, BarStyle.SEGMENTED_20);

        // Scale entity size slightly larger (1.4)
        var scaleAttr = entity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(1.4);
        }

        // Equip Odin custom head
        ItemStack odinHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) odinHead.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§eYelmo de Odín");
            PlayerProfile profile = (PlayerProfile) Bukkit.createProfile(UUID.randomUUID());
            profile.setProperty(new ProfileProperty("textures", ODIN_HEAD_BASE64));
            meta.setPlayerProfile(profile);
            odinHead.setItemMeta(meta);
        }

        if (entity.getEquipment() != null) {
            entity.getEquipment().setHelmet(odinHead);
            entity.getEquipment().setHelmetDropChance(0.0f);

            // Full netherite gear
            entity.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
            entity.getEquipment().setChestplateDropChance(0.0f);
            entity.getEquipment().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
            entity.getEquipment().setLeggingsDropChance(0.0f);
            entity.getEquipment().setBoots(new ItemStack(Material.NETHERITE_BOOTS));
            entity.getEquipment().setBootsDropChance(0.0f);

            // Gungnir Trident
            ItemStack trident = new ItemStack(Material.TRIDENT);
            trident.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.IMPALING, 5);
            entity.getEquipment().setItemInMainHand(trident);
            entity.getEquipment().setItemInMainHandDropChance(0.0f);
        }

        // Add skills
        skills.add(new OdinRavensSkill());
        skills.add(new OdinSpearThrowSkill());
        skills.add(new OdinSkyLightningSkill());
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
