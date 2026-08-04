package org.metamechanists.odysseia.economy;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommerceRateLimiterTest {
    @Test
    void enforcesCooldownBeforeRecordingAnotherSale() {
        CommerceRateLimiter limiter = new CommerceRateLimiter();
        UUID player = UUID.randomUUID();
        assertEquals(CommerceRateLimiter.Reason.ACCEPTED,
                limiter.reserve(player, Map.of(Material.WHEAT, 64), 1_000L, 20_000L, 3_600_000L, 100).reason());
        assertEquals(CommerceRateLimiter.Reason.COOLDOWN,
                limiter.reserve(player, Map.of(Material.WHEAT, 1), 2_000L, 20_000L, 3_600_000L, 100).reason());
    }

    @Test
    void leavesExcessItemsUntouchedAfterMaterialQuota() {
        CommerceRateLimiter limiter = new CommerceRateLimiter();
        UUID player = UUID.randomUUID();
        limiter.reserve(player, Map.of(Material.WHEAT, 80), 1_000L, 0L, 3_600_000L, 100);
        CommerceRateLimiter.Decision decision = limiter.reserve(player, Map.of(Material.WHEAT, 80), 2_000L, 0L, 3_600_000L, 100);
        assertEquals(CommerceRateLimiter.Reason.ACCEPTED, decision.reason());
        assertEquals(20, decision.accepted().get(Material.WHEAT));
    }
}
