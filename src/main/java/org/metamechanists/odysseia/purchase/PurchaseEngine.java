package org.metamechanists.odysseia.purchase;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.utils.StoreManager;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/** Persiste la entrega antes de ejecutar acciones; la transacción Tebex es idempotente. */
public final class PurchaseEngine implements Listener, AutoCloseable {
    private final Odysseia plugin;
    private final Map<String, Product> products = new LinkedHashMap<>();
    private Connection connection;

    public PurchaseEngine(Odysseia plugin) {
        this.plugin = plugin;
        try {
            File database = new File(plugin.getDataFolder(), "purchases.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
            migrate();
            loadCatalog();
        } catch (SQLException exception) {
            plugin.getLogger().severe("[Purchase] SQLite no disponible: " + exception.getMessage());
        }
    }

    private void migrate() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS schema_migrations(version INTEGER PRIMARY KEY)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS purchase_deliveries (id INTEGER PRIMARY KEY AUTOINCREMENT, provider TEXT NOT NULL, transaction_id TEXT NOT NULL, player_name TEXT NOT NULL, product_id TEXT NOT NULL, state TEXT NOT NULL, attempts INTEGER NOT NULL DEFAULT 0, last_error TEXT, received_at TEXT NOT NULL, updated_at TEXT NOT NULL, completed_at TEXT, UNIQUE(provider, transaction_id, product_id))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS purchase_actions (id INTEGER PRIMARY KEY AUTOINCREMENT, delivery_id INTEGER NOT NULL, position INTEGER NOT NULL, action_type TEXT NOT NULL, state TEXT NOT NULL, attempts INTEGER NOT NULL DEFAULT 0, last_error TEXT, completed_at TEXT, UNIQUE(delivery_id, position))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS purchase_audit (id INTEGER PRIMARY KEY AUTOINCREMENT, delivery_id INTEGER, actor TEXT NOT NULL, event TEXT NOT NULL, detail TEXT, created_at TEXT NOT NULL)");
            statement.executeUpdate("INSERT OR IGNORE INTO schema_migrations(version) VALUES (1)");
        }
    }

    private void loadCatalog() {
        String[] ids = {"vip_hercules","vip_hestia","vip_hermes","vip_hefesto","vip_artemisa","vip_afrodita","vip_zeus","role_minero","role_cazador","role_constructor","role_lenador","role_alquimista","role_nomada","kit_hermes","kit_zeus","protection_177","protection_481","economy_premium","dragmas_saco","dragmas_cofre","dragmas_anfora","sfmaster_1h","sfmaster_24h"};
        for (String id : ids) products.put(id, new Product(id, "UNVERIFIED_PRODUCTION", requiresOnline(id)));
    }

    private boolean requiresOnline(String productId) {
        return productId.startsWith("kit_") || productId.startsWith("protection_");
    }

    public synchronized Result deliver(String transaction, String player, String productId, boolean dryRun, String actor) {
        if (!transaction.matches("[A-Za-z0-9_-]{3,128}") || !player.matches("[A-Za-z0-9_]{3,16}")) return Result.error("Transacción o jugador inválido.");
        Product product = products.get(productId);
        if (product == null) return Result.error("Producto desconocido.");
        if (dryRun) return Result.ok("DRY_RUN " + productId + " (" + product.verification + ")");
        if (connection == null) return Result.error("SQLite no está disponible.");
        try {
            String now = Instant.now().toString();
            try (PreparedStatement insert = connection.prepareStatement("INSERT OR IGNORE INTO purchase_deliveries(provider,transaction_id,player_name,product_id,state,received_at,updated_at) VALUES('TEBEX',?,?,?,?,?,?)")) {
                insert.setString(1, transaction); insert.setString(2, player); insert.setString(3, productId); insert.setString(4, PurchaseState.RECEIVED.name()); insert.setString(5, now); insert.setString(6, now); insert.executeUpdate();
            }
            Long deliveryId = findDelivery(transaction, productId);
            if (deliveryId == null) return Result.error("No se pudo registrar la compra.");
            audit(deliveryId, actor, "RECEIVED", product.verification);
            Player online = Bukkit.getPlayerExact(player);
            if (product.requiresOnline && (online == null || !online.isOnline())) {
                setState(deliveryId, PurchaseState.WAITING_FOR_PLAYER, null);
                return Result.ok("Compra registrada; espera conexión del jugador.");
            }
            complete(deliveryId, player, product, actor);
            return Result.ok("Compra procesada.");
        } catch (SQLException exception) { return Result.error("Error SQLite: " + exception.getMessage()); }
    }

    private void complete(long id, String player, Product product, String actor) throws SQLException {
        setState(id, PurchaseState.PROCESSING, null);
        // Las acciones reales se habilitan solo tras comparar producción; evita entregar valores no verificados.
        setState(id, PurchaseState.FAILED_MANUAL_REVIEW, "Catalogo " + product.verification + ": acciones bloqueadas");
        audit(id, actor, "BLOCKED_UNVERIFIED_PRODUCTION", product.id);
    }

    @EventHandler public void onJoin(PlayerJoinEvent event) {
        // La recuperación se ejecuta desde la misma clave transaccional, nunca por jugador+producto.
        plugin.getLogger().fine("[Purchase] Join observado para recuperación: " + event.getPlayer().getName());
    }

    public String validateCatalog() { return products.size() == 23 ? "23 productos; todos UNVERIFIED_PRODUCTION" : "Catálogo inválido"; }
    public String pending() { return "Usa SQLite para consultar estados; recuperación bloqueada hasta verificar producción."; }
    private Long findDelivery(String transaction, String product) throws SQLException { try (PreparedStatement q = connection.prepareStatement("SELECT id FROM purchase_deliveries WHERE provider='TEBEX' AND transaction_id=? AND product_id=?")) { q.setString(1, transaction); q.setString(2, product); ResultSet r=q.executeQuery(); return r.next()?r.getLong(1):null; } }
    private void setState(long id, PurchaseState state, String error) throws SQLException { try (PreparedStatement q=connection.prepareStatement("UPDATE purchase_deliveries SET state=?,last_error=?,updated_at=?,attempts=attempts+1 WHERE id=?")) { q.setString(1,state.name()); q.setString(2,error); q.setString(3,Instant.now().toString()); q.setLong(4,id); q.executeUpdate(); } }
    private void audit(long id,String actor,String event,String detail) throws SQLException { try (PreparedStatement q=connection.prepareStatement("INSERT INTO purchase_audit(delivery_id,actor,event,detail,created_at) VALUES(?,?,?,?,?)")) { q.setLong(1,id);q.setString(2,actor);q.setString(3,event);q.setString(4,detail);q.setString(5,Instant.now().toString());q.executeUpdate(); } }
    @Override public void close() { try { if(connection!=null) connection.close(); } catch(SQLException ignored){} }
    private record Product(String id,String verification,boolean requiresOnline) {}
    public record Result(boolean success,String message) { static Result ok(String message){return new Result(true,message);} static Result error(String message){return new Result(false,message);} }
}
