package org.metamechanists.odysseia.boss.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class BossArenaPricingTest {
    @Test
    void resolvesPerBossFeesAndUsesTheConfiguredDefault() {
        YamlConfiguration fees = new YamlConfiguration();
        fees.set("default", 100_000D);
        fees.set("zeus", 50_000D);

        assertEquals(50_000D, BossArenaPricing.feeFor(fees, "ZEUS"));
        assertEquals(100_000D, BossArenaPricing.feeFor(fees, "unknown_boss"));
    }

    @Test
    void neverReturnsNegativeEntryFees() {
        YamlConfiguration fees = new YamlConfiguration();
        fees.set("default", -1D);
        fees.set("hades", -50D);

        assertEquals(0D, BossArenaPricing.feeFor(fees, "hades"));
        assertEquals(0D, BossArenaPricing.feeFor(fees, "zeus"));
    }
}
