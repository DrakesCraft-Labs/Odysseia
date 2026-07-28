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
    void bossWarpUsesPaidEntriesAndCanExplainItsPrices() throws IOException {
        String command = Files.readString(Path.of("src", "main", "java", "org", "metamechanists", "odysseia", "commands", "BossWarpCommand.java"));
        String arena = Files.readString(Path.of("src", "main", "java", "org", "metamechanists", "odysseia", "boss", "arena", "BossArenaService.java"));

        assertTrue(command.contains("bosswarp precios"));
        assertTrue(command.contains("Entrada cobrada"));
        assertTrue(arena.contains("withdrawPlayer"));
        assertTrue(arena.contains("Tu entrada fue reembolsada"));
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

    @Test
    void everyCombatFamilyHasAFourthTelegraphedVariantAndPhaseRupture() throws IOException {
        String director = Files.readString(Path.of("src", "main", "java", "org", "metamechanists", "odysseia", "boss", "combat", "BossCombatDirector.java"));
        String boss = Files.readString(Path.of("src", "main", "java", "org", "metamechanists", "odysseia", "boss", "OdysseyBoss.java"));

        assertTrue(director.contains("Math.floorMod(rotation / 2, 4)"));
        assertTrue(director.contains("tempestCage(boss, target)"));
        assertTrue(director.contains("ruptureWave(boss)"));
        assertTrue(director.contains("hunterMark(boss, target)"));
        assertTrue(boss.contains("emitPhaseRupture(loc, phase)"));
        assertTrue(boss.contains("boss-balance.phase-rupture.radius"));
    }
}
