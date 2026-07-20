package org.metamechanists.odysseia;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigDefaultsMergeTest {

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
}
