package org.metamechanists.odysseia.commands;

import java.io.File;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UltraGodConfigurationTest {

    @Test
    void titanVariantIsBoundedWithoutBeingBlockedByTheStaffPermission() {
        YamlConfiguration plugin = YamlConfiguration.loadConfiguration(
                new File("src/main/resources/plugin.yml"));
        YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new File("src/main/resources/config.yml"));

        assertFalse(plugin.contains("commands.ultragod.permission"),
                "El permiso global del comando bloquearía la variante de Titan Caos");
        assertEquals(false, plugin.getBoolean("permissions.odysseia.ultragod.titan.default"));
        assertEquals(45, config.getInt("ultragod.titan.duration-seconds"));
        assertEquals(30, config.getInt("ultragod.titan.cooldown-minutes"));
    }
}
