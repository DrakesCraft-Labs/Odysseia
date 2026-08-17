package org.metamechanists.odysseia.listeners;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.metamechanists.odysseia.Odysseia;

import java.util.List;
import java.util.Locale;

/**
 * Devuelve el combate de 1.8: cada click pega con el 100% del dano, sin esperar la barra.
 *
 * Desde 1.9 el juego escala el dano segun cuanto se haya recargado el atributo de velocidad de
 * ataque, de modo que golpear rapido resta dano. Eso no se puede desactivar con una opcion: la
 * unica palanca real es subir esa velocidad lo bastante como para que la recarga termine antes del
 * siguiente click, y entonces el juego aplica siempre el golpe completo.
 *
 * Se aplica como modificador con clave propia en vez de tocar el valor base. El valor base se
 * recalcula solo cuando cambia el arma en la mano, asi que escribirlo directamente se pierde en
 * cuanto el jugador cambia de item; un modificador sobrevive a ese recalculo y ademas se puede
 * retirar entero si se apaga la opcion.
 *
 * Tambien se anula el ataque en barrido, que no existia en 1.8 y reparte dano a quien pasaba cerca.
 */
public class LegacyCombatListener implements Listener {

    /**
     * Un valor tan alto vacia la barra en menos de un tick.
     *
     * No hace falta afinarlo: cualquier cosa por encima de veinte ataques por segundo ya deja la
     * recarga por debajo del tiempo minimo entre dos clicks humanos.
     */
    private static final double VELOCIDAD_ATAQUE = 1024.0D;

    private final Odysseia plugin;
    private final NamespacedKey clave;
    private final boolean activado;
    private final List<String> mundos;
    private final boolean anularBarrido;

    public LegacyCombatListener(Odysseia plugin) {
        this.plugin = plugin;
        this.clave = new NamespacedKey(plugin, "combate_clasico");

        var seccion = plugin.getConfig().getConfigurationSection("combate-clasico");
        this.activado = seccion == null || seccion.getBoolean("activado", true);
        this.anularBarrido = seccion == null || seccion.getBoolean("anular-barrido", true);
        // Lista vacia: se aplica en todos los mundos.
        this.mundos = seccion == null ? List.of() : seccion.getStringList("mundos").stream()
                .map(m -> m.toLowerCase(Locale.ROOT)).toList();
    }

    /** Aplica el combate clasico a quien ya estaba conectado; se llama al habilitar el plugin. */
    public void aplicarATodos() {
        for (Player jugador : Bukkit.getOnlinePlayers()) {
            aplicar(jugador);
        }
    }

    /**
     * Retira el modificador de todos los conectados.
     *
     * Sin esto, apagar la opcion o desactivar el plugin dejaria a la gente con la velocidad de
     * ataque alterada hasta que el servidor reescribiese su perfil.
     */
    public void retirarDeTodos() {
        for (Player jugador : Bukkit.getOnlinePlayers()) {
            limpiar(jugador.getAttribute(Attribute.ATTACK_SPEED));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        aplicar(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        // Al reaparecer el jugador estrena atributos, asi que hay que volver a ponerlo.
        Bukkit.getScheduler().runTask(plugin, () -> aplicar(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        aplicar(event.getPlayer());
    }

    /** El barrido reparte dano a todo lo que rodea al objetivo; en 1.8 no existia. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSweep(EntityDamageByEntityEvent event) {
        if (!activado || !anularBarrido) {
            return;
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            return;
        }
        if (!afectaAlMundo(event.getEntity().getWorld().getName())) {
            return;
        }
        event.setCancelled(true);
    }

    private void aplicar(Player jugador) {
        AttributeInstance atributo = jugador.getAttribute(Attribute.ATTACK_SPEED);
        if (atributo == null) {
            return;
        }

        // Siempre se limpia primero: el modificador viaja en el perfil del jugador y volver a
        // anadirlo sin quitar el anterior lanza una excepcion por clave duplicada.
        limpiar(atributo);

        if (!activado || !afectaAlMundo(jugador.getWorld().getName())) {
            return;
        }

        try {
            atributo.addModifier(new AttributeModifier(
                    clave, VELOCIDAD_ATAQUE, AttributeModifier.Operation.ADD_NUMBER));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[Combate] No se pudo aplicar el combate clasico a "
                    + jugador.getName() + ": " + e.getMessage());
        }
    }

    private void limpiar(AttributeInstance atributo) {
        if (atributo == null) {
            return;
        }
        for (AttributeModifier modificador : List.copyOf(atributo.getModifiers())) {
            if (clave.equals(modificador.getKey())) {
                atributo.removeModifier(modificador);
            }
        }
    }

    private boolean afectaAlMundo(String mundo) {
        return mundos.isEmpty() || mundos.contains(mundo.toLowerCase(Locale.ROOT));
    }
}
