package org.metamechanists.odysseia.kits;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * La tienda no puede prometer un kit que la configuracion no define.
 *
 * Se agrego tras encontrar que purchases.yml vendia 8 rangos cuyos kits no existian en config.yml:
 * cualquier compra de Titan habria fallado en la accion KIT.
 */
class KitCatalogConsistencyTest {

    @Test
    void everyKitPromisedByTheStoreExists() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));
        YamlConfiguration purchases = YamlConfiguration.loadConfiguration(new File("src/main/resources/purchases.yml"));

        Set<String> defined = config.getConfigurationSection("kits") == null
                ? Set.of()
                : config.getConfigurationSection("kits").getKeys(false);

        List<String> missing = new ArrayList<>();
        var products = purchases.getConfigurationSection("products");
        for (String product : products.getKeys(false)) {
            for (Map<?, ?> action : products.getMapList(product + ".actions")) {
                if (!"KIT".equalsIgnoreCase(String.valueOf(action.get("type")))) continue;
                Object parameters = action.get("parameters");
                if (!(parameters instanceof Map<?, ?> map)) continue;
                String kit = String.valueOf(map.get("kit"));
                if (!defined.contains(kit)) missing.add(product + " -> kit '" + kit + "'");
            }
        }

        assertEquals(List.of(), missing,
                "purchases.yml promete kits que no existen en la seccion kits de config.yml");
    }
}
