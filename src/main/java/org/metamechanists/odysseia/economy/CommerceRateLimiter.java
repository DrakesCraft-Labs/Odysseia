package org.metamechanists.odysseia.economy;

import org.bukkit.Material;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Limits commercial throughput before inventory state is modified. */
public final class CommerceRateLimiter {
    private final Map<UUID, Window> windows = new HashMap<>();

    public synchronized Decision reserve(UUID playerId, Map<Material, Integer> requested, long now,
                                         long cooldownMillis, long windowMillis, int maxPerMaterial) {
        Window window = windows.computeIfAbsent(playerId, ignored -> new Window(now));
        if (now - window.lastSaleAt < cooldownMillis) return Decision.cooldown(cooldownMillis - (now - window.lastSaleAt));
        if (now - window.windowStartedAt >= windowMillis) window.reset(now);

        Map<Material, Integer> accepted = new EnumMap<>(Material.class);
        for (Map.Entry<Material, Integer> entry : requested.entrySet()) {
            int remaining = Math.max(0, maxPerMaterial - window.sold.getOrDefault(entry.getKey(), 0));
            int amount = Math.min(remaining, entry.getValue());
            if (amount > 0) accepted.put(entry.getKey(), amount);
        }
        if (accepted.isEmpty()) return Decision.quota(windowMillis - (now - window.windowStartedAt));

        accepted.forEach((material, amount) -> window.sold.merge(material, amount, Integer::sum));
        window.lastSaleAt = now;
        return Decision.accepted(accepted);
    }

    private static final class Window {
        private final Map<Material, Integer> sold = new EnumMap<>(Material.class);
        private long windowStartedAt;
        private long lastSaleAt;

        private Window(long now) {
            reset(now);
            lastSaleAt = Long.MIN_VALUE / 2;
        }

        private void reset(long now) {
            sold.clear();
            windowStartedAt = now;
        }
    }

    public record Decision(Map<Material, Integer> accepted, long retryAfterMillis, Reason reason) {
        private static Decision accepted(Map<Material, Integer> accepted) { return new Decision(Map.copyOf(accepted), 0L, Reason.ACCEPTED); }
        private static Decision cooldown(long retryAfterMillis) { return new Decision(Map.of(), retryAfterMillis, Reason.COOLDOWN); }
        private static Decision quota(long retryAfterMillis) { return new Decision(Map.of(), retryAfterMillis, Reason.QUOTA_REACHED); }
    }

    public enum Reason { ACCEPTED, COOLDOWN, QUOTA_REACHED }
}
