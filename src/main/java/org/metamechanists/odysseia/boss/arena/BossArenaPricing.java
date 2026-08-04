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
        String normalized = canonicalId(bossType);
        return Math.max(0.0D, fees.getDouble(normalized, fees.getDouble("default", 0.0D)));
    }
    /** Keeps public aliases tied to the same advertised entry fee. */
    private static String canonicalId(String bossType) {
        String normalized = bossType == null ? "" : bossType.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "tifón" -> "tifon";
            case "dragon_ancestral", "dragon-ancestral" -> "dragon";
            case "wither", "wither-storm", "witherstorm" -> "wither_storm";
            default -> normalized;
        };
    }
}
