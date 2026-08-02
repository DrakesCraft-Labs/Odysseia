package org.metamechanists.odysseia.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CommerceCommandLimiterTest {
    @Test
    void blocksCooldownThenRestrictsRepeatedCommandBursts() {
        CommerceCommandLimiter limiter = new CommerceCommandLimiter();
        UUID player = UUID.randomUUID();

        assertTrue(limiter.reserve(player, 0L, 500L, 10_000L, 2, 60_000L).allowed());
        assertEquals(CommerceCommandLimiter.Reason.COOLDOWN,
                limiter.reserve(player, 100L, 500L, 10_000L, 2, 60_000L).reason());
        assertTrue(limiter.reserve(player, 600L, 500L, 10_000L, 2, 60_000L).allowed());
        assertEquals(CommerceCommandLimiter.Reason.RESTRICTED,
                limiter.reserve(player, 1_200L, 500L, 10_000L, 2, 60_000L).reason());
    }
}
