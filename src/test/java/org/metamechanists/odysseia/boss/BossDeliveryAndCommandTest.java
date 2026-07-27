package org.metamechanists.odysseia.boss;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossDeliveryAndCommandTest {
    @Test
    void customBossLootUsesOneDeliveryPath() throws IOException {
        String source = Files.readString(Path.of("src", "main", "java", "org", "metamechanists", "odysseia", "boss", "BossManager.java"));

        assertFalse(source.contains("dropItemNaturally(dropLocation, item.clone())"));
        assertTrue(source.indexOf("recipient.getInventory().addItem(item)")
                        < source.indexOf("dropItemNaturally(dropLocation, leftover)"),
                "El suelo debe ser solo el respaldo cuando el inventario no tiene espacio");
    }

    @Test
    void commandExposesEndgameBossesAndManualDomains() throws IOException {
        String source = Files.readString(Path.of("src", "main", "java", "org", "metamechanists", "odysseia", "commands", "BossCommand.java"));

        assertTrue(source.contains("wither_storm"));
        assertTrue(source.contains("dragon_ancestral"));
        assertTrue(source.contains("spawnBoss(bossType, player.getLocation(), true)"));
    }
}
