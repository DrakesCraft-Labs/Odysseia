package org.metamechanists.odysseia.boss.instances;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.metamechanists.odysseia.boss.OdysseyBoss;
import org.metamechanists.odysseia.boss.skills.BossSkill;
import org.metamechanists.odysseia.boss.skills.OlympusSwordsSkill;
import org.metamechanists.odysseia.boss.skills.SoulShieldSkill;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class DiosCorruptoBoss extends OdysseyBoss {

    private final List<BossSkill> skills = new ArrayList<>();
    private final Random random = new Random();
    private static final String HADES_HEAD_BASE64 = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2Y1MDQzYThjNjA0YTJjYTM2NTEzNzQzYTVmYmYxMWQzZWZjOWI5NTRhOTUzNzU5ZGE4ZDRhN2U3YTY0MWUzYiJ9fX0=";

    private final List<EnderCrystal> activeCrystals = new ArrayList<>();
    private boolean shieldActivated = false;

    public DiosCorruptoBoss(LivingEntity entity) {
        super(entity, "dios_corrupto", "§4§lDios Corrupto §7§l- §4El Caído", 2000.0, BarColor.PURPLE, BarStyle.SEGMENTED_20);

        // Scale entity size to make it look like a giant god (1.21+ attribute GENERIC_SCALE)
        var scaleAttr = entity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(2.0);
        }

        // Equip Hades custom head
        ItemStack hadesHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) hadesHead.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§4Yelmo del Dios Corrupto");
            PlayerProfile profile = (PlayerProfile) Bukkit.createProfile(UUID.randomUUID());
            profile.setProperty(new ProfileProperty("textures", HADES_HEAD_BASE64));
            meta.setPlayerProfile(profile);
            hadesHead.setItemMeta(meta);
        }

        if (entity.getEquipment() != null) {
            entity.getEquipment().setHelmet(hadesHead);
            entity.getEquipment().setHelmetDropChance(0.0f);

            // Full netherite gear
            entity.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
            entity.getEquipment().setChestplateDropChance(0.0f);
            entity.getEquipment().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
            entity.getEquipment().setLeggingsDropChance(0.0f);
            entity.getEquipment().setBoots(new ItemStack(Material.NETHERITE_BOOTS));
            entity.getEquipment().setBootsDropChance(0.0f);

            // Flaming netherite sword
            ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
            sword.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.FIRE_ASPECT, 2);
            entity.getEquipment().setItemInMainHand(sword);
            entity.getEquipment().setItemInMainHandDropChance(0.0f);
        }

        // Add skills
        skills.add(new SoulShieldSkill());
        skills.add(new OlympusSwordsSkill());
    }

    @Override
    public void executeSkillsRotation() {
        if (entity == null || entity.isDead()) return;

        Player target = getNearestPlayer(25);
        if (target != null) {
            // First check if shield needs to be activated
            if (entity.getHealth() < maxHealth * 0.5 && !shieldActivated) {
                // Force execute shield skill
                new SoulShieldSkill().execute(this, target);
            } else {
                // Execute random skill
                BossSkill skill = skills.get(random.nextInt(skills.size()));
                skill.execute(this, target);
            }
        }
    }

    @Override
    public void updateBossBar() {
        super.updateBossBar();
        if (isShieldActive()) {
            Location bossLoc = entity.getLocation().add(0, 1.5, 0);
            for (EnderCrystal crystal : activeCrystals) {
                crystal.setBeamTarget(bossLoc);
            }
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();
        for (EnderCrystal crystal : activeCrystals) {
            if (crystal != null && !crystal.isDead()) {
                crystal.remove();
            }
        }
        activeCrystals.clear();
    }

    public List<EnderCrystal> getActiveCrystals() {
        return activeCrystals;
    }

    public boolean isShieldActivated() {
        return shieldActivated;
    }

    public void setShieldActivated(boolean shieldActivated) {
        this.shieldActivated = shieldActivated;
    }

    public boolean isShieldActive() {
        activeCrystals.removeIf(crystal -> crystal == null || crystal.isDead() || !crystal.isValid());
        return !activeCrystals.isEmpty();
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
