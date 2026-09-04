package org.metamechanists.odysseia.reencarnacion;

import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Representa una sesion de solicitud de reencarnacion activa con su codigo efimero
 * y los items seleccionados para la Capsula de Recuerdos.
 */
public final class ReencarnacionSession {

    private final UUID playerUuid;
    private final String playerName;
    private final String code;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final List<ItemStack> capsuleItems;

    public ReencarnacionSession(UUID playerUuid, String playerName, String code, List<ItemStack> capsuleItems) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.code = code;
        this.createdAt = Instant.now();
        this.expiresAt = this.createdAt.plusSeconds(15 * 60); // 15 minutos
        this.capsuleItems = capsuleItems != null ? new ArrayList<>(capsuleItems) : new ArrayList<>();
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getCode() {
        return code;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public List<ItemStack> getCapsuleItems() {
        return capsuleItems;
    }
}
