package org.metamechanists.odysseia.purchase;

import java.util.UUID;

public interface PurchaseActionRuntime {
    UUID resolveUuid(String player);
    boolean isOnline(String player);
    ActionResult execute(ExecutionContext context, ProductAction action);
    ActionResult revoke(ExecutionContext context, ProductAction action, String previousResult);
}
