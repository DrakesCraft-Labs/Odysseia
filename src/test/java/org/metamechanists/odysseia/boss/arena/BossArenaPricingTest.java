package org.metamechanists.odysseia.boss.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class BossArenaPricingTest {
    @Test
    void resolvesPerBossFeesAndUsesTheConfiguredDefault() {
        YamlConfiguration fees = new YamlConfiguration();
        fees.set("default", 500_000D);
        fees.set("zeus", 250_000D);

        assertEquals(250_000D, BossArenaPricing.feeFor(fees, "ZEUS"));
        assertEquals(500_000D, BossArenaPricing.feeFor(fees, "unknown_boss"));
    }

    @Test
    void neverReturnsNegativeEntryFees() {
        YamlConfiguration fees = new YamlConfiguration();
        fees.set("default", -1D);
        fees.set("hades", -50D);

        assertEquals(0D, BossArenaPricing.feeFor(fees, "hades"));
        assertEquals(0D, BossArenaPricing.feeFor(fees, "zeus"));
    }

    @Test
    void publicBossAliasesKeepTheirConfiguredPrice() {
        YamlConfiguration fees = new YamlConfiguration();
        fees.set("tifon", 1_500_000D);
        fees.set("dragon", 2_500_000D);
        fees.set("wither_storm", 5_000_000D);

        assertEquals(1_500_000D, BossArenaPricing.feeFor(fees, "tifón"));
        assertEquals(2_500_000D, BossArenaPricing.feeFor(fees, "dragon_ancestral"));
        assertEquals(5_000_000D, BossArenaPricing.feeFor(fees, "wither"));
    }
}
