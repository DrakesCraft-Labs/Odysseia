package org.metamechanists.odysseia.kits;

import org.bukkit.configuration.file.YamlConfiguration;
import org.metamechanists.odysseia.Odysseia;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/** Persiste y consulta los cooldowns de kits sin mezclar estado de jugadores con config.yml. */
public final class KitClaimService {
    private final Odysseia plugin;
    private final File file;
    private final YamlConfiguration data;

    public KitClaimService(Odysseia plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kit-claims.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public ClaimState state(UUID playerId, String kit, String cooldown) {
        long lastClaim = data.getLong(playerId + "." + kit, 0L);
        long duration = parseDuration(cooldown);
        if (lastClaim == 0L) return ClaimState.availableNow();
        if (duration < 0L) return ClaimState.claimedForever();
        long remaining = lastClaim + duration - System.currentTimeMillis();
        return remaining > 0L ? ClaimState.cooldown(remaining) : ClaimState.availableNow();
    }

    public void record(UUID playerId, String kit) {
        data.set(playerId + "." + kit, System.currentTimeMillis());
        try {
            data.save(file);
        } catch (IOException error) {
            plugin.getLogger().severe("[Kits] No se pudo guardar kit-claims.yml: " + error.getMessage());
        }
    }

    public static long parseDuration(String value) {
        if (value == null || value.equals("-1")) return -1L;
        try {
            long amount = Long.parseLong(value.substring(0, value.length() - 1));
            return switch (value.charAt(value.length() - 1)) {
                case 'm' -> amount * 60_000L;
                case 'h' -> amount * 3_600_000L;
                case 'd' -> amount * 86_400_000L;
                default -> 30L * 86_400_000L;
            };
        } catch (RuntimeException ignored) {
            return 30L * 86_400_000L;
        }
    }

    public record ClaimState(boolean available, long remainingMillis) {
        static ClaimState availableNow() { return new ClaimState(true, 0L); }
        static ClaimState cooldown(long remaining) { return new ClaimState(false, remaining); }
        static ClaimState claimedForever() { return new ClaimState(false, -1L); }

        public String remainingText() {
            if (remainingMillis < 0L) return "sin nuevo reclamo";
            long days = remainingMillis / 86_400_000L;
            long hours = (remainingMillis % 86_400_000L) / 3_600_000L;
            long minutes = (remainingMillis % 3_600_000L) / 60_000L;
            return days + "d " + hours + "h " + minutes + "m";
        }
    }
}
