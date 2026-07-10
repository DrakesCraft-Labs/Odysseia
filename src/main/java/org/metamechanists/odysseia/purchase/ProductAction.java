package org.metamechanists.odysseia.purchase;

import java.util.Map;

public record ProductAction(
        String id,
        ActionType type,
        Map<String, String> parameters,
        boolean requiresOnline,
        RefundPolicy refundPolicy,
        boolean required
) {}
