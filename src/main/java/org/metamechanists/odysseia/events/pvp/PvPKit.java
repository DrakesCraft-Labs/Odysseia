package org.metamechanists.odysseia.events.pvp;

import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;

/**
 * Kits competitivos balanceados para el sistema de Torneos PvP de Odysseia.
 */
public enum PvPKit {
    GLADIADOR("Gladiador", "⚔️", "Equilibrio ofensivo cuerpo a cuerpo."),
    TANQUE("Tanque", "🛡️", "Máxima resistencia física con armadura de diamante."),
    ARQUERO("Arquero", "🏹", "Especialista en combate a distancia con arco infinito."),
    BERSERKER("Berserker", "🪓", "Daño colosal con hacha y poción de fuerza pero armadura ligera.");

    @Getter private final String displayName;
    @Getter private final String icon;
    @Getter private final String description;

    PvPKit(String displayName, String icon, String description) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
    }

    public static PvPKit fromString(String name) {
        if (name == null) return null;
        String clean = name.trim().toUpperCase(Locale.ROOT);
        for (PvPKit kit : values()) {
            if (kit.name().equalsIgnoreCase(clean) || kit.displayName.equalsIgnoreCase(clean)) {
                return kit;
            }
        }
        return null;
    }

    public void apply(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);

        switch (this) {
            case GLADIADOR -> {
                // Armadura de hierro Prot II
                player.getInventory().setHelmet(enchant(new ItemStack(Material.IRON_HELMET), Enchantment.PROTECTION, 2));
                player.getInventory().setChestplate(enchant(new ItemStack(Material.IRON_CHESTPLATE), Enchantment.PROTECTION, 2));
                player.getInventory().setLeggings(enchant(new ItemStack(Material.IRON_LEGGINGS), Enchantment.PROTECTION, 2));
                player.getInventory().setBoots(enchant(new ItemStack(Material.IRON_BOOTS), Enchantment.PROTECTION, 2));

                player.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));
                player.getInventory().addItem(enchant(new ItemStack(Material.DIAMOND_SWORD), Enchantment.SHARPNESS, 2));
                player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 16));
                player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 2));
            }
            case TANQUE -> {
                // Armadura de diamante Prot I
                player.getInventory().setHelmet(enchant(new ItemStack(Material.DIAMOND_HELMET), Enchantment.PROTECTION, 1));
                player.getInventory().setChestplate(enchant(new ItemStack(Material.DIAMOND_CHESTPLATE), Enchantment.PROTECTION, 1));
                player.getInventory().setLeggings(enchant(new ItemStack(Material.DIAMOND_LEGGINGS), Enchantment.PROTECTION, 1));
                player.getInventory().setBoots(enchant(new ItemStack(Material.DIAMOND_BOOTS), Enchantment.PROTECTION, 1));

                player.getInventory().setItemInOffHand(new ItemStack(Material.SHIELD));
                ItemStack sword = new ItemStack(Material.IRON_SWORD);
                sword.addEnchantment(Enchantment.KNOCKBACK, 1);
                sword.addEnchantment(Enchantment.SHARPNESS, 1);
                player.getInventory().addItem(sword);
                player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 16));
                player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 1));
                player.getInventory().addItem(createSplashPotion(PotionEffectType.RESISTANCE, 1200, 0));
            }
            case ARQUERO -> {
                // Cota de malla Prot II
                player.getInventory().setHelmet(enchant(new ItemStack(Material.CHAINMAIL_HELMET), Enchantment.PROTECTION, 2));
                player.getInventory().setChestplate(enchant(new ItemStack(Material.CHAINMAIL_CHESTPLATE), Enchantment.PROTECTION, 2));
                player.getInventory().setLeggings(enchant(new ItemStack(Material.CHAINMAIL_LEGGINGS), Enchantment.PROTECTION, 2));
                player.getInventory().setBoots(enchant(new ItemStack(Material.CHAINMAIL_BOOTS), Enchantment.PROTECTION, 2));

                ItemStack bow = new ItemStack(Material.BOW);
                bow.addEnchantment(Enchantment.POWER, 3);
                bow.addEnchantment(Enchantment.PUNCH, 1);
                bow.addEnchantment(Enchantment.INFINITY, 1);
                player.getInventory().addItem(bow);
                player.getInventory().addItem(enchant(new ItemStack(Material.IRON_SWORD), Enchantment.SHARPNESS, 1));
                player.getInventory().addItem(new ItemStack(Material.ARROW, 1));
                player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 16));
                player.getInventory().addItem(createSplashPotion(PotionEffectType.SPEED, 1800, 1));
            }
            case BERSERKER -> {
                // Cuero rojo Prot IV
                player.getInventory().setHelmet(coloredLeather(Material.LEATHER_HELMET, Color.RED, 4));
                player.getInventory().setChestplate(coloredLeather(Material.LEATHER_CHESTPLATE, Color.RED, 4));
                player.getInventory().setLeggings(coloredLeather(Material.LEATHER_LEGGINGS, Color.RED, 4));
                player.getInventory().setBoots(coloredLeather(Material.LEATHER_BOOTS, Color.RED, 4));

                ItemStack axe = new ItemStack(Material.NETHERITE_AXE);
                axe.addEnchantment(Enchantment.SHARPNESS, 3);
                player.getInventory().addItem(axe);
                player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 16));
                player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 3));
                player.getInventory().addItem(createSplashPotion(PotionEffectType.STRENGTH, 1200, 0));
                player.getInventory().addItem(createSplashPotion(PotionEffectType.SPEED, 1200, 0));
            }
        }
    }

    private static ItemStack enchant(ItemStack item, Enchantment ench, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(ench, level, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack coloredLeather(Material mat, Color color, int protLevel) {
        ItemStack item = new ItemStack(mat);
        if (item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(color);
            meta.addEnchant(Enchantment.PROTECTION, protLevel, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createSplashPotion(PotionEffectType type, int durationTicks, int amplifier) {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        if (potion.getItemMeta() instanceof PotionMeta meta) {
            meta.addCustomEffect(new PotionEffect(type, durationTicks, amplifier), true);
            meta.setDisplayName("§dElixir de " + type.getName());
            potion.setItemMeta(meta);
        }
        return potion;
    }
}
