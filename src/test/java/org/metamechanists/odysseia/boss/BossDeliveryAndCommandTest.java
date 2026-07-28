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
        assertTrue(arena.contains("spawnBoss(type, center.clone()"));
        assertTrue(arena.indexOf("spawnBoss(type, center.clone()") < arena.indexOf("chargeEntry(type, players)"));
        assertTrue(arena.contains("rollbackSpawn(boss, players, cell, center, charge)"));
        assertTrue(command.contains("/bosswarp staff <jefe> <jugador>"));
        assertTrue(command.contains("startForced"));
        assertTrue(command.contains("ARENA_BOSS_TYPES"));
        assertTrue(command.contains("Circe, Polifemo, Dios Corrupto"));
        assertTrue(command.contains("Heimdall, Hidra, Cerbero"));
        assertTrue(arena.contains("Entrada cobrada:"));
        assertTrue(arena.contains("enforceArenaBounds"));
        assertTrue(arena.contains("Contención aplicada"));
    }

    @Test
    void jaxAcceptsTheSpanishFriendlyAjaxAlias() throws IOException {
        String manager = Files.readString(Path.of("src", "main", "java", "org", "metamechanists", "odysseia", "boss", "BossManager.java"));
        String command = Files.readString(Path.of("src", "main", "java", "org", "metamechanists", "odysseia", "commands", "BossCommand.java"));
        String bossWarp = Files.readString(Path.of("src", "main", "java", "org", "metamechanists", "odysseia", "commands", "BossWarpCommand.java"));

        assertTrue(manager.contains("case \"jax\", \"ajax\""));
        assertTrue(manager.contains("type.equalsIgnoreCase(\"ajax\")"));
        assertTrue(command.contains("type.equals(\"jax\") || type.equals(\"ajax\")"));
        assertTrue(bossWarp.contains("\"jax\""));
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
