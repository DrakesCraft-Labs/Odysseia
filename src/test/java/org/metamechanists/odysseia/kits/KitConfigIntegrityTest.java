package org.metamechanists.odysseia.kits;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Evita que las entregas pagadas dependan de claves YAML ambiguas.
 * Bukkit conserva una sola ocurrencia de una clave repetida, ocultando el error.
 */
class KitConfigIntegrityTest {

    @Test
    void enchantmentBlocksDoNotContainDuplicateKeys() throws IOException {
        List<String> duplicates = new ArrayList<>();
        List<String> lines = Files.readAllLines(Path.of("src/main/resources/config.yml"));
        Set<String> seen = new HashSet<>();
        int enchantmentIndent = -1;

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            String trimmed = line.strip();
            int indent = line.length() - line.stripLeading().length();

            if (trimmed.equals("enchantments:")) {
                enchantmentIndent = indent;
                seen.clear();
                continue;
            }
            if (enchantmentIndent < 0 || trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            if (indent <= enchantmentIndent) {
                enchantmentIndent = -1;
                continue;
            }
            if (!trimmed.contains(":")) continue;

            String key = trimmed.substring(0, trimmed.indexOf(':')).trim();
            if (!seen.add(key)) duplicates.add("linea " + (index + 1) + ": " + key);
        }

        assertEquals(List.of(), duplicates,
                "Cada bloque enchantments debe declarar una clave una sola vez");
    }

    @Test
    void titanCaosDistributesItsSetBonusesAcrossEveryRelevantItem() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new File("src/main/resources/config.yml"));
        List<java.util.Map<?, ?>> items = config.getMapList("kits.titancaos.vanilla-items");

        Set<String> armor = Set.of(
                "NETHERITE_HELMET", "NETHERITE_CHESTPLATE",
                "NETHERITE_LEGGINGS", "NETHERITE_BOOTS");
        Set<String> weapons = Set.of("NETHERITE_SWORD", "NETHERITE_AXE", "MACE");
        int armorPieces = 0;
        int weaponsFound = 0;

        for (java.util.Map<?, ?> item : items) {
            String material = String.valueOf(item.get("material"));
            Object enchantmentsValue = item.get("enchantments");

            if (armor.contains(material)) {
                assertNotNull(enchantmentsValue, material + " debe declarar encantamientos");
                java.util.Map<?, ?> enchantments = (java.util.Map<?, ?>) enchantmentsValue;
                armorPieces++;
                assertEquals("amethyst", item.get("trim-material"));
                assertEquals("silence", item.get("trim-pattern"));
                for (String enchantment : List.of(
                        "hardened", "fire_shield", "ice_shield",
                        "elemental_protection", "regrowth", "temper")) {
                    assertTrue(enchantments.containsKey(enchantment),
                            material + " no recibió " + enchantment);
                }
            }
            if (weapons.contains(material)) {
                assertNotNull(enchantmentsValue, material + " debe declarar encantamientos");
                java.util.Map<?, ?> enchantments = (java.util.Map<?, ?>) enchantmentsValue;
                weaponsFound++;
                for (String enchantment : List.of(
                        "vampire", "thunder", "infernus", "paralyze",
                        "wither", "decapitator", "rage")) {
                    assertTrue(enchantments.containsKey(enchantment),
                            material + " no recibió " + enchantment);
                }
            }
        }

        assertEquals(4, armorPieces, "Titan Caos debe entregar cuatro piezas de armadura");
        assertEquals(3, weaponsFound, "Titan Caos debe repartir su perfil entre espada, hacha y mazo");
    }
}
