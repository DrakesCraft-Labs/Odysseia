package org.metamechanists.odysseia.boss.combat;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BossCombatProfileTest {
    private static final Set<String> PRODUCTION_BOSSES = Set.of(
            "circe", "polifemo", "dios_corrupto", "thor", "ares", "hades", "poseidon", "zeus",
            "loki", "odin", "kratos", "heimdall", "hidra", "cerbero", "artemisa", "tifon",
            "prometeo", "coloso_end", "wither_storm", "dragon_ancestral");

    @Test
    void everyProductionBossHasAnExplicitNonEmptyCombatProfile() {
        assertEquals(PRODUCTION_BOSSES, BossCombatProfile.configuredBossIds());
        PRODUCTION_BOSSES.forEach(id -> assertFalse(BossCombatProfile.forBoss(id).families().isEmpty()));
    }

    @Test
    void aliasesAndUnknownBossesRemainSafe() {
        assertEquals(BossCombatProfile.forBoss("coloso_end"), BossCombatProfile.forBoss("coloso-end"));
        assertEquals(3, BossCombatProfile.forBoss("future_boss").families().size());
    }
}
