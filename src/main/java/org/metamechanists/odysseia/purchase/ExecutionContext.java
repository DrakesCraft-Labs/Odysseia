package org.metamechanists.odysseia.purchase;

import java.util.UUID;

public record ExecutionContext(long deliveryId, String provider, String transaction, String player, UUID uuid,
                               ProductDefinition product, boolean dryRun, String actor) {}
