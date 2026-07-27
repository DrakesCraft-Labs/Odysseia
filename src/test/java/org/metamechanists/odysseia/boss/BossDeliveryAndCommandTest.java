package org.metamechanists.odysseia.boss;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossDeliveryAndCommandTest {
    @Test
    void customBossLootNeverFallsToTheGround() throws IOException {
        String source = Files.readString(Path.of("src", "main", "java", "org", "metamechanists", "odysseia", "boss", "BossManager.java"));

        assertFalse(source.contains("dropItemNaturally(dropLocation, item.clone())"));
        assertFalse(source.contains("dropItemNaturally(dropLocation, leftover)"));
        assertTrue(source.contains("queuePendingReward(recipient.getUniqueId(), leftover)"));
        assertTrue(source.contains("boss-rewards.yml"));
    }

    @Test
    void commandExposesEndgameBossesAndManualDomains() throws IOException {
        String source = Files.readString(Path.of("src", "main", "java", "org", "metamechanists", "odysseia", "commands", "BossCommand.java"));

        assertTrue(source.contains("wither_storm"));
        assertTrue(source.contains("dragon_ancestral"));
        assertTrue(source.contains("spawnBoss(bossType, player.getLocation(), true)"));
    }

    @Test
    void leviathanUsesBoundedOrbitAndSafeInventoryReturn() throws IOException {
        String source = Files.readString(Path.of("src", "main", "java", "org", "metamechanists", "odysseia", "listeners", "BossItemListener.java"));

        assertTrue(source.contains("startLeviathanOrbit(player, item)"));
        assertTrue(source.contains("tick >= 100"));
        assertTrue(source.contains("victim instanceof Player || victim instanceof ArmorStand"));
        assertTrue(source.contains("pendingLeviathanAxes.put"));
        assertTrue(source.contains("player.getInventory().addItem(pending.item())"));
        assertFalse(source.contains("setItemInMainHand(axeItem)"));
        assertFalse(source.contains("setItemInOffHand(axeItem)"));
    }
}
