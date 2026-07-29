package org.metamechanists.odysseia.boss.arena;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;

/** Immutable session identity; mutable participants live in the arena service. */
public record BossArenaSession(UUID id, UUID bossId, String bossType, Location center,
                               boolean group, Set<UUID> participants, Map<UUID, Location> returnLocations,
                               long createdAt) {
}
