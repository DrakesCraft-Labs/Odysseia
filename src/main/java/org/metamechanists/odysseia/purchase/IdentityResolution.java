package org.metamechanists.odysseia.purchase;

import java.util.List;
import java.util.UUID;

public record IdentityResolution(IdentityResolutionStatus status, UUID uuid, String canonicalName,
                                 String platform, String confidence, String source, String detail,
                                 List<UUID> candidates) {
    public boolean resolved() { return status == IdentityResolutionStatus.RESOLVED; }
    static IdentityResolution resolved(PurchaseRepository.PlayerIdentity identity, String source) {
        return new IdentityResolution(IdentityResolutionStatus.RESOLVED, identity.uuid(), identity.canonicalName(),
                identity.platform(), identity.confidence(), source, "Identidad verificada", List.of(identity.uuid()));
    }
    static IdentityResolution result(IdentityResolutionStatus status, String detail, List<UUID> candidates) {
        return new IdentityResolution(status, null, null, "UNKNOWN", "NONE", "RESOLVER", detail, candidates);
    }
}
