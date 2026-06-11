package org.metamechanists.odysseia.boss.skills;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.boss.OdysseyBoss;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PolymorphSkill implements BossSkill {

    @Getter
    private static final Map<UUID, ItemStack> polymorphedHelmets = new HashMap<>();

    private static final String PIG_HEAD_BASE64 = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjJmMDQ5MWQ3ZGMyMGFjNDA2MTFmNzhkM2MxMmVmN2RiN2UxY2FlM2Q2YTFmOWU1MWYzNDQ4Y2QxNWQ5N2NkIn19fQ==";

    @Override
    public void execute(OdysseyBoss boss, Player target) {
        if (target == null || !target.isOnline() || target.isDead()) {
            return;
        }

        UUID uuid = target.getUniqueId();
        if (polymorphedHelmets.containsKey(uuid)) {
            return; // Already polymorphed
        }

        // Save current helmet
        ItemStack originalHelmet = target.getInventory().getHelmet();
        polymorphedHelmets.put(uuid, originalHelmet);

        // Create Pig Head
        ItemStack pigHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) pigHead.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dCabeza de Cerdo"));
            PlayerProfile profile = (PlayerProfile) Bukkit.createProfile(UUID.randomUUID());
            profile.setProperty(new ProfileProperty("textures", PIG_HEAD_BASE64));
            meta.setPlayerProfile(profile);
            pigHead.setItemMeta(meta);
        }

        // Apply effects
        target.getInventory().setHelmet(pigHead);
        // Clean old effects of blindness and slowness to refresh them
        target.removePotionEffect(PotionEffectType.BLINDNESS);
        target.removePotionEffect(PotionEffectType.SLOWNESS);
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0, false, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2, false, false, true));

        // Audio-visuals
        target.getWorld().spawnParticle(Particle.WITCH, target.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PIG_AMBIENT, 1.0f, 1.0f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITCH_CELEBRATE, 1.0f, 1.0f);
        target.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d&l¡Circe te ha convertido temporalmente en un cerdo!"));

        // Schedule restore in 5 seconds (100 ticks)
        Bukkit.getScheduler().runTaskLater(Odysseia.getInstance(), () -> restorePlayer(target), 100L);
    }

    public static void restorePlayer(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        if (polymorphedHelmets.containsKey(uuid)) {
            ItemStack original = polymorphedHelmets.remove(uuid);
            if (player.isOnline()) {
                player.getInventory().setHelmet(original);
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 1.2f);
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a&l¡Has recuperado tu forma humana!"));
            }
        }
    }
}
