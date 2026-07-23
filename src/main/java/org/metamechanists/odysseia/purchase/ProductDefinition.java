package org.metamechanists.odysseia.purchase;

import java.util.List;

public record ProductDefinition(
        String id,
        int tebexPackageId,
        String name,
        String category,
        String description,
        String source,
        int version,
        VerificationState verification,
        List<String> dependencies,
        List<ProductAction> actions
) {}
