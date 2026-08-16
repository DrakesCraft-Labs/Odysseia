package org.metamechanists.odysseia.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.metamechanists.odysseia.Odysseia;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Prende a los muertos vivientes que deberian arder de dia y no lo hacen.
 *
 * Los jugadores reportaron que zombies y phantoms sobreviven al amanecer a cielo abierto. La
 * combustion de vanilla depende de la hora del mundo, y este servidor tiene el ciclo alterado
 * ({@code daylight-cycle-ticks} en purpur.yml) ademas de plugins que manipulan la hora, asi que
 * el juego deja de considerar que es de dia y nunca los enciende.
 *
 * En vez de tocar el ciclo --que afectaria a granjas y al ritmo del servidor entero-- se
 * comprueba aqui la condicion real: cielo despejado sobre el mob y luz de sol suficiente.
 *
 * El coste esta acotado a proposito, porque el hilo principal ya va justo: se revisa cada varios
 * segundos, solo alrededor de jugadores conectados, y se sale de inmediato si es de noche.
 */
public final class SunlightBurnListener implements Listener {

    /** Los que arden al sol en vanilla. Husk y zombie de momia estan fuera porque son inmunes. */
    private static final Set<EntityType> ARDEN = Set.of(
            EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, EntityType.DROWNED,
            EntityType.SKELETON, EntityType.STRAY, EntityType.PHANTOM);

    private final Odysseia plugin;

    public SunlightBurnListener(Odysseia plugin) {
        this.plugin = plugin;
        programar();
    }

    private void programar() {
        long intervalo = Math.max(20L, plugin.getConfig().getLong("quemar-al-sol.intervalo-ticks", 60L));
        Bukkit.getScheduler().runTaskTimer(plugin, this::revisar, intervalo, intervalo);
    }

    private void revisar() {
        if (!plugin.getConfig().getBoolean("quemar-al-sol.enabled", true)) return;

        int radio = Math.max(8, plugin.getConfig().getInt("quemar-al-sol.radio", 48));
        int segundos = Math.max(1, plugin.getConfig().getInt("quemar-al-sol.segundos-de-fuego", 8));
        List<String> mundos = plugin.getConfig().getStringList("quemar-al-sol.mundos");

        for (Player jugador : Bukkit.getOnlinePlayers()) {
            World mundo = jugador.getWorld();
            if (!mundos.isEmpty() && mundos.stream().noneMatch(m -> m.equalsIgnoreCase(mundo.getName()))) {
                continue;
            }
            if (!esDeDia(mundo)) continue;

            for (Entity entidad : jugador.getNearbyEntities(radio, radio, radio)) {
                if (!ARDEN.contains(entidad.getType()) || !(entidad instanceof LivingEntity mob)) continue;
                if (mob.isDead() || mob.getFireTicks() > 0) continue;
                if (!expuestoAlSol(mob)) continue;
                if (llevaCasco(mob)) continue;

                mob.setFireTicks(segundos * 20);
            }
        }
    }

    /**
     * Si el mundo esta en horas de sol.
     *
     * Se mira el rango de vanilla y no {@code isDayTime()}, porque ese metodo depende del ciclo
     * configurado y es justo lo que aqui esta alterado.
     */
    private static boolean esDeDia(World mundo) {
        long hora = mundo.getTime() % 24000L;
        return hora < 12300L || hora > 23850L;
    }

    /** Cielo despejado encima y luz de sol directa, que es lo que exige vanilla para encender. */
    private static boolean expuestoAlSol(LivingEntity mob) {
        var bloque = mob.getLocation().getBlock();
        return bloque.getLightFromSky() >= 15
                && mob.getWorld().getHighestBlockYAt(mob.getLocation()) <= bloque.getY()
                && !mob.getLocation().getBlock().isLiquid();
    }

    /** Un casco protege del sol en vanilla, aunque sea de cuero. */
    private static boolean llevaCasco(LivingEntity mob) {
        EntityEquipment equipo = mob.getEquipment();
        if (equipo == null) return false;
        ItemStack casco = equipo.getHelmet();
        return casco != null && casco.getType() != Material.AIR;
    }

    /** Nombre legible para el log de arranque. */
    public static String resumen() {
        return ARDEN.stream()
                .map(tipo -> tipo.name().toLowerCase(Locale.ROOT))
                .sorted()
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }
}
