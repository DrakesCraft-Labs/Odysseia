package org.metamechanists.odysseia.items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.metamechanists.odysseia.Odysseia;

import java.util.ArrayList;
import java.util.List;

public final class OdysseyItemManager {

    public static final NamespacedKey ITEM_KEY = new NamespacedKey(Odysseia.getInstance(), "odyssey_item_type");

    private OdysseyItemManager() {}

    private static ItemStack createBaseItem(Material material, String name, String typeId, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(ITEM_KEY, PersistentDataType.STRING, typeId);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ─── LOKI ───────────────────────────────────────────────────────────────

    public static ItemStack createLokiDagger() {
        ItemStack item = createBaseItem(
                Material.NETHERITE_SWORD,
                "&a&l🗡 Daga del Engaño de Loki",
                "loki_dagger",
                "&8▸ Arma Mítica de Loki",
                "&7Forjada en las sombras de Asgard con metal",
                "&7robado de los elfos oscuros de Svartalfheim.",
                "",
                "&a&lENGAÑO MORTAL &r&7— Al golpear aplica",
                "&7Ceguera III por 4 segundos.",
                "",
                "&a&lSOMBRA DE LOKI &r&7— 20% de chance de",
                "&7volverte invisible por 3 segundos."
        );
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.SHARPNESS, 10, true);
            meta.addEnchant(Enchantment.LOOTING, 5, true);
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createLokiScepter() {
        ItemStack item = createBaseItem(
                Material.GOLDEN_HOE,
                "&a&l✦ Cetro de la Ilusión de Loki",
                "loki_scepter",
                "&8▸ Arma Mítica de Loki",
                "&7El canalizador de la magia del engaño.",
                "&7Tejido con hechizos de los Jotuns.",
                "",
                "&2&lPROYECTIL MÁGICO &r&7— Click Derecho",
                "&7para disparar un orbe que ciega y",
                "&7confunde al objetivo por 5 segundos.",
                "",
                "&a&lCOOLDOWN: 5s &r&7| &a&lALCANCE: 20 bloques"
        );
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.POWER, 8, true);
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ─── ODÍN ───────────────────────────────────────────────────────────────

    public static ItemStack createOdinSpear() {
        ItemStack item = createBaseItem(
                Material.TRIDENT,
                "&e&l✦ Lanza Gungnir de Odín",
                "odin_spear",
                "&8▸ Arma Mítica de Odín",
                "&7La lanza sagrada del Padre de Todo.",
                "&7Forjada por los enanos, nunca falla su blanco.",
                "",
                "&e&lFURIA DEL TRUENO &r&7— Cada lanzamiento",
                "&7invoca un rayo sobre el objetivo al impactar.",
                "",
                "&6&lIMPALING X — LOYALTY V — UNBREAKING X"
        );
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.LOYALTY, 5, true);
            meta.addEnchant(Enchantment.IMPALING, 10, true);
            meta.addEnchant(Enchantment.CHANNELING, 1, true);
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createOdinHelmet() {
        ItemStack item = createBaseItem(
                Material.GOLDEN_HELMET,
                "&e&l👁 Yelmo de la Sabiduría de Odín",
                "odin_helmet",
                "&8▸ Armadura Mítica de Odín",
                "&7El casco del mismísimo Padre de Todo.",
                "&7Otorga la visión de los cuervos Huginn y Muninn.",
                "",
                "&e&lOJO DE ODÍN &r&7— Visión Nocturna",
                "&7y Absorción III permanentes al llevarlo.",
                "",
                "&6&lPROTECTION X — UNBREAKING X"
        );
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.PROTECTION, 10, true);
            meta.addEnchant(Enchantment.RESPIRATION, 5, true);
            meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ─── KRATOS ─────────────────────────────────────────────────────────────

    public static ItemStack createKratosBlade() {
        ItemStack item = createBaseItem(
                Material.NETHERITE_SWORD,
                "&c&l🔥 Espadas del Caos &r&7[Kratos]",
                "kratos_blade",
                "&8▸ Arma Mítica de Kratos",
                "&7Las Espadas del Caos — cadenas encadenadas al alma",
                "&7del Fantasma de Esparta, infundidas con fuego del",
                "&7Inframundo griego. Creadas por Ares para Kratos.",
                "",
                "&c&lCADENAS ÍGNEAS &r&7— Al golpear, atrae",
                "&7al enemigo 3 bloques hacia ti y le aplica",
                "&7Fuego por 5 segundos.",
                "",
                "&4&lFURIA DEL CAOS &r&7— +30% de daño",
                "&7cuando tienes menos de 6♥ de vida.",
                "",
                "&6&lFIRE ASPECT V — SHARPNESS XII — LOOTING V"
        );
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.SHARPNESS, 12, true);
            meta.addEnchant(Enchantment.FIRE_ASPECT, 5, true);
            meta.addEnchant(Enchantment.LOOTING, 5, true);
            meta.addEnchant(Enchantment.KNOCKBACK, 3, true);
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createLeviathanAxe() {
        ItemStack item = createBaseItem(
                Material.NETHERITE_AXE,
                "&b&l❄ Hacha Leviatán &r&7[Kratos]",
                "leviathan_axe",
                "&8▸ Arma Mítica de Kratos",
                "&7Forjada por los hermanos enanos Brok y Sindri",
                "&7para restaurar el balance entre el hielo y el fuego.",
                "&7Imbuid con la esencia de Fimbulwinter.",
                "",
                "&b&lCONGELAMIENTO RÚNICO &r&7— Al golpear aplica",
                "&7Lentitud V + Debilitación III por 4 segundos.",
                "",
                "&3&lRECLAMACIÓN &r&7— Al lanzar el hacha, regresa",
                "&7a tu mano destruyendo a los enemigos en su camino.",
                "",
                "&b&lSHARPNESS X — EFFICIENCY VIII — LOOTING V"
        );
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.SHARPNESS, 10, true);
            meta.addEnchant(Enchantment.EFFICIENCY, 8, true);
            meta.addEnchant(Enchantment.LOOTING, 5, true);
            meta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }
}
