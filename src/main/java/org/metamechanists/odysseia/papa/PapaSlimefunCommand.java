package org.metamechanists.odysseia.papa;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.metamechanists.odysseia.kits.CustomContentResolver;

/**
 * Entrega un item de Slimefun como premio del trueque.
 *
 * Existe porque el servidor es casi todo Slimefun y un canje que solo diera cosas de vanilla se
 * quedaria corto. Los niveles del trueque lo llaman con el id del item.
 *
 * Es de consola: lo invoca el canje, no el jugador.
 *
 * **Si el item no existe no se entrega nada, pero se grita en el log.** Un premio que falla en
 * silencio es peor que uno que no existe: el jugador ya pago sus papas. Es exactamente lo que paso
 * con los kits de oficio y con el cosmetico, asi que aqui queda registrado a proposito.
 */
public final class PapaSlimefunCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public PapaSlimefunCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player && !sender.hasPermission("odysseia.admin")) {
            sender.sendMessage(ChatColor.RED + "Este comando lo usa el trueque, no tu.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("Uso: /papasf <jugador> <ID_SLIMEFUN> [cantidad]");
            return true;
        }

        Player jugador = plugin.getServer().getPlayerExact(args[0]);
        if (jugador == null) {
            sender.sendMessage(ChatColor.RED + "Jugador no conectado: " + args[0]);
            return true;
        }

        String id = args[1].toUpperCase(java.util.Locale.ROOT);
        int cantidad = 1;
        if (args.length > 2) {
            try {
                cantidad = Math.max(1, Math.min(64, Integer.parseInt(args[2])));
            } catch (NumberFormatException ignored) {
                // cantidad invalida: se entrega una
            }
        }

        ItemStack item = CustomContentResolver.slimefunItem(id);
        if (item == null) {
            plugin.getLogger().severe("[Papa] El premio de Slimefun '" + id + "' NO EXISTE. "
                    + jugador.getName() + " pago y no recibio nada. Revisa papa-trader.yml.");
            jugador.sendMessage(ChatColor.RED
                    + "Ese premio no se pudo entregar. Avisa al staff: ya se avisó en el log.");
            return true;
        }

        item.setAmount(cantidad);
        // Lo que no quepa cae al suelo antes que perderse: el jugador ya lo pago.
        jugador.getInventory().addItem(item).values()
                .forEach(sobra -> jugador.getWorld().dropItemNaturally(jugador.getLocation(), sobra));
        plugin.getLogger().info("[Papa] Slimefun '" + id + "' x" + cantidad
                + " entregado a " + jugador.getName());
        return true;
    }
}
