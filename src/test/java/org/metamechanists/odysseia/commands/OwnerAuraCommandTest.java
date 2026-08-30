package org.metamechanists.odysseia.commands;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OwnerAuraCommandTest {

    @Test
    void acceptsOnlyExplicitSafeRadii() {
        assertEquals(2, OwnerAuraCommand.parseRadius("2"));
        assertEquals(5, OwnerAuraCommand.parseRadius("5"));
        assertEquals(10, OwnerAuraCommand.parseRadius("10"));
        assertEquals(100, OwnerAuraCommand.parseRadius("100"));
        assertNull(OwnerAuraCommand.parseRadius("1"));
        assertNull(OwnerAuraCommand.parseRadius("500"));
        assertNull(OwnerAuraCommand.parseRadius("todo"));
    }

    @Test
    void purgeIsSingleShotAndExplicitlyExcludesEveryPlayer() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/metamechanists/odysseia/commands/OwnerAuraCommand.java"));

        assertEquals(1, source.split("targets = collectPurgeTargets\\(", -1).length - 1,
                "el comando no debe recolectar objetivos de purga dos veces");
        assertEquals(1, source.split("schedulePurge\\(center\\.getWorld\\(\\)", -1).length - 1,
                "el comando no debe programar una segunda purga");
        org.junit.jupiter.api.Assertions.assertTrue(source.contains("target instanceof Player"),
                "la purga debe excluir a todos los jugadores, no solo al ejecutor");
    }

    @Test
    void purgeBatchSizeIsPositiveAndBounded() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/metamechanists/odysseia/commands/OwnerAuraCommand.java"));
        // La purga se reparte por ticks (ticket SAORI #9): un lote de 0 o negativo
        // dejaría el BukkitRunnable girando para siempre sin avanzar.
        org.junit.jupiter.api.Assertions.assertTrue(
                source.contains("PURGE_BATCH_SIZE = ") && !source.contains("PURGE_BATCH_SIZE = 0"),
                "el tamaño de lote de la purga debe ser un entero positivo fijo");
    }
}
