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

    public static ItemStack createLokiDagger() {
        ItemStack item = createBaseItem(
                Material.NETHERITE_SWORD,
                "&a&lDaga del Engaño de Loki",
                "loki_dagger",
                "&7Forjada en las sombras de Asgard.",
                "&7Otorga &aCeguera III &7al golpear a tus enemigos.",
                "",
                "&a&lPROPIEDAD MÍTICA",
                "&7- Ceguera temporal en combate.",
                "&7- Daño base elevado."
        );
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.SHARPNESS, 6, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createLokiScepter() {
        ItemStack item = createBaseItem(
                Material.GOLDEN_HOE,
                "&a&lCetro de la Ilusión de Loki",
                "loki_scepter",
                "&7El canalizador de la magia del engaño.",
                "&eClick Derecho &7para disparar un proyectil mágico",
                "&7que ciega y confunde al objetivo.",
                "",
                "&a&lPROPIEDAD MÍTICA",
                "&7- Disparo de magia verde.",
                "&7- Cooldown: 5 segundos."
        );
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createOdinSpear() {
        ItemStack item = createBaseItem(
                Material.TRIDENT,
                "&e&lLanza Gungnir de Odín",
                "odin_spear",
                "&7La lanza sagrada que nunca falla su blanco.",
                "&7¡Siempre invoca un rayo al golpear a un enemigo!",
                "",
                "&e&lPROPIEDAD MÍTICA",
                "&7- Furia del trueno constante.",
                "&7- Lealtad divina."
        );
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.LOYALTY, 5, true);
            meta.addEnchant(Enchantment.IMPALING, 6, true);
            meta.addEnchant(Enchantment.UNBREAKING, 4, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createOdinHelmet() {
        ItemStack item = createBaseItem(
                Material.GOLDEN_HELMET,
                "&e&lYelmo de la Sabiduría de Odín",
                "odin_helmet",
                "&7El casco del mismísimo Padre de Todo.",
                "&7Otorga &eVisión Nocturna &7y &eAbsorción II &7permanentes",
                "&7al llevarlo puesto.",
                "",
                "&e&lPROPIEDAD MÍTICA",
                "&7- Sabiduría y resistencia absoluta."
        );
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.PROTECTION, 5, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createKratosBlade() {
        ItemStack item = createBaseItem(
                Material.NETHERITE_SWORD,
                "&c&lEspada del Caos de Kratos",
                "kratos_blade",
                "&7Espadas encadenadas infundidas con el fuego de las profundidades.",
                "&7Quema a los enemigos y los &catrae hacia ti &7al golpear.",
                "",
                "&c&lPROPIEDAD MÍTICA",
                "&7- Llama constante (Aspecto Ígneo V).",
                "&7- Atracción por cadenas de fuego."
        );
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.SHARPNESS, 7, true);
            meta.addEnchant(Enchantment.FIRE_ASPECT, 5, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createLeviathanAxe() {
        ItemStack item = createBaseItem(
                Material.DIAMOND_AXE,
                "&b&lHacha Leviatán de Kratos",
                "leviathan_axe",
                "&7Forjada por los hermanos Huldra para restaurar el balance.",
                "&7Aplica un &bCongelamiento Profundo (Lentitud V) &7por 3 segundos.",
                "",
                "&b&lPROPIEDAD MÍTICA",
                "&7- Filo rúnico helado.",
                "&7- Lentitud severa al impactar."
        );
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.SHARPNESS, 6, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            item.setItemMeta(meta);
        }
        return item;
    }
}
