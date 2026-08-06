package org.metamechanists.odysseia.listeners;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.metamechanists.odysseia.deaths.DeathContext;
import org.metamechanists.odysseia.deaths.DeathMessageCatalog;
import org.metamechanists.odysseia.deaths.DeathStreakTracker;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Cambia los mensajes de muerte de vanilla por otros con gracia.
 *
 * El mensaje de vanilla acierta en una cosa: es **especifico**. "hit the ground too hard while
 * trying to escape SUPER Infernal ENDERMAN" cuenta una historia entera. Lo que le falta es
 * chispa. Asi que aqui no se pierde informacion: quien te mato, con que, y donde siguen dentro
 * del mensaje mediante marcadores; solo cambia el tono.
 *
 * Corre en {@code HIGH}, antes del {@code MONITOR} de {@link PresenceEventListener}, para que el
 * mensaje que se reenvia a Discord sea ya el bueno y no el de vanilla.
 *
 * Los textos viven en {@code muertes.yml}, aparte del config principal, porque Odysseia reescribe
 * {@code config.yml} al arrancar y se lleva los comentarios por delante.
 */
public final class DeathMessageListener implements Listener {

    private static final String ARCHIVO = "muertes.yml";

    private final JavaPlugin plugin;
    private boolean enabled;
    private DeathMessageCatalog catalogo;
    private DeathStreakTracker rachas;
    private int rachaMinima;
    private List<String> coletillasRacha = List.of();

    public DeathMessageListener(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    /** Relee muertes.yml. Se llama al arrancar y desde /odysseia reload. */
    public void reload() {
        File archivo = new File(plugin.getDataFolder(), ARCHIVO);
        if (!archivo.exists()) plugin.saveResource(ARCHIVO, false);
        FileConfiguration datos = YamlConfiguration.loadConfiguration(archivo);

        enabled = datos.getBoolean("enabled", true);
        rachaMinima = Math.max(2, datos.getInt("racha.minimo", 3));
        rachas = new DeathStreakTracker(Math.max(1, datos.getInt("racha.ventana-minutos", 10)) * 60_000L);
        coletillasRacha = datos.getStringList("racha.coletillas");

        Map<String, List<String>> grupos = new HashMap<>();
        var seccion = datos.getConfigurationSection("mensajes");
        if (seccion != null) {
            for (String clave : seccion.getKeys(false)) {
                grupos.put(clave.toLowerCase(java.util.Locale.ROOT), seccion.getStringList(clave));
            }
        }
        catalogo = new DeathMessageCatalog(grupos);
        plugin.getLogger().info("[Muertes] " + catalogo.grupos() + " grupos de mensajes cargados"
                + (enabled ? "" : " (desactivados)") + ".");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        if (!enabled || event.deathMessage() == null) return;

        Player muerto = event.getEntity();
        DeathContext contexto = describir(muerto);
        String plantilla = catalogo.elegir(contexto.clave(), ThreadLocalRandom.current());
        // Sin mensaje para esa causa se deja el de vanilla, que al menos informa.
        if (plantilla == null) return;

        StringBuilder texto = new StringBuilder(contexto.aplicar(plantilla));

        int seguidas = rachas.registrar(muerto.getUniqueId(), System.currentTimeMillis());
        if (seguidas >= rachaMinima && !coletillasRacha.isEmpty()) {
            String coletilla = coletillasRacha.get(
                    ThreadLocalRandom.current().nextInt(coletillasRacha.size()));
            texto.append(" ").append(coletilla.replace("{veces}", String.valueOf(seguidas)));
        }

        event.deathMessage(LegacyComponentSerializer.legacySection()
                .deserialize(ChatColor.translateAlternateColorCodes('&', texto.toString())));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        rachas.olvidar(event.getPlayer().getUniqueId());
    }

    /** Traduce la muerte de Bukkit a los datos planos que necesitan los mensajes. */
    private DeathContext describir(Player muerto) {
        EntityDamageEvent ultimo = muerto.getLastDamageCause();
        String causa = ultimo == null ? "" : ultimo.getCause().name();

        Entity responsable = culpable(ultimo);
        String asesino = responsable == null ? "" : nombreVisible(responsable);

        boolean pvp = responsable instanceof Player && !responsable.equals(muerto);
        boolean jefe = responsable != null && esJefe(responsable);
        boolean propia = responsable != null && esSuyo(responsable, muerto);

        ItemStack enMano = muerto.getKiller() == null
                ? null : muerto.getKiller().getInventory().getItemInMainHand();
        String arma = enMano == null || enMano.getType().isAir() ? "" : nombreArma(enMano);

        var sitio = muerto.getLocation();
        return new DeathContext(muerto.getName(), asesino, arma,
                sitio.getWorld() == null ? "" : sitio.getWorld().getName(),
                sitio.getBlockY(), causa, pvp, jefe, propia);
    }

    /** Quien causo el daño, siguiendo la flecha o la bola de fuego hasta quien la lanzo. */
    private static Entity culpable(EntityDamageEvent evento) {
        if (!(evento instanceof org.bukkit.event.entity.EntityDamageByEntityEvent porEntidad)) return null;
        Entity dañador = porEntidad.getDamager();
        if (dañador instanceof Projectile proyectil) {
            ProjectileSource origen = proyectil.getShooter();
            if (origen instanceof Entity lanzador) return lanzador;
        }
        return dañador;
    }

    /** El nombre que ve el jugador: el personalizado si lo tiene, que es el que da la gracia. */
    private static String nombreVisible(Entity entidad) {
        if (entidad.customName() != null) {
            return LegacyComponentSerializer.legacySection().serialize(entidad.customName());
        }
        if (entidad instanceof Player jugador) return jugador.getName();
        return bonito(entidad.getType().name());
    }

    /** True si la entidad es uno de los jefes de Odysseia. */
    private static boolean esJefe(Entity entidad) {
        return entidad instanceof LivingEntity vivo
                && org.metamechanists.odysseia.boss.BossFaction.esAliado(vivo);
    }

    /** True si lo mato algo suyo: su mascota, o el mismo. */
    private static boolean esSuyo(Entity responsable, Player muerto) {
        if (responsable.equals(muerto)) return true;
        return responsable instanceof Tameable domesticado
                && domesticado.isTamed()
                && muerto.equals(domesticado.getOwner());
    }

    private static String nombreArma(ItemStack item) {
        var meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName() && meta.displayName() != null) {
            return LegacyComponentSerializer.legacySection().serialize(meta.displayName());
        }
        return bonito(item.getType().name());
    }

    /** ZOMBIE_VILLAGER -> Zombie Villager. */
    private static String bonito(String bruto) {
        List<String> palabras = new ArrayList<>();
        for (String parte : bruto.toLowerCase(java.util.Locale.ROOT).split("_")) {
            if (parte.isEmpty()) continue;
            palabras.add(Character.toUpperCase(parte.charAt(0)) + parte.substring(1));
        }
        return String.join(" ", palabras);
    }
}
