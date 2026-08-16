package org.metamechanists.odysseia.kits;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
