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

        /*
         * Se resuelve el mundo de la modalidad en vez de despachar /spawn desde consola.
         *
         * Aquello mandaba al jugador al spawn de EssentialsX, que es unico y esta en el lobby:
         * pulsar "Slimefun" en el menu te dejaba justo donde ya estabas. No se notaba porque la
         * gente entraba al mundo con /rtp, y el holograma del lobby lo decia tal cual. Al cerrar
         * el /rtp en el lobby, esa via dejo de existir y esto quedo como unica entrada.
         *
         * La resolucion se pide al listener de spawn para no tener dos maneras distintas de
         * calcular el mismo destino, que es como acaban divergiendo cuando alguien mueve un mundo.
         */
        org.bukkit.World destino = plugin instanceof org.metamechanists.odysseia.Odysseia odysseia
                && odysseia.getModalitySpawn() != null
                        ? odysseia.getModalitySpawn().mundoDeModalidad("survival")
                        : null;

        if (destino != null) {
            player.teleport(destino.getSpawnLocation());
            player.sendMessage(color("&6DrakesCraft &8· &7Te llevamos al &aSurvival&7."));
            return true;
        }

        // Sin mundo resuelto se cae al comportamiento anterior, que al menos deja al jugador
        // en un sitio valido en vez de no hacer nada.
        String comando = plugin.getConfig().getString("modalidades.comando-spawn", "spawn");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), comando + " " + player.getName());
        player.sendMessage(color("&6DrakesCraft &8· &7Te llevamos al &aSurvival&7. &8(spawn por defecto)"));
        plugin.getLogger().warning("[Spawn] No se pudo resolver el mundo de survival; "
                + "revisa modalidades.spawn-por-modalidad.mundos.survival");
        return true;
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
