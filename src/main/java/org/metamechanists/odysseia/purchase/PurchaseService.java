package org.metamechanists.odysseia.purchase;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Coordina entregas, reintentos y eventos financieros sin duplicar acciones. */
public final class PurchaseService {
    private static final String PROVIDER = "TEBEX";
    private final ProductCatalog catalog;
    private final PurchaseRepository repository;
    private final PurchaseActionRuntime runtime;
    private final PlayerIdentityResolver identities;
    private final Set<Long> processing = ConcurrentHashMap.newKeySet();
    private volatile Consumer<Telemetry> telemetry = ignored -> { };

    public PurchaseService(ProductCatalog catalog, PurchaseRepository repository, PurchaseActionRuntime runtime) {
        this(catalog, repository, runtime, PlayerIdentityResolver.legacy(runtime));
    }

    public PurchaseService(ProductCatalog catalog, PurchaseRepository repository, PurchaseActionRuntime runtime, PlayerIdentityResolver identities) {
        this.catalog = catalog; this.repository = repository; this.runtime = runtime; this.identities = identities;
    }

    /** Telemetry is best-effort and never participates in a purchase transaction. */
    public void setTelemetry(Consumer<Telemetry> telemetry) {
        this.telemetry = telemetry == null ? ignored -> { } : telemetry;
    }

    public Result deliver(String transaction, String player, String productId, boolean dryRun, String actor) {
        if (!validTransaction(transaction)) return Result.error("Transacción inválida.");
        ProductDefinition product = catalog.get(productId);
        if (product == null) return Result.error("Producto inexistente: " + productId);
        if (dryRun) return Result.ok("DRY_RUN: " + product.id() + " -> " + product.actions().size() + " acciones; " + product.verification());
        try {
            IdentityResolution identity = identities.resolve(player);
            PurchaseRepository.Delivery delivery = repository.createOrLoad(PROVIDER, transaction, identity.resolved() ? identity.canonicalName() : player, identity.uuid(), product, actor);
            if (!identity.resolved()) {
                repository.deliveryState(delivery.id(), PurchaseState.FAILED_MANUAL_REVIEW, identity.detail());
                repository.audit(delivery.id(), actor, "IDENTITY_" + identity.status(), identity.detail());
                return Result.error("Identidad en revisión: " + identity.detail());
            }
            if (delivery.state() == PurchaseState.COMPLETED) return Result.ok("Idempotente: la compra ya estaba completada.");
            process(delivery, actor);
            PurchaseRepository.Delivery current = repository.find(PROVIDER, transaction, productId).orElseThrow();
            return Result.ok("Estado: " + current.state());
        } catch (Exception error) { return Result.error("No se pudo registrar/procesar: " + error.getMessage()); }
    }

    public Result retry(String transaction, String actor) {
        try {
            List<PurchaseRepository.Delivery> deliveries = repository.findByTransaction(transaction);
            if (deliveries.isEmpty()) return Result.error("Transacción no encontrada.");
            for (PurchaseRepository.Delivery delivery : deliveries) process(delivery, actor);
            return Result.ok("Retry ejecutado para " + deliveries.size() + " entrega(s).");
        } catch (Exception error) { return Result.error(error.getMessage()); }
    }

    public Result retryAction(String transaction, long actionId, String actor) {
        try {
            List<PurchaseRepository.Delivery> deliveries = repository.findByTransaction(transaction);
            for (PurchaseRepository.Delivery delivery : deliveries) {
                for (PurchaseRepository.ActionRecord action : repository.actions(delivery.id())) {
                    if (action.id() == actionId) {
                        repository.actionState(action.id(), ActionState.FAILED_RETRYABLE, null, "Retry administrativo");
                        process(delivery, actor); return Result.ok("Acción reintentada.");
                    }
                }
            }
            return Result.error("Acción no encontrada en la transacción.");
        } catch (Exception error) { return Result.error(error.getMessage()); }
    }

    public void resumePlayer(String player, String actor) {
        try {
            IdentityResolution identity = identities.resolve(player);
            List<PurchaseRepository.Delivery> pending = identity.resolved()
                    ? repository.findPendingForIdentity(identity.uuid(), identity.canonicalName())
                    : repository.findPendingForPlayer(player);
            for (PurchaseRepository.Delivery delivery : pending) process(delivery, actor);
        }
        catch (Exception ignored) { }
    }

    public void recover(String actor) {
        try { for (PurchaseRepository.Delivery delivery : repository.pending()) process(delivery, actor); }
        catch (Exception ignored) { }
    }

    private void process(PurchaseRepository.Delivery delivery, String actor) throws SQLException {
        if (!processing.add(delivery.id())) return;
        try {
            ProductDefinition product = catalog.get(delivery.product());
            if (product == null) { repository.deliveryState(delivery.id(), PurchaseState.FAILED_MANUAL_REVIEW, "Producto eliminado del catálogo"); return; }
            IdentityResolution resolution = delivery.uuid() == null ? identities.resolve(delivery.player()) : null;
            if (resolution != null && !resolution.resolved()) {
                repository.deliveryState(delivery.id(), PurchaseState.FAILED_MANUAL_REVIEW, resolution.detail());
                repository.audit(delivery.id(), actor, "IDENTITY_" + resolution.status(), resolution.detail());
                return;
            }
            if (resolution != null) repository.bindIdentity(delivery.id(), new PurchaseRepository.PlayerIdentity(resolution.uuid(), resolution.canonicalName(), resolution.platform(), resolution.confidence()), actor);
            UUID uuid = delivery.uuid() == null ? resolution.uuid() : UUID.fromString(delivery.uuid());
            String canonical = resolution == null ? delivery.player() : resolution.canonicalName();
            ExecutionContext context = new ExecutionContext(delivery.id(), delivery.provider(), delivery.transaction(), canonical, uuid, product, false, actor);
            repository.deliveryState(delivery.id(), PurchaseState.PROCESSING, null);
            boolean waiting = false, retryable = false, manual = false;
            for (PurchaseRepository.ActionRecord record : repository.actionable(delivery.id())) {
                ProductAction action = product.actions().stream().filter(candidate -> candidate.id().equals(record.key())).findFirst().orElse(null);
                if (action == null) { repository.actionState(record.id(), ActionState.FAILED_MANUAL_REVIEW, null, "Acción ausente del catálogo v" + product.version()); manual = true; continue; }
                if (action.type() == ActionType.ANNOUNCEMENT) continue;
                if (action.requiresOnline() && !runtime.isOnline(delivery.player())) {
                    repository.actionState(record.id(), ActionState.WAITING_FOR_PLAYER, null, "Jugador offline"); waiting = true; continue;
                }
                repository.actionState(record.id(), ActionState.PROCESSING, null, null);
                ActionResult result = runtime.execute(context, action);
                switch (result.status()) {
                    case COMPLETED -> repository.actionState(record.id(), ActionState.COMPLETED, result.detail(), null);
                    case WAITING_FOR_PLAYER -> { repository.actionState(record.id(), ActionState.WAITING_FOR_PLAYER, null, result.detail()); waiting = true; }
                    case RETRYABLE_FAILURE -> { repository.actionState(record.id(), ActionState.FAILED_RETRYABLE, null, result.detail()); retryable = true; }
                    case MANUAL_REVIEW -> { repository.actionState(record.id(), ActionState.FAILED_MANUAL_REVIEW, null, result.detail()); manual = true; }
                    case SKIPPED -> repository.actionState(record.id(), ActionState.SKIPPED, result.detail(), null);
                }
            }
            List<PurchaseRepository.ActionRecord> actions = repository.actions(delivery.id());
            boolean incomplete = actions.stream().anyMatch(a -> a.required() && a.type() != ActionType.ANNOUNCEMENT && !Set.of(ActionState.COMPLETED, ActionState.SKIPPED).contains(a.state()));
            if (!incomplete) {
                completeAnnouncement(context, actions);
                // The announcement can fail independently after all rewards were delivered.
                // Reload action state so recovery can retry it without replaying rewards.
                actions = repository.actions(delivery.id());
            }
            waiting = actions.stream().anyMatch(a -> a.required() && a.state() == ActionState.WAITING_FOR_PLAYER);
            retryable = actions.stream().anyMatch(a -> a.required() && a.state() == ActionState.FAILED_RETRYABLE);
            manual = actions.stream().anyMatch(a -> a.required() && a.state() == ActionState.FAILED_MANUAL_REVIEW);
            PurchaseState state = manual ? PurchaseState.FAILED_MANUAL_REVIEW : retryable ? PurchaseState.FAILED_RETRYABLE : waiting ? PurchaseState.WAITING_FOR_PLAYER : incomplete ? PurchaseState.PARTIALLY_DELIVERED : PurchaseState.COMPLETED;
            repository.deliveryState(delivery.id(), state, null);
            repository.audit(delivery.id(), actor, "STATE_" + state, null);
            emitTelemetry(product.id(), state);
        } finally { processing.remove(delivery.id()); }
    }

    private void completeAnnouncement(ExecutionContext context, List<PurchaseRepository.ActionRecord> records) throws SQLException {
        for (PurchaseRepository.ActionRecord record : records) {
            if (record.type() != ActionType.ANNOUNCEMENT || record.state() == ActionState.COMPLETED) continue;
            ProductAction action = context.product().actions().stream().filter(candidate -> candidate.id().equals(record.key())).findFirst().orElseThrow();
            ActionResult result = runtime.execute(context, action);
            if (result.status() != ActionResult.Status.COMPLETED) {
                repository.actionState(record.id(), ActionState.FAILED_RETRYABLE, result.detail(), null);
                continue;
            }
            if (!repository.markAnnouncementSent(context.deliveryId())) {
                repository.actionState(record.id(), ActionState.SKIPPED, "Anuncio ya emitido", null);
                continue;
            }
            repository.actionState(record.id(), ActionState.COMPLETED, result.detail(), null);
        }
    }

    public Result financialEvent(String transaction, String productId, boolean chargeback, String actor) {
        String type = chargeback ? "CHARGEBACK" : "REFUND";
        try {
            ProductDefinition product = catalog.get(productId);
            if (product == null) return Result.error("Producto inexistente.");
            PurchaseRepository.Delivery delivery = repository.find(PROVIDER, transaction, productId).orElse(null);
            if (delivery == null) return Result.error("Entrega no encontrada.");
            if (!repository.registerFinancialEvent(PROVIDER, transaction, productId, type, actor)) return Result.ok("Evento financiero ya procesado.");
            ExecutionContext context = new ExecutionContext(delivery.id(), PROVIDER, transaction, delivery.player(),
                    delivery.uuid() == null ? runtime.resolveUuid(delivery.player()) : UUID.fromString(delivery.uuid()), product, false, actor);
            List<PurchaseRepository.ActionRecord> records = repository.actions(delivery.id());
            for (int index = records.size() - 1; index >= 0; index--) {
                PurchaseRepository.ActionRecord record = records.get(index);
                if (record.state() != ActionState.COMPLETED) continue;
                ProductAction action = product.actions().stream().filter(candidate -> candidate.id().equals(record.key())).findFirst().orElseThrow();
                if (action.refundPolicy() == RefundPolicy.AUTO_REVOKE) {
                    ActionResult result = runtime.revoke(context, action, record.result());
                    repository.actionState(record.id(), result.status() == ActionResult.Status.COMPLETED ? ActionState.REVOKED : ActionState.FAILED_MANUAL_REVIEW, result.detail(), result.status() == ActionResult.Status.COMPLETED ? null : result.detail());
                } else if (action.refundPolicy() == RefundPolicy.MANUAL_REVIEW) {
                    repository.actionState(record.id(), ActionState.FAILED_MANUAL_REVIEW, record.result(), "Revisión requerida por " + type);
                }
            }
            repository.deliveryState(delivery.id(), chargeback ? PurchaseState.CHARGEBACK : PurchaseState.REFUNDED, null);
            repository.audit(delivery.id(), actor, type, "Política de revocación aplicada");
            emitTelemetry(product.id(), chargeback ? PurchaseState.CHARGEBACK : PurchaseState.REFUNDED);
            return Result.ok(type + " registrado.");
        } catch (Exception error) { return Result.error(error.getMessage()); }
    }

    public List<PurchaseRepository.Delivery> pending() throws SQLException { return repository.pending(); }
    public List<PurchaseRepository.Delivery> history(String player) throws SQLException { return repository.history(player); }
    public List<PurchaseRepository.Delivery> status(String transaction) throws SQLException { return repository.findByTransaction(transaction); }
    public ProductCatalog catalog() { return catalog; }

    public Result adminState(String transaction, String operation, String actor) {
        try {
            List<PurchaseRepository.Delivery> deliveries = repository.findByTransaction(transaction);
            if (deliveries.isEmpty()) return Result.error("Transacción no encontrada.");
            PurchaseState state = switch (operation.toLowerCase()) {
                case "manual-review" -> PurchaseState.FAILED_MANUAL_REVIEW;
                case "complete" -> PurchaseState.COMPLETED;
                case "cancel" -> PurchaseState.CANCELLED;
                default -> throw new IllegalArgumentException("Operación inválida");
            };
            for (PurchaseRepository.Delivery delivery : deliveries) {
                repository.deliveryState(delivery.id(), state, "Estado administrativo");
                repository.audit(delivery.id(), actor, "ADMIN_" + state, null);
            }
            return Result.ok("Estado " + state + " aplicado a " + deliveries.size() + " entrega(s).");
        } catch (Exception error) { return Result.error(error.getMessage()); }
    }

    public Result reconcile(String actor) {
        try {
            int resumed = 0;
            for (PurchaseRepository.Delivery delivery : repository.pending()) { process(delivery, actor); resumed++; }
            return Result.ok("Reconciliación ejecutada sobre " + resumed + " entrega(s).");
        } catch (Exception error) { return Result.error(error.getMessage()); }
    }

    public Result resolveIdentity(String transaction, UUID uuid, String actor) {
        try {
            PurchaseRepository.PlayerIdentity identity = repository.findIdentityByUuid(uuid).orElse(null);
            if (identity == null) return Result.error("UUID no observado por el servidor.");
            List<PurchaseRepository.Delivery> deliveries = repository.findByTransaction(transaction);
            if (deliveries.isEmpty()) return Result.error("Transacción no encontrada.");
            for (PurchaseRepository.Delivery delivery : deliveries) { repository.bindIdentity(delivery.id(), identity, actor); process(repository.find(delivery.provider(), delivery.transaction(), delivery.product()).orElseThrow(), actor); }
            return Result.ok("Identidad vinculada y acciones pendientes reanudadas.");
        } catch (Exception error) { return Result.error(error.getMessage()); }
    }

    private boolean validTransaction(String value) { return value != null && value.matches("[A-Za-z0-9._:-]{3,128}"); }
    private void emitTelemetry(String productId, PurchaseState state) {
        try { telemetry.accept(new Telemetry(productId, state)); }
        catch (Exception ignored) { /* Observability must never affect delivery. */ }
    }
    public record Telemetry(String productId, PurchaseState state) { }
    public record Result(boolean success, String message) { public static Result ok(String value){return new Result(true,value);} public static Result error(String value){return new Result(false,value);} }
}
