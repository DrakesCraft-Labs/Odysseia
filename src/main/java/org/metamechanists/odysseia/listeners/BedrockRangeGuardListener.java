package org.metamechanists.odysseia.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;

/**
 * Mantiene a los jugadores de Bedrock dentro del rango donde su cliente funciona.
 *
 * Minecraft Bedrock pierde precision con coordenadas altas: mas alla de unos 150.000 bloques el
 * jugador apenas puede caminar, el terreno tiembla y los bloques dejan de responder. No es un
 * fallo del servidor y no tiene arreglo desde aqui, asi que lo unico sensato es no dejar que
 * lleguen ahi.
 *
 * Tres capas, de la mas suave a la mas dura:
 *   1. Un borde de mundo propio para cada jugador Bedrock, que su cliente dibuja y respeta solo.
 *   2. Cancelacion de cualquier teletransporte cuyo destino se pase del limite, venga de donde
 *      venga: /tpa, /home, /warp, /back, /rtp o un plugin.
 *   3. Aviso a ambas partes cuando se pide un /tpa que cruzaria esa frontera.
 *
 * La direccion Java -> Bedrock siempre se permite: un jugador de Java puede ir sin problema a
 * donde este el de Bedrock. La que se corta es la contraria.
 */
public final class BedrockRangeGuardListener implements Listener {

    /** Comandos que piden ir hacia otro jugador. */
    private static final List<String> IR_HACIA = List.of("tpa", "tpask", "call", "tpahere");

    private final JavaPlugin plugin;

    public BedrockRangeGuardListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Floodgate antepone un punto al nombre de las cuentas Bedrock. */
    public static boolean esBedrock(Player player) {
        return player.getName().startsWith(".");
    }

    private boolean activo() {
        return plugin.getConfig().getBoolean("bedrock-guard.enabled", true);
    }

    /** Limite del mundo indicado; el nether va comprimido igual que sus coordenadas. */
    private int limite(World world) {
        String nombre = world.getName().toLowerCase(Locale.ROOT);
        int base = plugin.getConfig().getInt("bedrock-guard.limite", 150_000);
        if (nombre.endsWith("_nether")) {
            return plugin.getConfig().getInt("bedrock-guard.limite-nether", base / 8);
        }
        return base;
    }

    /** Mundo de exploracion libre; los acotados —islas, hub, arenas— no necesitan borde propio. */
    private boolean mundoAmplio(World world) {
        String nombre = world.getName().toLowerCase(Locale.ROOT);
        List<String> configurados = plugin.getConfig().getStringList("bedrock-guard.mundos");
        // Si la seccion no llego a produccion, vigilar al menos el mundo principal en vez de
        // quedarse inerte sin ningun aviso.
        if (configurados.isEmpty()) return nombre.equals("world");
        return configurados.stream().anyMatch(m -> nombre.equals(m.toLowerCase(Locale.ROOT)));
    }

    private boolean fueraDeRango(Location location) {
        if (location.getWorld() == null) return false;
        return fueraDeRango(location.getX(), location.getZ(), limite(location.getWorld()));
    }

    /** Frontera cuadrada, igual que la del borde de mundo: cuenta cada eje por separado. */
    static boolean fueraDeRango(double x, double z, int limite) {
        return Math.abs(x) > limite || Math.abs(z) > limite;
    }

    /** Nombre del comando sin la barra ni el namespace del plugin ({@code essentials:tpa}). */
    static String etiquetaDe(String mensaje) {
        String[] partes = mensaje.substring(1).trim().split("\\s+");
        String etiqueta = partes[0].toLowerCase(Locale.ROOT);
        int separador = etiqueta.lastIndexOf(':');
        return separador < 0 ? etiqueta : etiqueta.substring(separador + 1);
    }

    /** Un comando de teletransporte hacia otro jugador. */
    static boolean esPeticionDeViaje(String etiqueta) {
        return IR_HACIA.contains(etiqueta);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        aplicarBorde(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        aplicarBorde(event.getPlayer());
    }

    /**
     * Da a cada jugador Bedrock su propio borde de mundo.
     *
     * Es la capa mas barata: la dibuja su cliente y le impide cruzarla sin que el servidor tenga
     * que comprobar nada en cada movimiento.
     */
    private void aplicarBorde(Player player) {
        if (!activo() || !esBedrock(player)) return;
        World world = player.getWorld();
        if (!mundoAmplio(world)) {
            player.setWorldBorder(null);
            return;
        }
        WorldBorder borde = Bukkit.createWorldBorder();
        borde.setCenter(0.5, 0.5);
        borde.setSize(limite(world) * 2.0D);
        borde.setWarningDistance(64);
        player.setWorldBorder(borde);
    }

    /** Ningun teletransporte puede dejar a un jugador Bedrock fuera de su rango util. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!activo()) return;
        Player player = event.getPlayer();
        if (!esBedrock(player) || !mundoAmplio(event.getTo().getWorld())) return;
        if (!fueraDeRango(event.getTo())) return;

        event.setCancelled(true);
        int limite = limite(event.getTo().getWorld());
        player.sendMessage(color("&6DrakesCraft &8· &cNo puedes viajar tan lejos."));
        player.sendMessage(color("&7Minecraft &eBedrock &7pierde precisión más allá de &e"
                + String.format(Locale.ROOT, "%,d", limite) + " &7bloques: el terreno tiembla y"
                + " apenas se puede caminar. Es un límite del cliente, no del servidor."));
        player.sendMessage(color("&7Quien juegue en &eJava &7sí puede viajar hasta donde estás tú."));
    }

    /**
     * Avisa a las dos partes cuando un /tpa cruzaria la frontera.
     *
     * El jugador de Java no siempre sabe que su amigo es de Bedrock, y el de Bedrock no entiende
     * por que no llega. Sin este aviso la conversacion acaba en "esta bugeado".
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleportRequest(PlayerCommandPreprocessEvent event) {
        if (!activo()) return;
        String[] partes = event.getMessage().substring(1).trim().split("\\s+");
        if (partes.length < 2) return;

        String etiqueta = etiquetaDe(event.getMessage());
        if (!esPeticionDeViaje(etiqueta)) return;

        Player emisor = event.getPlayer();
        Player destino = Bukkit.getPlayerExact(partes[1]);
        if (destino == null || destino.equals(emisor)) return;

        // Quien acaba viajando depende del comando: tpahere trae al otro.
        Player viajero = etiqueta.equals("tpahere") ? destino : emisor;
        Player anfitrion = etiqueta.equals("tpahere") ? emisor : destino;
        if (!esBedrock(viajero) || !fueraDeRango(anfitrion.getLocation())) return;

        int limite = limite(anfitrion.getLocation().getWorld());
        String aviso = "&6DrakesCraft &8· &e" + viajero.getName() + " &7juega en &eBedrock&7 y"
                + " no puede viajar más allá de &e" + String.format(Locale.ROOT, "%,d", limite)
                + " &7bloques.";
        emisor.sendMessage(color(aviso));
        destino.sendMessage(color(aviso));

        String salida = esBedrock(anfitrion)
                ? "&7Los dos juegan en Bedrock: tendrán que quedar más cerca del centro."
                : "&7Ve tú hacia " + viajero.getName() + " &7en su lugar: desde Java sí funciona.";
        emisor.sendMessage(color(salida));
        destino.sendMessage(color(salida));
    }

    private static String color(String texto) {
        return ChatColor.translateAlternateColorCodes('&', texto);
    }
}
