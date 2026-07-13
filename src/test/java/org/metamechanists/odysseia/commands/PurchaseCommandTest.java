package org.metamechanists.odysseia.commands;

import org.junit.jupiter.api.Test;
import org.metamechanists.odysseia.purchase.ActionType;
import org.metamechanists.odysseia.purchase.ProductCatalog;
import org.metamechanists.odysseia.purchase.ProductAction;
import org.metamechanists.odysseia.purchase.ProductDefinition;
import org.metamechanists.odysseia.purchase.RefundPolicy;
import org.metamechanists.odysseia.purchase.VerificationState;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PurchaseCommandTest {
    private final ProductCatalog catalog = new ProductCatalog(List.of(new ProductDefinition(
            "vip_hermes", 7510349, "Hermes", "ranks", "test", "test", 1,
            VerificationState.VERIFIED_PRODUCTION, List.of(), List.of(new ProductAction(
                    "notify", ActionType.NOTIFICATION, Map.of(), false, RefundPolicy.NOT_REVOCABLE, true)))));

    @Test void acceptsTheCanonicalTransactionPlayerProductOrder() {
        PurchaseCommand.DeliveryArguments arguments = PurchaseCommand.DeliveryArguments.from(
                catalog, "tbx-transaction", "Mr_Em1lio", "vip_hermes");

        assertEquals("tbx-transaction", arguments.transaction());
        assertEquals("Mr_Em1lio", arguments.player());
        assertEquals("vip_hermes", arguments.productId());
    }

    @Test void acceptsTheLegacyTebexPlayerProductTransactionOrder() {
        PurchaseCommand.DeliveryArguments arguments = PurchaseCommand.DeliveryArguments.from(
                catalog, "Mr_Em1lio", "vip_hermes", "tbx-95619326a46079-ea0e03");

        assertEquals("tbx-95619326a46079-ea0e03", arguments.transaction());
        assertEquals("Mr_Em1lio", arguments.player());
        assertEquals("vip_hermes", arguments.productId());
    }

    @Test void rejectsArgumentsWithoutAKnownProductId() {
        assertThrows(IllegalArgumentException.class, () -> PurchaseCommand.DeliveryArguments.from(
                catalog, "tbx-transaction", "Mr_Em1lio", "unknown_product"));
    }
}
