package org.metamechanists.odysseia;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigDefaultsMergeTest {

    @Test
    void productionConfigContainsNativeAutomationAndWelcomeBook() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.load(new File("src/main/resources/config.yml"));

        assertTrue(config.getBoolean("automation-guard.enabled"));
        assertEquals(40, config.getInt("automation-guard.redstone.fast-pulse-limit"));
        assertEquals(180, config.getInt("automation-guard.redstone.long-pulse-limit"));
        assertEquals(3, config.getInt("automation-guard.redstone.violations-before-break"));
        assertTrue(config.getBoolean("translation.enabled"));
        assertEquals("inicial", config.getString("starter-kit.kit"));
        assertEquals(400L, config.getLong("starter-kit.delay-ticks"));
        assertTrue(config.getConfigurationSection("starter-kit.items") == null);
        assertTrue(config.getStringList("starter-kit.commands").isEmpty());
        assertTrue(config.getBoolean("discord-translator.translate-discord-to-mc"));
        assertTrue(!config.getBoolean("discord-translator.translate-mc-to-discord"));
        assertEquals(6, config.getMapList("kits.inicial.vanilla-items").stream()
                .filter(item -> "WRITTEN_BOOK".equals(item.get("material")))
                .map(item -> ((java.util.List<?>) item.get("pages")).size())
                .findFirst()
                .orElse(0));
    }

    @Test
    void fillsExplicitlyEmptySectionsWithoutReplacingProductionValues() throws Exception {
        YamlConfiguration current = new YamlConfiguration();
        current.loadFromString("kits:\n  hermes: {}\nnative-menus:\n  shop:\n    title: Produccion\n    entries: {}\n");

        YamlConfiguration defaults = new YamlConfiguration();
        defaults.loadFromString("kits:\n  hermes:\n    permission: drakes.kit.hermes\nnative-menus:\n  shop:\n    title: Default\n    entries:\n      slimefun:\n        slot: 40\n");

        assertTrue(Odysseia.mergeMissingConfig(current, defaults));
        assertEquals("drakes.kit.hermes", current.getString("kits.hermes.permission"));
        assertEquals(40, current.getInt("native-menus.shop.entries.slimefun.slot"));
        assertEquals("Produccion", current.getString("native-menus.shop.title"));
    }

    @Test
    void migratesOnlyKnownUnsafeAntigravityDefaults() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString("""
                starter-kit:
                  delay-ticks: 100
                  commands: [ps give novato player 1]
                  items: [{material: STONE_SWORD}]
                translation:
                  join-delay-ticks: 60
                discord-translator:
                  translate-mc-to-discord: true
                automation-guard:
                  redstone:
                    fast-pulse-limit: 12
                    long-window-seconds: 600
                    long-pulse-limit: 8
                """);

        assertTrue(Odysseia.migrateUnsafeLegacyDefaults(config));
        assertEquals(400L, config.getLong("starter-kit.delay-ticks"));
        assertTrue(config.getConfigurationSection("starter-kit.items") == null);
        assertTrue(config.getStringList("starter-kit.commands").isEmpty());
        assertEquals(400L, config.getLong("translation.join-delay-ticks"));
        assertTrue(!config.getBoolean("discord-translator.translate-mc-to-discord"));
        assertEquals(40, config.getInt("automation-guard.redstone.fast-pulse-limit"));
        assertEquals(120, config.getInt("automation-guard.redstone.long-window-seconds"));
        assertEquals(180, config.getInt("automation-guard.redstone.long-pulse-limit"));
    }
}
