package org.metamechanists.odysseia.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.modalities.Modality;

import java.util.List;
import java.util.Locale;

/**
 * Hace que /spawn lleve al spawn de la modalidad que estas jugando, no siempre al lobby.
 *
 * EssentialsX resuelve /spawn por GRUPO de permisos, no por mundo, asi que con un unico grupo
 * manda a todo el mundo al mismo sitio: el lobby. En un servidor de una sola modalidad eso da
 * igual; aqui significa que un jugador de Clasico que escribe /spawn acaba fuera de Clasico.
 *
 * El destino sale del punto de aparicion del propio mundo, que es el que ya gestiona Multiverse
 * con /mv setspawn. Asi no hay una segunda lista de coordenadas que mantener a mano y en
 * paralelo: se mueve el spawn del mundo y esto lo sigue solo.
 *
 * Para volver al hub queda /lobby, que es explicito. Antes la unica forma de salir era /spawn,
 * que es justo lo que ahora ya no saca a nadie de su modalidad sin pedirlo.
 */
public final class ModalitySpawnListener implements Listener {

    private final Odysseia plugin;

    public ModalitySpawnListener(Odysseia plugin) {
        this.plugin = plugin;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("modalidades.spawn-por-modalidad.enabled", true);
    }

    /**
     * Si la modalidad gestiona su propia aparicion y hay que dejarla en paz.
     *
     * SkyBlock y OneBlock no tienen spawn: se entra directamente a la isla, y de eso se encarga
     * BentoBox. Sus mundos son de vacio, asi que el punto de aparicion del mundo --8,64,8-- no
     * es un sitio, es aire: mandar ahi a alguien es tirarlo al vacio. Y al morir, BentoBox ya
     * devuelve al jugador a su isla; pisar ese respawn seria cambiar una vuelta a casa por una
     * caida.
     *
     * Por eso la exclusion es una lista explicita y no una heuristica sobre el tipo de mundo:
     * quien anada una modalidad gestionada por otro plugin tiene que decirlo aqui a mano.
     */
    private boolean gestionaSuPropioSpawn(Modality modalidad) {
        for (String id : plugin.getConfig()
                .getStringList("modalidades.spawn-por-modalidad.gestionan-su-spawn")) {
            if (id.equalsIgnoreCase(modalidad.id())) return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!enabled()) return;

        List<String> escrito = ModalityStorageGuardListener.tokens(
                event.getMessage().startsWith("/") ? event.getMessage().substring(1) : event.getMessage());
        if (escrito.isEmpty()) return;

        String comando = escrito.get(0);
        Player player = event.getPlayer();

        String comandoLobby = plugin.getConfig()
                .getString("modalidades.spawn-por-modalidad.comando-lobby", "lobby")
                .toLowerCase(Locale.ROOT);

        if (comando.equals(comandoLobby)) {
            event.setCancelled(true);
            irAlLobby(player);
            return;
        }

        if (!comando.equals("spawn")) return;

        // Se deja pasar sin tocarlo: lo atiende quien corresponda (BentoBox, EssentialsX).
        if (gestionaSuPropioSpawn(modalidad(player))) return;

        World destino = mundoDeLaModalidad(player);
        // Si la modalidad no resuelve a ningun mundo cargado, no se toca el comando: que lo
        // atienda EssentialsX como siempre. Mas vale caer en el lobby que no ir a ningun sitio.
        if (destino == null) return;

        if (destino.equals(player.getWorld())) {
            event.setCancelled(true);
            teletransportar(player, destino.getSpawnLocation());
            player.sendMessage(color("&6DrakesCraft &8· &7Al spawn de &e"
                    + limpio(modalidad(player).displayName()) + "&7."));
            return;
        }

        event.setCancelled(true);
        teletransportar(player, destino.getSpawnLocation());
        player.sendMessage(color("&6DrakesCraft &8· &7Al spawn de &e"
                + limpio(modalidad(player).displayName()) + "&7. &8Usa &7/" + comandoLobby
                + " &8para volver al lobby."));
    }

    /**
     * Al morir se reaparece en la modalidad, no en el lobby.
     *
     * Sin esto la mitad del arreglo se queda coja: da igual que /spawn respete la modalidad si
     * cada muerte te saca de ella. Se cede ante la cama y el ancla, que son decision del jugador.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!enabled()) return;
        if (event.isBedSpawn() || event.isAnchorSpawn()) return;
        if (gestionaSuPropioSpawn(modalidad(event.getPlayer()))) return;

        World destino = mundoDeLaModalidad(event.getPlayer());
        if (destino != null) {
            event.setRespawnLocation(destino.getSpawnLocation());
        }
    }

    private Modality modalidad(Player player) {
        return plugin.getModalityService().resolve(player);
    }

    /**
     * El mundo cuyo spawn representa a una modalidad por id, o null si no se puede saber.
     *
     * Publico porque /survival necesita exactamente esta respuesta y no debe tener la suya:
     * dos formas de resolver el mismo destino acaban divergiendo en cuanto alguien mueve un
     * mundo de sitio.
     */
    public World mundoDeModalidad(String id) {
        String configurado = plugin.getConfig()
                .getString("modalidades.spawn-por-modalidad.mundos." + id.toLowerCase(Locale.ROOT));
        if (configurado != null && !configurado.isBlank()) {
            World mundo = Bukkit.getWorld(configurado);
            if (mundo != null) return mundo;
        }
        for (Modality modalidad : plugin.getModalityService().modalities()) {
            if (!modalidad.id().equalsIgnoreCase(id)) continue;
            for (String nombre : modalidad.worlds()) {
                World mundo = Bukkit.getWorld(nombre);
                if (mundo != null) return mundo;
            }
        }
        return null;
    }

    /** El mundo cuyo spawn representa a la modalidad del jugador, o null si no se puede saber. */
    private World mundoDeLaModalidad(Player player) {
        Modality modalidad = modalidad(player);
        String id = modalidad.id().toLowerCase(Locale.ROOT);

        // Un mundo declarado a mano manda sobre todo lo demas. Hace falta para survival, que es
        // la modalidad de respaldo y por eso no declara mundos propios.
        String configurado = plugin.getConfig()
                .getString("modalidades.spawn-por-modalidad.mundos." + id);
        if (configurado != null && !configurado.isBlank()) {
            World mundo = Bukkit.getWorld(configurado);
            if (mundo != null) return mundo;
            plugin.getLogger().warning("[Spawn] La modalidad " + id + " apunta al mundo '"
                    + configurado + "', que no esta cargado.");
        }

        // Si el jugador ya esta en un mundo de su modalidad, ese mismo sirve: respeta el nether
        // y el end de cada modalidad en vez de sacarlo al overworld al morir.
        if (modalidad.matches(player.getWorld().getName())) {
            return player.getWorld();
        }

        for (String nombre : modalidad.worlds()) {
            World mundo = Bukkit.getWorld(nombre);
            if (mundo != null) return mundo;
        }
        return null;
    }

    private void irAlLobby(Player player) {
        String nombre = plugin.getConfig()
                .getString("modalidades.spawn-por-modalidad.mundo-lobby", "SpawnWarps");
        World lobby = Bukkit.getWorld(nombre);
        if (lobby == null) {
            player.sendMessage(color("&6DrakesCraft &8· &cEl lobby no esta disponible ahora mismo."));
            plugin.getLogger().warning("[Spawn] El mundo del lobby '" + nombre + "' no esta cargado.");
            return;
        }
        teletransportar(player, lobby.getSpawnLocation());
        player.sendMessage(color("&6DrakesCraft &8· &7Al lobby. Elige modalidad con &e/modalidades&7."));
    }

    private void teletransportar(Player player, Location destino) {
        Bukkit.getScheduler().runTask(plugin, () -> player.teleport(destino));
    }

    private static String limpio(String texto) {
        return texto == null ? "" : texto;
    }

    private static String color(String texto) {
        return ChatColor.translateAlternateColorCodes('&', texto);
    }
}
