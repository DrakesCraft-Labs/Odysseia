package org.metamechanists.odysseia.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Lleva al jugador a la modalidad principal.
 *
 * Existe como comando de plugin y no como alias de commands.yml a proposito: los alias de Bukkit
 * tienen la prioridad mas baja, asi que cualquier plugin que registre /survival gana. Eso hacia
 * que al staff lo pusiera en modo de juego survival en vez de teletransportarlo.
 *
 * El destino se resuelve delegando en el /spawn ya configurado, despachado desde consola para no
 * depender de que el jugador tenga el permiso de EssentialsX.
 */
public final class SurvivalCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public SurvivalCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo un jugador puede viajar a la modalidad principal.");
            return true;
        }

        String destino = plugin.getConfig().getString("modalidades.comando-spawn", "spawn");
        boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), destino + " " + player.getName());
        if (ok) {
            player.sendMessage(color("&6DrakesCraft &8· &7Te llevamos al &aSurvival&7."));
        } else {
            // Sin el comando de spawn disponible, al menos dejamos al jugador en un lugar valido.
            player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
            player.sendMessage(color("&6DrakesCraft &8· &7Te llevamos al &aSurvival&7. "
                    + "&8(spawn por defecto)"));
        }
        return true;
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
