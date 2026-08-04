package org.metamechanists.odysseia.economy;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Limits bursty commerce commands independently of the shop implementation.
 * It prevents macro spam while keeping normal manual purchases and sales intact.
 */
public final class CommerceCommandLimiter {
    private final Map<UUID, Window> windows = new HashMap<>();
    private final Map<UUID, Long> restrictedUntil = new HashMap<>();

    public Decision reserve(UUID player, long now, long cooldownMillis,
                            long windowMillis, int maxInWindow, long restrictionMillis) {
        long lockedUntil = restrictedUntil.getOrDefault(player, 0L);
        if (lockedUntil > now) {
            return Decision.restricted(lockedUntil - now);
        }

        Window window = windows.computeIfAbsent(player, ignored -> new Window());
        window.prune(now - windowMillis);
        if (window.lastUse >= 0 && now - window.lastUse < cooldownMillis) {
            return Decision.cooldown(cooldownMillis - (now - window.lastUse));
        }

        if (window.uses.size() >= maxInWindow) {
            restrictedUntil.put(player, now + restrictionMillis);
            return Decision.restricted(restrictionMillis);
        }

        window.uses.addLast(now);
        window.lastUse = now;
        return Decision.permitted();
    }

    public void forget(UUID player) {
        restrictedUntil.remove(player);
        windows.remove(player);
    }

    public enum Reason { ALLOWED, COOLDOWN, RESTRICTED }

    public record Decision(Reason reason, long retryAfterMillis) {
        static Decision permitted() { return new Decision(Reason.ALLOWED, 0L); }
        static Decision cooldown(long retryAfterMillis) { return new Decision(Reason.COOLDOWN, retryAfterMillis); }
        static Decision restricted(long retryAfterMillis) { return new Decision(Reason.RESTRICTED, retryAfterMillis); }
        public boolean allowed() { return reason == Reason.ALLOWED; }
    }

    private static final class Window {
        private final ArrayDeque<Long> uses = new ArrayDeque<>();
        private long lastUse = -1L;

        private void prune(long cutoff) {
            while (!uses.isEmpty() && uses.peekFirst() < cutoff) uses.removeFirst();
        }
    }
}
