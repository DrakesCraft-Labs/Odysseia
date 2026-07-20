package org.metamechanists.odysseia.purchase;

import org.junit.jupiter.api.*;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseServiceTest {
    private File directory;
    private PurchaseRepository repository;
    private FakeRuntime runtime;
    private PurchaseService service;

    @BeforeEach void setUp() throws Exception {
        directory = Files.createTempDirectory("odysseia-purchase-test").toFile();
        repository = new PurchaseRepository(new File(directory, "purchases.db"));
        runtime = new FakeRuntime();
        ProductDefinition product = new ProductDefinition("dragmas_saco", 1, "Saco", "economy", "test", "test", 1,
                VerificationState.VERIFIED_PRODUCTION, List.of(), List.of(
                new ProductAction("money", ActionType.ECONOMY, Map.of("amount", "50000"), false, RefundPolicy.MANUAL_REVIEW, true),
                new ProductAction("kit", ActionType.KIT, Map.of("kit", "test"), true, RefundPolicy.MANUAL_REVIEW, true),
                new ProductAction("announce", ActionType.ANNOUNCEMENT, Map.of(), false, RefundPolicy.NOT_REVOCABLE, true)));
        service = new PurchaseService(new ProductCatalog(List.of(product)), repository, runtime);
    }

    @AfterEach void tearDown() throws Exception { repository.close(); delete(directory); }

    @Test void sameTransactionIsIdempotentAndAnnouncementIsUnique() throws Exception {
        runtime.online = true;
        assertTrue(service.deliver("txn-1", "TestPlayer", "dragmas_saco", false, "test").success());
        assertTrue(service.deliver("txn-1", "TestPlayer", "dragmas_saco", false, "test").success());
        assertEquals(1, runtime.calls(ActionType.ECONOMY));
        assertEquals(1, runtime.calls(ActionType.KIT));
        assertEquals(1, runtime.calls(ActionType.ANNOUNCEMENT));
        assertEquals(PurchaseState.COMPLETED, service.status("txn-1").getFirst().state());
    }

    @Test void tebexDeliveryExecutesRewardsAndQueuesItsAnnouncement() throws Exception {
        runtime.online = true;

        assertTrue(service.deliver("test-tebex-flow", "TestPlayer", "dragmas_saco", false, "CONSOLE_TEST").success());

        assertEquals(1, runtime.calls(ActionType.ECONOMY));
        assertEquals(1, runtime.calls(ActionType.KIT));
        assertEquals(1, runtime.calls(ActionType.ANNOUNCEMENT));
        assertEquals(PurchaseState.COMPLETED, service.status("test-tebex-flow").getFirst().state());
    }

    @Test void differentTransactionsCanPurchaseSameProduct() throws Exception {
        runtime.online = true;
        service.deliver("txn-1", "TestPlayer", "dragmas_saco", false, "test");
        service.deliver("txn-2", "TestPlayer", "dragmas_saco", false, "test");
        assertEquals(2, runtime.calls(ActionType.ECONOMY));
        assertEquals(2, service.status("txn-1").size() + service.status("txn-2").size());
    }

    @Test void offlineActionResumesOnlyWhatWasPending() throws Exception {
        runtime.online = false;
        service.deliver("txn-offline", "TestPlayer", "dragmas_saco", false, "test");
        assertEquals(1, runtime.calls(ActionType.ECONOMY));
        assertEquals(0, runtime.calls(ActionType.KIT));
        assertEquals(PurchaseState.WAITING_FOR_PLAYER, service.status("txn-offline").getFirst().state());
        runtime.online = true;
        service.resumePlayer("TestPlayer", "join");
        assertEquals(1, runtime.calls(ActionType.ECONOMY));
        assertEquals(1, runtime.calls(ActionType.KIT));
        assertEquals(1, runtime.calls(ActionType.ANNOUNCEMENT));
    }

    @Test void retryExecutesOnlyFailedAction() throws Exception {
        runtime.online = true;
        runtime.failKitOnce = true;
        service.deliver("txn-retry", "TestPlayer", "dragmas_saco", false, "test");
        assertEquals(PurchaseState.FAILED_RETRYABLE, service.status("txn-retry").getFirst().state());
        service.retry("txn-retry", "admin");
        assertEquals(1, runtime.calls(ActionType.ECONOMY));
        assertEquals(2, runtime.calls(ActionType.KIT));
        assertEquals(PurchaseState.COMPLETED, service.status("txn-retry").getFirst().state());
    }

    @Test void failedAnnouncementRemainsRecoverableWithoutRepeatingRewards() throws Exception {
        runtime.online = true;
        runtime.failAnnouncementOnce = true;
        service.deliver("txn-announce-retry", "TestPlayer", "dragmas_saco", false, "test");

        long deliveryId = service.status("txn-announce-retry").getFirst().id();
        assertEquals(PurchaseState.FAILED_RETRYABLE, service.status("txn-announce-retry").getFirst().state());
        assertEquals(ActionState.FAILED_RETRYABLE, repository.actions(deliveryId).stream()
                .filter(action -> action.type() == ActionType.ANNOUNCEMENT).findFirst().orElseThrow().state());

        service.retry("txn-announce-retry", "admin");
        assertEquals(1, runtime.calls(ActionType.ECONOMY));
        assertEquals(1, runtime.calls(ActionType.KIT));
        assertEquals(2, runtime.calls(ActionType.ANNOUNCEMENT));
        assertEquals(PurchaseState.COMPLETED, service.status("txn-announce-retry").getFirst().state());
    }

    @Test void concurrentDuplicateEventsStillDeliverOnce() throws Exception {
        runtime.online = true;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> first = executor.submit(() -> service.deliver("txn-race", "TestPlayer", "dragmas_saco", false, "test"));
        Future<?> second = executor.submit(() -> service.deliver("txn-race", "TestPlayer", "dragmas_saco", false, "test"));
        first.get(); second.get(); executor.shutdown();
        assertEquals(1, runtime.calls(ActionType.ECONOMY));
        assertEquals(1, runtime.calls(ActionType.ANNOUNCEMENT));
    }

    @Test void financialEventIsIdempotentAndMarksManualActionsForReview() throws Exception {
        runtime.online = true;
        service.deliver("txn-refund", "TestPlayer", "dragmas_saco", false, "test");
        assertTrue(service.financialEvent("txn-refund", "dragmas_saco", false, "admin").success());
        assertTrue(service.financialEvent("txn-refund", "dragmas_saco", false, "admin").success());
        assertEquals(PurchaseState.REFUNDED, service.status("txn-refund").getFirst().state());
        assertTrue(repository.actions(service.status("txn-refund").getFirst().id()).stream().anyMatch(action -> action.state() == ActionState.FAILED_MANUAL_REVIEW));
    }

    @Test void chargebackUsesTheSameFinancialIdempotencyGuard() throws Exception {
        runtime.online = true;
        service.deliver("txn-chargeback", "TestPlayer", "dragmas_saco", false, "test");
        assertTrue(service.financialEvent("txn-chargeback", "dragmas_saco", true, "admin").success());
        assertTrue(service.financialEvent("txn-chargeback", "dragmas_saco", true, "admin").success());
        assertEquals(PurchaseState.CHARGEBACK, service.status("txn-chargeback").getFirst().state());
    }

    @Test void restartResumesOnlyPendingActions() throws Exception {
        runtime.online = false;
        service.deliver("txn-restart", "TestPlayer", "dragmas_saco", false, "test");
        repository.close();
        repository = new PurchaseRepository(new File(directory, "purchases.db"));
        runtime.online = true;
        ProductDefinition product = service.catalog().get("dragmas_saco");
        service = new PurchaseService(new ProductCatalog(List.of(product)), repository, runtime);
        service.resumePlayer("TestPlayer", "restart");
        assertEquals(1, runtime.calls(ActionType.ECONOMY));
        assertEquals(1, runtime.calls(ActionType.KIT));
        assertEquals(PurchaseState.COMPLETED, service.status("txn-restart").getFirst().state());
    }

    @Test void retryActionDoesNotRepeatCompletedActions() throws Exception {
        runtime.online = true;
        runtime.failKitOnce = true;
        service.deliver("txn-action", "TestPlayer", "dragmas_saco", false, "test");
        long actionId = repository.actions(service.status("txn-action").getFirst().id()).stream()
                .filter(action -> action.type() == ActionType.KIT).findFirst().orElseThrow().id();
        service.retryAction("txn-action", actionId, "admin");
        assertEquals(1, runtime.calls(ActionType.ECONOMY));
        assertEquals(2, runtime.calls(ActionType.KIT));
    }

    @Test void unavailableDependencyRemainsRetryable() throws Exception {
        runtime.online = true;
        runtime.failEconomy = true;
        service.deliver("txn-dependency", "TestPlayer", "dragmas_saco", false, "test");
        assertEquals(PurchaseState.FAILED_RETRYABLE, service.status("txn-dependency").getFirst().state());
        assertEquals(0, runtime.calls(ActionType.ANNOUNCEMENT));
    }

    @Test void optionalOfflineNotificationDoesNotBlockCompletion() throws Exception {
        ProductDefinition product = new ProductDefinition("notify_optional", 2, "Optional Notify", "economy", "test", "test", 1,
                VerificationState.VERIFIED_PRODUCTION, List.of(), List.of(
                new ProductAction("money", ActionType.ECONOMY, Map.of("amount", "1"), false, RefundPolicy.MANUAL_REVIEW, true),
                new ProductAction("notify", ActionType.NOTIFICATION, Map.of("message", "ok"), true, RefundPolicy.NOT_REVOCABLE, false),
                new ProductAction("announce", ActionType.ANNOUNCEMENT, Map.of(), false, RefundPolicy.NOT_REVOCABLE, true)));
        service = new PurchaseService(new ProductCatalog(List.of(product)), repository, runtime);
        runtime.online = false;
        service.deliver("txn-optional-notify", "TestPlayer", "notify_optional", false, "test");
        assertEquals(PurchaseState.COMPLETED, service.status("txn-optional-notify").getFirst().state());
        assertEquals(1, runtime.calls(ActionType.ECONOMY));
        assertEquals(1, runtime.calls(ActionType.ANNOUNCEMENT));
    }

    @Test void invalidInputAndUnknownProductAreRejected() {
        assertFalse(service.deliver("x", "bad name", "missing", false, "test").success());
        assertFalse(service.deliver("txn", "TestPlayer", "missing", false, "test").success());
    }

    @Test void packagedCatalogHasAllAuditedProductsAndNoDuplicateTebexIds() {
        ProductCatalog catalog = new ProductCatalog(new File("src/main/resources/purchases.yml"));
        assertEquals(23, catalog.all().size());
        assertTrue(catalog.validate().isEmpty());
        assertEquals(VerificationState.PARTIALLY_VERIFIED, catalog.get("protection_481").verification());
        assertEquals(ActionType.MANUAL, catalog.get("protection_481").actions().getFirst().type());
        assertEquals("polis", action(catalog, "vip_hermes", "base-rank").parameters().get("group"));
        assertEquals("viphermes", action(catalog, "vip_hermes", "claim").parameters().get("alias"));
        assertEquals("vipzeus", action(catalog, "vip_zeus", "claim").parameters().get("alias"));
        assertEquals("viphefesto", catalog.get("protection_177").actions().getFirst().parameters().get("alias"));
    }

    private ProductAction action(ProductCatalog catalog, String productId, String actionId) {
        return catalog.get(productId).actions().stream()
                .filter(action -> action.id().equals(actionId))
                .findFirst()
                .orElseThrow();
    }

    private static void delete(File file) {
        File[] children = file.listFiles(); if (children != null) for (File child : children) delete(child); file.delete();
    }

    private static final class FakeRuntime implements PurchaseActionRuntime {
        private final Map<ActionType, Integer> counts = new EnumMap<>(ActionType.class);
        private boolean online;
        private boolean failKitOnce;
        private boolean failEconomy;
        private boolean failAnnouncementOnce;
        @Override public UUID resolveUuid(String player) { return UUID.nameUUIDFromBytes(player.getBytes()); }
        @Override public boolean isOnline(String player) { return online; }
        @Override public ActionResult execute(ExecutionContext context, ProductAction action) {
            counts.merge(action.type(), 1, Integer::sum);
            if (action.type() == ActionType.ECONOMY && failEconomy) return ActionResult.retryable("Vault unavailable");
            if (action.requiresOnline() && !online) return ActionResult.waiting("offline");
            if (action.type() == ActionType.KIT && failKitOnce) { failKitOnce = false; return ActionResult.retryable("transient"); }
            if (action.type() == ActionType.ANNOUNCEMENT && failAnnouncementOnce) { failAnnouncementOnce = false; return ActionResult.retryable("webhook unavailable"); }
            return ActionResult.completed(action.id());
        }
        @Override public ActionResult revoke(ExecutionContext context, ProductAction action, String result) { return ActionResult.completed("revoked"); }
        int calls(ActionType type) { return counts.getOrDefault(type, 0); }
    }
}
