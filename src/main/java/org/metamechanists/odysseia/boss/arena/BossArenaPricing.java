package org.metamechanists.odysseia.boss.arena;

import java.util.Locale;
import org.bukkit.configuration.ConfigurationSection;

/** Resolves configurable, non-negative entry fees without coupling policy to Vault. */
final class BossArenaPricing {
    private BossArenaPricing() {
    }

    static double feeFor(ConfigurationSection fees, String bossType) {
        if (fees == null) {
            return 0.0D;
        }
        String normalized = bossType.toLowerCase(Locale.ROOT);
        return Math.max(0.0D, fees.getDouble(normalized, fees.getDouble("default", 0.0D)));
    }
}
