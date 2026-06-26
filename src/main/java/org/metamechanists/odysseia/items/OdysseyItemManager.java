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

    // ─── THOR ───────────────────────────────────────────────────────────────

    public static ItemStack createMjolnir() {
        ItemStack item = createBaseItem(
                Material.MACE,
                "&6&l⚡ Mjolnir &r&7[El Martillo del Trueno]",
                "mjolnir",
                "&8▸ Arma Mítica de Thor",
                "&7Forjado por los enanos de Nidavellir",
                "&7en el corazón de una estrella moribunda.",
                "",
                "&e&lFURIA DEL TRUENO &r&7— Al golpear,",
                "&7invoca un rayo sobre el objetivo.",
                "",
                "&6&lENCIERRA EL PODER DEL OLIMPO"
        );
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.SHARPNESS, 10, true);
            meta.addEnchant(Enchantment.FIRE_ASPECT, 4, true);
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ─── ARES ───────────────────────────────────────────────────────────────

    public static ItemStack createAresBlade() {
        ItemStack item = createBaseItem(
                Material.NETHERITE_SWORD,
                "&c&l⚔ Filo de Ares &r&7[Espada de la Guerra]",
                "ares_blade",
                "&8▸ Arma Mítica de Ares",
                "&7Bañada en la sangre de mil batallas.",
                "&7Quien la empuña entra en frenesí de combate.",
                "",
                "&c&lSED DE SANGRE &r&7— Cada víctima derrotada",
                "&7te otorga Fuerza temporal acumulable.",
                "",
                "&4&lCORRUPTA CON LA ESENCIA DE LA GUERRA"
        );
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.SHARPNESS, 12, true);
            meta.addEnchant(Enchantment.FIRE_ASPECT, 3, true);
            meta.addEnchant(Enchantment.LOOTING, 5, true);
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createAresShield() {
        ItemStack item = createBaseItem(
                Material.SHIELD,
                "&c&l🛡 Escudo Espartano de Ares",
                "ares_shield",
                "&8▸ Armadura Mítica de Ares",
                "&7El escudo de los guerreros más feroces.",
                "",
                "&c&lBLOQUEO PERFECTO &r&7— Refleja el 20%",
                "&7del daño bloqueado al atacante."
        );
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ─── HADES ──────────────────────────────────────────────────────────────

    public static ItemStack createHadesScythe() {
        ItemStack item = createBaseItem(
                Material.NETHERITE_HOE,
                "&5&l☠ Guadaña del Inframundo &r&7[Segadora de Almas]",
                "hades_scythe",
                "&8▸ Arma Mítica de Hades",
                "&7La herramienta del cosechador de almas.",
                "&7Cada golpe drena la vida de la víctima.",
                "",
                "&5&lDRENAJE DE ALMA &r&7— Roba 3♥ de vida",
                "&7por golpe y las añade a tu salud.",
                "",
                "&8&lCOSECHADOR DE ALMAS &r&7— Looting X"
        );
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.SHARPNESS, 50, true);
            meta.addEnchant(Enchantment.LOOTING, 10, true);
            meta.addEnchant(Enchantment.KNOCKBACK, 5, true);
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ─── POSEIDÓN ───────────────────────────────────────────────────────────

    public static ItemStack createPoseidonTrident() {
        ItemStack item = createBaseItem(
                Material.TRIDENT,
                "&9&l🔱 Tridente de Poseidón &r&7[Señor del Mar]",
                "poseidon_trident",
                "&8▸ Arma Mítica de Poseidón",
                "&7Forjado en las profundidades del Océano Eterno.",
                "&7Controla las mareas, las tormentas y los mares.",
                "",
                "&9&lTSUNAMI &r&7— Al impactar arrojado, lanza una",
                "&7ola que empuja a todos los enemigos cercanos.",
                "",
                "&b&lRIPTIDE V — IMPALING X — LOYALTY V"
        );
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.IMPALING, 10, true);
            meta.addEnchant(Enchantment.RIPTIDE, 5, true);
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ─── ZEUS ───────────────────────────────────────────────────────────────

    public static ItemStack createZeusMace() {
        ItemStack item = createBaseItem(
                Material.MACE,
                "&e&l⚡ Rayo de Zeus &r&7[Padre de los Dioses]",
                "zeus_mace",
                "&8▸ Arma Mítica de Zeus",
                "&7El rayo definitivo forjado por los Cíclopes",
                "&7en el taller secreto del Olimpo.",
                "",
                "&e&lTORMENTA DIVINA &r&7— Al golpear invoca",
                "&7tres rayos en un radio de 5 bloques.",
                "",
                "&e&lPODER DEL OLIMPO — SHARPNESS XII"
        );
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.SHARPNESS, 12, true);
            meta.addEnchant(Enchantment.KNOCKBACK, 5, true);
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  EXPANSIÓN II — Drops de los nuevos jefes
    // ════════════════════════════════════════════════════════════════════════

    // ─── HEIMDALL ────────────────────────────────────────────────────────────

    public static ItemStack createGjallarhorn() {
        ItemStack item = createBaseItem(Material.MACE,
                "&f&l✦ Gjallarhorn &r&7[Cuerno del Bifröst]",
                "gjallarhorn",
                "&8▸ Arma Mítica de Heimdall",
                "&7El cuerno que anuncia el Ragnarök.",
                "",
                "&f&lECO DEL BIFRÖST &r&7— Golpea con la fuerza",
                "&7de los nueve mundos.",
                "",
                "&6&lSHARPNESS X — KNOCKBACK VIII");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.SHARPNESS, 10, true);
            meta.addEnchant(Enchantment.KNOCKBACK, 8, true);
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createBifrostWings() {
        ItemStack item = createBaseItem(Material.ELYTRA,
                "&f&l✦ Alas del Bifröst",
                "bifrost_wings",
                "&8▸ Armadura Mítica de Heimdall",
                "&7Tejidas con la luz del puente arcoíris.",
                "",
                "&6&lPROTECTION X — UNBREAKING X — MENDING");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.PROTECTION, 10, true);
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ─── HIDRA ───────────────────────────────────────────────────────────────

    public static ItemStack createHydraFang() {
        ItemStack item = createBaseItem(Material.NETHERITE_SWORD,
                "&a&l🐍 Colmillo de la Hidra",
                "hydra_fang",
                "&8▸ Arma Mítica de la Hidra de Lerna",
                "&7Destila el veneno eterno de Lerna.",
                "",
                "&2&lVENENO DE LERNA &r&7— Fire Aspect III",
                "",
                "&6&lSHARPNESS XV — LOOTING VIII");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.SHARPNESS, 15, true);
            meta.addEnchant(Enchantment.FIRE_ASPECT, 3, true);
            meta.addEnchant(Enchantment.LOOTING, 8, true);
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createHydraScale() {
        ItemStack item = createBaseItem(Material.TURTLE_HELMET,
                "&a&l🐍 Escama de la Hidra",
                "hydra_scale",
                "&8▸ Armadura Mítica de la Hidra de Lerna",
                "&7Escama regenerativa e impenetrable.",
                "",
                "&6&lPROTECTION XII — THORNS V");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.PROTECTION, 12, true);
            meta.addEnchant(Enchantment.THORNS, 5, true);
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ─── CERBERO ─────────────────────────────────────────────────────────────

    public static ItemStack createCerberoHide() {
        ItemStack item = createBaseItem(Material.NETHERITE_CHESTPLATE,
                "&5&l🐕 Piel de Cerbero",
                "cerbero_hide",
                "&8▸ Armadura Mítica de Cerbero",
                "&7La coraza del guardián del Inframundo.",
                "",
                "&6&lPROTECTION XV — THORNS VIII — FIRE PROT X");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.PROTECTION, 15, true);
            meta.addEnchant(Enchantment.THORNS, 8, true);
            meta.addEnchant(Enchantment.FIRE_PROTECTION, 10, true);
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ─── ARTEMISA ────────────────────────────────────────────────────────────

    public static ItemStack createArtemisBow() {
        ItemStack item = createBaseItem(Material.BOW,
                "&9&l🌙 Arco Lunar de Artemisa",
                "artemis_bow",
                "&8▸ Arma Mítica de Artemisa",
                "&7Ninguna presa escapa de la cazadora.",
                "",
                "&3&lLUZ DE LUNA &r&7— Power X — Flame — Punch V",
                "",
                "&6&lINFINITY — UNBREAKING X — MENDING");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.POWER, 10, true);
            meta.addEnchant(Enchantment.FLAME, 1, true);
            meta.addEnchant(Enchantment.PUNCH, 5, true);
            meta.addEnchant(Enchantment.INFINITY, 1, true);
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ─── TIFÓN ───────────────────────────────────────────────────────────────

    public static ItemStack createTifonClaw() {
        ItemStack item = createBaseItem(Material.NETHERITE_SWORD,
                "&4&l🌋 Garra de Tifón",
                "tifon_claw",
                "&8▸ Arma Mítica de Tifón",
                "&7La garra del padre de todos los monstruos.",
                "",
                "&c&lFURIA PRIMORDIAL &r&7— Fire Aspect V",
                "",
                "&6&lSHARPNESS XX — LOOTING X");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.SHARPNESS, 20, true);
            meta.addEnchant(Enchantment.FIRE_ASPECT, 5, true);
            meta.addEnchant(Enchantment.LOOTING, 10, true);
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createTifonChestplate() {
        ItemStack item = createBaseItem(Material.NETHERITE_CHESTPLATE,
                "&4&l🌋 Coraza del Padre Monstruo",
                "tifon_chest",
                "&8▸ Armadura Mítica de Tifón",
                "&7Forjada con la piel de mil bestias.",
                "",
                "&6&lPROTECTION XX — THORNS X — FIRE PROT X");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.PROTECTION, 20, true);
            meta.addEnchant(Enchantment.THORNS, 10, true);
            meta.addEnchant(Enchantment.FIRE_PROTECTION, 10, true);
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ─── PROMETEO ────────────────────────────────────────────────────────────

    public static ItemStack createPrometeoFlame() {
        ItemStack item = createBaseItem(Material.BLAZE_ROD,
                "&6&l🔥 Llama Eterna de Prometeo",
                "prometeo_flame",
                "&8▸ Reliquia Mítica de Prometeo",
                "&7El fuego robado a los dioses para los mortales.",
                "",
                "&e&lCHISPA DIVINA &r&7— Reliquia legendaria",
                "&7que arde por toda la eternidad.");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.FIRE_ASPECT, 5, true);
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }
}
