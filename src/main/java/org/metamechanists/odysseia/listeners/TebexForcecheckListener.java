package org.metamechanists.odysseia.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.metamechanists.odysseia.purchase.ForcecheckThrottle;

/**
 * Pide a Tebex que revise entregas pendientes cuando alguien entra al servidor.
 *
 * Tebex entrega en su propio ciclo de sondeo. Quien compra y entra recien pasado un ciclo espera
 * hasta el siguiente: el 2026-08-06 a XlGranRojolx le tocaron 14 minutos con la compra ya pagada y
 * el producto listo. Odysseia la entrego en menos de un segundo desde que le llego la orden; el
 * problema era cuando llegaba.
 *
 * El ritmo lo lleva {@link ForcecheckThrottle}: varias entradas seguidas comparten una sola
 * revision y nunca se lanzan dos mas juntas que el enfriamiento. Si Tebex no esta instalado, esto
 * no hace nada.
 */
public final class TebexForcecheckListener implements Listener {

    private static final String COMANDO = "tebex forcecheck";

    private final JavaPlugin plugin;
    private final boolean enabled;
    private final long retrasoTicks;
    private final ForcecheckThrottle throttle;

    public TebexForcecheckListener(JavaPlugin plugin) {
        this.plugin = plugin;
        var config = plugin.getConfig();
        this.enabled = config.getBoolean("purchase-engine.tebex-forcecheck.enabled", true)
                && Bukkit.getPluginManager().getPlugin("Tebex") != null;
        // Un margen tras la entrada: si el jugador todavia esta cargando chunks, un kit que se
        // entrega al inventario puede perderse. Tres segundos bastan y siguen siendo instantaneos
        // comparados con los 15 minutos del ciclo.
        this.retrasoTicks = Math.max(20, config.getLong("purchase-engine.tebex-forcecheck.delay-ticks", 60));
        this.throttle = new ForcecheckThrottle(
                Math.max(30, config.getLong("purchase-engine.tebex-forcecheck.cooldown-seconds", 90)) * 1000L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) return;

        long esperaMillis = throttle.registrarEntrada(System.currentTimeMillis());
        if (esperaMillis < 0) return;   // ya hay una revision en camino; esta entrada la aprovecha

        long ticks = Math.max(retrasoTicks, esperaMillis / 50L);
        Bukkit.getScheduler().runTaskLater(plugin, this::forzarRevision, ticks);
    }

    private void forzarRevision() {
        throttle.registrarDisparo(System.currentTimeMillis());
        // Si para cuando toca no queda nadie conectado, no hay a quien entregarle: la revision se
        // ahorra y el proximo que entre la vuelve a pedir.
        if (Bukkit.getOnlinePlayers().isEmpty()) return;
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), COMANDO);
    }
}
