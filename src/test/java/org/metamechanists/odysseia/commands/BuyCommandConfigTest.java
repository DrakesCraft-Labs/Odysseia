package org.metamechanists.odysseia.commands;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * /buy tiene que seguir siendo nuestro y apuntar a la tienda propia.
 *
 * El comando lo registraba el plugin de Tebex y abria un GUI que anunciaba "Duracion: 30 dias"
 * en todos los paquetes, incluidos los Dragmas y las protecciones, que son permanentes.
 */
class BuyCommandConfigTest {

    private static final YamlConfiguration PLUGIN =
            YamlConfiguration.loadConfiguration(new File("src/main/resources/plugin.yml"));
    private static final YamlConfiguration CONFIG =
            YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));

    @Test
    void odysseiaDeclaresBuySoTebexDoesNotWinTheRegistration() {
        assertNotNull(PLUGIN.getConfigurationSection("commands.buy"),
                "plugin.yml debe declarar /buy; los alias de commands.yml pierden contra el plugin de Tebex");
        assertEquals(List.of("comprar", "store", "shop"), PLUGIN.getStringList("commands.buy.aliases"));
    }

    @Test
    void theStoreUrlPointsToOurOwnSiteAndNotToTebex() {
        String url = CONFIG.getString("tienda.url-web", "");
        assertTrue(url.startsWith("https://web.drakescraft.cl/"), "la tienda debe apuntar al dominio propio: " + url);
        assertFalse(url.contains("tebex.io"), "el enlace publico no puede volver a la tienda de Tebex");
    }
}
