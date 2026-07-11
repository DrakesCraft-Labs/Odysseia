package org.metamechanists.odysseia.purchase;

import org.bukkit.Bukkit;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.metamechanists.odysseia.Odysseia;

import java.io.File;

/** Ciclo de vida Bukkit del motor; la lógica transaccional vive en PurchaseService. */
public final class PurchaseEngine implements Listener, AutoCloseable {
    private final Odysseia plugin;
    private PurchaseRepository repository;
    private PurchaseService service;
    private PlayerIdentityResolver identities;
    private String startupError;

    public PurchaseEngine(Odysseia plugin) {
        this.plugin = plugin;
        try {
            ProductCatalog catalog = new ProductCatalog(plugin);
            repository = new PurchaseRepository(new File(plugin.getDataFolder(), "purchases.db"));
            identities = new PlayerIdentityResolver(repository);
            service = new PurchaseService(catalog, repository, new BukkitPurchaseRuntime(plugin), identities);
            Bukkit.getScheduler().runTaskLater(plugin, () -> service.recover("SYSTEM_RECOVERY"), 40L);
            plugin.getLogger().info("[Purchase] Motor listo: " + catalog.all().size() + " productos, SQLite WAL.");
        } catch (Exception error) {
            startupError = error.getMessage();
            plugin.getLogger().severe("[Purchase] Motor deshabilitado: " + startupError);
        }
    }

    public PurchaseService service() { return service; }
    public String startupError() { return startupError; }
    @EventHandler public void onJoin(PlayerJoinEvent event) {
        if (service != null) Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try { identities.observe(event.getPlayer().getUniqueId(), event.getPlayer().getName()); }
            catch (Exception error) { plugin.getLogger().warning("[Purchase] Identidad no registrada: " + error.getMessage()); }
            service.resumePlayer(event.getPlayer().getName(), "PLAYER_JOIN");
        }, 20L);
    }
    @Override public void close() {
        if (repository != null) try { repository.close(); } catch (Exception error) { plugin.getLogger().warning("[Purchase] Error cerrando SQLite: " + error.getMessage()); }
    }
}
