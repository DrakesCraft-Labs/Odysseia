package org.metamechanists.odysseia.purchase;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ninguna piedra prometida puede apuntar a un alias que no existe en el mapa.
 *
 * Se agrego junto al arreglo de la entrega: los kits despachaban "/ps give <alias>", que
 * ProtectionStones rechaza porque identifica el bloque por material, y el fallo era silencioso
 * para el comprador.
 */
class ProtectionAliasConsistencyTest {

    private static final YamlConfiguration CONFIG =
            YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));
    private static final YamlConfiguration PURCHASES =
            YamlConfiguration.loadConfiguration(new File("src/main/resources/purchases.yml"));

    private static Set<String> aliasKeys() {
        ConfigurationSection section = CONFIG.getConfigurationSection("protectionstones.aliases");
        return section == null ? Set.of() : section.getKeys(false);
    }

    @Test
    void everyProtectionPromisedByTheStoreHasAnAlias() {
        Set<String> keys = aliasKeys();
        List<String> missing = new ArrayList<>();
        ConfigurationSection products = PURCHASES.getConfigurationSection("products");
        for (String product : products.getKeys(false)) {
            for (Map<?, ?> action : products.getMapList(product + ".actions")) {
                if (!"PROTECTION_STONE".equalsIgnoreCase(String.valueOf(action.get("type")))) continue;
                if (!(action.get("parameters") instanceof Map<?, ?> parameters)) continue;
                String alias = String.valueOf(parameters.get("alias"));
                // El runtime acepta la clave del mapa o el alias literal; falla solo si no es ninguno.
                if (!keys.contains(alias) && !aliasKeys().stream().anyMatch(key -> CONFIG
                        .getString("protectionstones.aliases." + key, "").equals(alias))) {
                    missing.add(product + " -> alias '" + alias + "'");
                }
            }
        }
        assertEquals(List.of(), missing, "purchases.yml promete protecciones sin alias en config.yml");
    }

    @Test
    void everyKitProtectionKeyResolvesToAnAlias() {
        Set<String> keys = aliasKeys();
        ConfigurationSection kits = CONFIG.getConfigurationSection("kits");
        List<String> broken = new ArrayList<>();
        for (String kit : kits.getKeys(false)) {
            String key = CONFIG.getString("kits." + kit + ".protection-alias", "").trim();
            if (key.isEmpty()) continue;
            if (!keys.contains(key)) broken.add(kit + " -> '" + key + "'");
            else if (CONFIG.getString("protectionstones.aliases." + key, "").isBlank()) {
                broken.add(kit + " -> '" + key + "' vacio");
            }
        }
        assertEquals(List.of(), broken, "kits con protection-alias que no resuelve a un alias real");
    }

    @Test
    void deliveryNeverGoesThroughAConsoleCommand() {
        // "/ps give" resuelve por material y cuatro pares de bloques comparten material, asi que
        // volver a un comando de consola reintroduce la entrega de la piedra equivocada.
        assertTrue(CONFIG.getString("protectionstones.give-command") == null,
                "la entrega debe usar la API por alias, no un comando de consola");
    }
}
