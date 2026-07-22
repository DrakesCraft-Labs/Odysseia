package org.metamechanists.odysseia.boss;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BossLocalMessagingTest {
    @Test
    void bossesNeverBroadcastCombatMessagesGlobally() throws IOException {
        Path bossRoot = Path.of("src", "main", "java", "org", "metamechanists", "odysseia", "boss");
        List<Path> offenders;
        try (var files = Files.walk(bossRoot)) {
            offenders = files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> containsGlobalBroadcast(path))
                    .toList();
        }
        assertEquals(List.of(), offenders, "Los bosses deben usar mensajes locales o action bar");
    }

    private boolean containsGlobalBroadcast(Path path) {
        try {
            return Files.readString(path).contains("Bukkit.broadcastMessage");
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo auditar " + path, exception);
        }
    }
}
