package org.metamechanists.odysseia.listeners;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El borde de proteccion tiene que poder apagarse.
 *
 * Dos jugadores preguntaron el mismo dia como quitar "los bloques de cristal que delimitan la
 * proteccion". No habia forma: el borde son bloques fantasma enviados solo a su cliente, asi que
 * ni romperlos servia, y solo desaparecian al salir de la region.
 */
class ProtectionBorderCommandTest {

    private static final YamlConfiguration PLUGIN =
            YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));
    private static final YamlConfiguration CONFIG =
            YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));

    @Test
    void theToggleCommandIsDeclared() {
        assertNotNull(PLUGIN.getConfigurationSection("commands.borde"),
                "sin /borde el jugador no tiene forma de ocultar el cristal");
        assertEquals(List.of("bordes", "border", "limite"), PLUGIN.getStringList("commands.borde.aliases"));
    }

    @Test
    void theBorderStaysEnabledByDefault() {
        // Apagarlo para todos seria peor: el borde evita que la gente construya fuera de su terreno.
        assertTrue(CONFIG.getBoolean("protection-border.enabled"));
        assertEquals("LIGHT_BLUE_STAINED_GLASS", CONFIG.getString("protection-border.material"));
    }

    @Test
    void theCommandDoesNotCollideWithAnotherOne() {
        var comandos = PLUGIN.getConfigurationSection("commands").getKeys(false);
        for (String comando : comandos) {
            if (comando.equals("borde")) continue;
            for (String alias : PLUGIN.getStringList("commands." + comando + ".aliases")) {
                assertTrue(!List.of("borde", "bordes", "border", "limite").contains(alias),
                        "el alias '" + alias + "' de /" + comando + " choca con /borde");
            }
        }
    }
}
