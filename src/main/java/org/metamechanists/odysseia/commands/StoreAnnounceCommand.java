package org.metamechanists.odysseia.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.utils.StoreManager;

/**
 * Dispara el anuncio público de Discord de una compra entregada.
 * Pensado para invocarse desde el panel de Tebex como deliverable de cada paquete,
 * de modo que las entregas vía plugin Tebex también generen el webhook de felicitaciones.
 *
 * Uso: /odysseiaannounce <nick> <producto...>
 */
public final class StoreAnnounceCommand implements CommandExecutor {

    private final Odysseia plugin;

    public StoreAnnounceCommand(Odysseia plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("odysseia.store.announce")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /" + label + " <nick> <producto>");
            return true;
        }

        String nick = args[0];
        // El producto puede tener espacios (ej. "Rango Hermes"): unimos el resto de argumentos.
        String productName = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        StoreManager.announcePurchase(plugin, nick, productName);
        plugin.getLogger().info("[Store] Anuncio manual de compra disparado para " + nick + " (" + productName + ").");
        sender.sendMessage(ChatColor.GREEN + "Anuncio de compra enviado: " + nick + " -> " + productName);
        return true;
    }
}
