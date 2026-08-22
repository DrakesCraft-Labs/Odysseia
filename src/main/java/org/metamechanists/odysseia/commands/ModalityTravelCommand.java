package org.metamechanists.odysseia.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.modalities.Modality;

import java.util.Locale;

/**
 * Lleva al jugador a la modalidad cuyo nombre coincide con el comando escrito.
 *
 * <p>Sustituye al antiguo {@code SurvivalCommand}, que solo sabia de una. El
 * problema que arregla es concreto: <b>{@code /clasico} y {@code /laboratorio}
 * no los atendia nadie</b>. Estaban declarados en la config de modalidades y los
 * NPC del lobby los lanzaban, pero no existia ni comando registrado ni listener
 * que los recogiera, asi que el servidor los aceptaba y no pasaba absolutamente
 * nada: ni viaje, ni mensaje de error.
 *
 * <p>Se notaba poco porque las otras tres si tenian quien las atendiera:
 * {@code survival} por este mismo plugin, y {@code skyblock} y {@code oneblock}
 * por BentoBox. Y en el NPC de Clasico el fallo quedaba tapado por la segunda
 * accion, un {@code /rtp} que si funciona y movia al jugador — al mundo
 * equivocado, que era justo la pista.
 *
 * <p>La modalidad se resuelve por el nombre del comando contra el campo
 * {@code comando} de cada modalidad en la config, asi que anadir una nueva no
 * necesita otra clase: basta declararla y registrar su comando.
 */
public final class ModalityTravelCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public ModalityTravelCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo un jugador puede viajar entre modalidades.");
            return true;
        }

        String invocado = command.getName().toLowerCase(Locale.ROOT);
        Modality modalidad = porComando(invocado);
        if (modalidad == null) {
            player.sendMessage(color("&6DrakesCraft &8· &cEsa modalidad ya no existe."));
            plugin.getLogger().warning("[Modalidades] No hay modalidad con comando '" + invocado
                    + "'. Revisa modalidades.modos en config.yml.");
            return true;
        }

        World destino = resolverMundo(modalidad.id());
        if (destino != null) {
            player.teleport(destino.getSpawnLocation());
            player.sendMessage(color("&6DrakesCraft &8· &7Te llevamos a " + modalidad.displayName() + "&7."));
            return true;
        }

        /*
         * Sin mundo resuelto se cae al spawn por defecto. No es lo ideal, pero deja al jugador
         * en un sitio valido en vez de dejarlo donde estaba sin decirle nada, que es exactamente
         * el fallo que este comando viene a arreglar.
         */
        String comandoSpawn = plugin.getConfig().getString("modalidades.comando-spawn", "spawn");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), comandoSpawn + " " + player.getName());
        player.sendMessage(color("&6DrakesCraft &8· &7Te llevamos a " + modalidad.displayName()
                + "&7. &8(spawn por defecto)"));
        plugin.getLogger().warning("[Modalidades] No se pudo resolver el mundo de '" + modalidad.id()
                + "'. Revisa modalidades.spawn-por-modalidad.mundos." + modalidad.id()
                + " y que el mundo este cargado.");
        return true;
    }

    /** Busca la modalidad cuyo campo `comando` coincide con lo que se escribio. */
    private Modality porComando(String comando) {
        if (!(plugin instanceof Odysseia odysseia) || odysseia.getModalityService() == null) {
            return null;
        }
        for (Modality modalidad : odysseia.getModalityService().modalities()) {
            if (comando.equalsIgnoreCase(modalidad.command())) return modalidad;
        }
        return null;
    }

    /**
     * Se pide la resolucion al listener de spawn para no tener dos formas distintas
     * de calcular el mismo destino, que es como acaban divergiendo cuando alguien
     * mueve un mundo de sitio.
     */
    private World resolverMundo(String id) {
        if (!(plugin instanceof Odysseia odysseia) || odysseia.getModalitySpawn() == null) {
            return null;
        }
        return odysseia.getModalitySpawn().mundoDeModalidad(id);
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
