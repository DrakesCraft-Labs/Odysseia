package org.metamechanists.odysseia.events;

import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.events.boost.ServerBoostManager;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.*;

/**
 * Gestor Autónomo de Eventos Estacionales del Multiverso y Ciclos Divinos de DrakesCraft.
 *
 * Módulos integrados:
 * 1. OCTUBRE (Halloween / Sombras del Tártaro):
 *    - Noche perpetua en el Overworld (tiempo fijado en 18000 ticks).
 *    - Descanso seguro en camas (evita phantoms reseteando TIME_SINCE_REST) sin avanzar el ciclo al día (TimeSkipEvent cancelado).
 *    - Emisión periódica de susurros y lamentos del Tártaro con ambientación sónica.
 *    - Mecánicas exclusivas para el Kit Sombras del Tártaro e Inmunidad a Radiación / Fuego / Lava.
 * 2. 29 DE NOVIEMBRE (Cumpleaños del Creador - JackStar):
 *    - 5X Economía, 5X EXP y 5X Dracmas activos 24h.
 *    - Bendición Divina (Haste X + Fuerza V + Velocidad II) para toda la comunidad.
 * 3. DICIEMBRE (Solsticio Invernal & Navidad):
 *    - Impulsos festivos y atmósfera de celebración.
 * 4. FEBRERO (Furia de Afrodita / San Valentín):
 *    - Bendiciones de amor y efectos de regeneración.
 */
public class SeasonalEventManager implements Listener {

    private final JavaPlugin plugin;
    private final ServerBoostManager boostManager;
    private final String discordWebhookUrl;
    private final ZoneId zone;
    private final Random random = new Random();

    private BukkitTask tickTask;
    private long lastAtmosphericBroadcast = 0;
    private long lastBirthdayBroadcast = 0;

    // Moduladores administrativos (null = ciclo automático de calendario)
    private Boolean forceHalloween = null;
    private Boolean forceBirthday = null;
    private Boolean forceWinter = null;
    private Boolean forceValentines = null;

    private static final List<String> HALLOWEEN_MESSAGES = List.of(
            "&4&l✦ [TÁRTARO] &cLa niebla del inframundo se densifica. Los espíritus de los antiguos guerreros susurran entre las sombras...",
            "&4&l✦ [TÁRTARO] &cUn escalofrío cósmico recorre DrakesCraft. En octubre, la luz del sol ha sido desterrada de este reino.",
            "&4&l✦ [TÁRTARO] &cEl barquero Caronte navega por los ríos subterráneos. La noche eterna no concede tregua a los mortales.",
            "&4&l✦ [TÁRTARO] &cLas cadenas que aprisionan a los Titanes vibran bajo la roca madre. Descansa si puedes, pero el alba no llegará.",
            "&4&l✦ [TÁRTARO] &cLos dioses del Olimpo han sellado los cielos. Sólo la oscuridad ancestral custodia tus dominios.",
            "&4&l✦ [TÁRTARO] &c\"Aquellos que temen a la noche aún no han visto lo que duerme en el foso más profundo...\"",
            "&4&l✦ [TÁRTARO] &cUn relámpago fantasmal tiñe las nubes de púrpura. La luna menguante vigila cada uno de tus pasos."
    );

    public SeasonalEventManager(JavaPlugin plugin, ServerBoostManager boostManager, String discordWebhookUrl) {
        this.plugin = plugin;
        this.boostManager = boostManager;
        this.discordWebhookUrl = discordWebhookUrl;
        this.zone = configuredZone();
    }

    public void start() {
        if (tickTask != null) tickTask.cancel();
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Tick cada 20 ticks (1 segundo)
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        plugin.getLogger().info("[SeasonalEvents] Gestor de eventos estacionales de calendario activado (" + zone.getId() + ").");
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    public void tick() {
        long now = System.currentTimeMillis();

        // 1. MÓDULO HALLOWEEN (OCTUBRE)
        if (isHalloweenActive()) {
            // Noche perpetua en mundos normales
            for (World world : Bukkit.getWorlds()) {
                if (world.getEnvironment() == World.Environment.NORMAL) {
                    if (world.getTime() < 13000 || world.getTime() > 23000) {
                        world.setTime(18000L); // Medianoche exacta
                    }
                }
            }

            // Emisión atmosférica cada 12 minutos (720.000 ms)
            if (now - lastAtmosphericBroadcast >= 720_000L && !Bukkit.getOnlinePlayers().isEmpty()) {
                lastAtmosphericBroadcast = now;
                String msg = ChatColor.translateAlternateColorCodes('&',
                        HALLOWEEN_MESSAGES.get(random.nextInt(HALLOWEEN_MESSAGES.size())));
                Bukkit.broadcastMessage(msg);

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 1.2f, 0.4f);
                    p.playSound(p.getLocation(), Sound.BLOCK_BELL_RESONATE, 0.9f, 0.4f);
                }
            }
        }

        // 2. MÓDULO CUMPLEAÑOS DEL CREADOR (29 DE NOVIEMBRE)
        if (isBirthdayActive()) {
            if (boostManager != null) {
                if (!boostManager.hasActiveBoost(ServerBoostManager.BoostCategory.ECONOMIA)) {
                    boostManager.startBoost(ServerBoostManager.BoostCategory.ECONOMIA, 5.0, 86400L);
                }
                if (!boostManager.hasActiveBoost(ServerBoostManager.BoostCategory.XP)) {
                    boostManager.startBoost(ServerBoostManager.BoostCategory.XP, 5.0, 86400L);
                }
                if (!boostManager.hasActiveBoost(ServerBoostManager.BoostCategory.DRACMAS)) {
                    boostManager.startBoost(ServerBoostManager.BoostCategory.DRACMAS, 5.0, 86400L);
                }
            }

            // Bendición divina a jugadores en línea
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 400, 9, false, false, true)); // Haste X
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 400, 4, false, false, true)); // Fuerza V
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 1, false, false, true)); // Velocidad II
            }

            // Anuncio periódico cada 2 horas
            if (now - lastBirthdayBroadcast >= 7200_000L && !Bukkit.getOnlinePlayers().isEmpty()) {
                lastBirthdayBroadcast = now;
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                        "&6&l👑 [CUMPLEAÑOS DEL CREADOR] &e¡Hoy celebramos el natalicio de &6Jack (JackStar)&e, Arquitecto & Creador Soberano de DrakesCraft y Star!"));
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                        "&a✦ 5X Economía, 5X EXP, 5X Dracmas y Bendición Divina (Haste X + Fuerza V) activos durante todo el día."));

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                }
            }
        }
    }

    // ==========================================
    // LISTENERS DE MECÁNICAS DE HALLOWEEN
    // ==========================================

    /**
     * Cancela el salto de tiempo cuando los jugadores duermen en la cama.
     * En Halloween la noche es perpetua y el amanecer nunca debe llegar.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTimeSkip(TimeSkipEvent event) {
        if (!isHalloweenActive()) return;
        if (event.getSkipReason() == TimeSkipEvent.SkipReason.NIGHT_SKIP) {
            event.setCancelled(true);
        }
    }

    /**
     * Permite dormir para resetear el insomnio (evita que aparezcan Phantoms)
     * y acompaña al jugador con la inmersión de la Noche Perpetua.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBedLeave(PlayerBedLeaveEvent event) {
        if (!isHalloweenActive()) return;
        Player player = event.getPlayer();

        // Resetea el contador de insomnio para que el jugador no sufra phantoms
        try {
            player.setStatistic(Statistic.TIME_SINCE_REST, 0);
        } catch (Throwable ignored) {}

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8[&4&lTÁRTARO&8] &7Dormiste bajo el firmamento eterno de Halloween. Tus ojos descansan, pero la noche no se desvanece..."));
        player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 1.2f, 0.4f);
        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.2f, 0.5f);
    }

    // ==========================================
    // LISTENERS DEL KIT SOMBRAS DEL TÁRTARO
    // ==========================================

    /**
     * Inmunidad al Fuego, Lava y Daño de Radiación de Slimefun para portadores del Tártaro.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTartarusDamageMitigation(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        boolean hasTartarusArmor = isWearingTartarusPiece(player);
        if (!hasTartarusArmor) return;

        EntityDamageEvent.DamageCause cause = event.getCause();

        // Inmunidad a fuego, lava y suelo caliente
        if (cause == EntityDamageEvent.DamageCause.LAVA
                || cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.HOT_FLOOR) {
            event.setCancelled(true);
            player.setFireTicks(0);
            return;
        }

        // Inmunidad a radiación Slimefun (suele manifestarse como daño CUSTOM o POISON/WITHER)
        if (cause == EntityDamageEvent.DamageCause.CUSTOM || cause == EntityDamageEvent.DamageCause.MAGIC) {
            // Verificar si el jugador tiene armadura del Tártaro completa
            if (isWearingFullTartarus(player)) {
                // Si proviene de radiación, mitigamos
                event.setDamage(event.getDamage() * 0.10);
            }
        }
    }

    /**
     * Daño Verdadero (True Damage) de la Guadaña Espectral de Caronte y
     * mitigación defensiva de la Coraza del Castigo Eterno.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTartarusCombat(EntityDamageByEntityEvent event) {
        // 1. ATAQUE CON GUADAÑA DE CARONTE
        if (event.getDamager() instanceof Player attacker) {
            ItemStack weapon = attacker.getInventory().getItemInMainHand();
            if (isCharonScythe(weapon) && event.getEntity() instanceof LivingEntity target) {
                // Daño Verdadero adicional que atraviesa armadura
                double trueDamage = Math.max(4.0, event.getDamage() * 0.40);
                target.damage(trueDamage);

                // Efectos espectrales
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 1));
                Location loc = target.getLocation();
                loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.add(0, 1, 0), 25, 0.4, 0.6, 0.4, 0.05);
                loc.getWorld().spawnParticle(Particle.SCULK_SOUL, loc, 15, 0.3, 0.5, 0.3, 0.02);
                attacker.playSound(loc, Sound.ENTITY_WITHER_HURT, 0.8f, 1.6f);
            }
        }

        // 2. DEFENSA CON CORAZA DEL TÁRTARO
        if (event.getEntity() instanceof Player defender) {
            ItemStack chest = defender.getInventory().getChestplate();
            if (isTartarusItem(chest)) {
                // Mitigación del 25% de daño
                event.setDamage(event.getDamage() * 0.75);

                // 15% probabilidad de contraataque sombrío
                if (random.nextDouble() < 0.15 && event.getDamager() instanceof LivingEntity damager) {
                    damager.setVelocity(damager.getLocation().toVector().subtract(defender.getLocation().toVector()).normalize().multiply(1.2).setY(0.4));
                    damager.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                    defender.getWorld().spawnParticle(Particle.REVERSE_PORTAL, defender.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
                    defender.playSound(defender.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
                }
            }
        }
    }

    private final java.util.concurrent.ConcurrentHashMap<UUID, Long> scytheAbilityCooldowns = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Habilidad Activa con Clic Derecho: Vórtice de Almas del Tártaro.
     * Desata una onda expansiva espectral de 8 bloques que inflige daño verdadero,
     * aplica Wither y otorga Absorción y Velocidad al portador.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onScytheAbility(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isCharonScythe(item)) return;

        long now = System.currentTimeMillis();
        long last = scytheAbilityCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long cd = 20_000L; // 20s de enfriamiento
        if (now - last < cd) {
            long remSecs = (cd - (now - last)) / 1000L + 1L;
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&4[Tártaro] &cLa Guadaña de Caronte está recargando su energía del inframundo (" + remSecs + "s)."));
            return;
        }

        scytheAbilityCooldowns.put(player.getUniqueId(), now);
        Location origin = player.getLocation();
        origin.getWorld().playSound(origin, Sound.ENTITY_WITHER_SPAWN, 1.2f, 1.8f);
        origin.getWorld().playSound(origin, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.7f);

        // Anillo de fuego de alma y partículas de almas
        origin.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, origin.clone().add(0, 1, 0), 60, 2.5, 0.5, 2.5, 0.05);
        origin.getWorld().spawnParticle(Particle.SCULK_SOUL, origin.clone().add(0, 1, 0), 40, 2.0, 1.0, 2.0, 0.03);

        // Buffs al portador
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 300, 1));

        int hitCount = 0;
        for (org.bukkit.entity.Entity e : player.getNearbyEntities(8.0, 4.0, 8.0)) {
            if (e instanceof LivingEntity target && !e.equals(player) && !(e instanceof org.bukkit.entity.ArmorStand)) {
                target.damage(12.0, player); // 12 HP de Daño Verdadero
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 120, 1));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1));
                target.setVelocity(target.getLocation().toVector().subtract(origin.toVector()).normalize().multiply(0.8).setY(0.3));
                hitCount++;
            }
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&4&l✦ [VÓRTICE DEL TÁRTARO] &c¡Has liberado las almas de Caronte dañando a " + hitCount + " enemigos!"));
    }

    // ==========================================
    // UTILIDADES DE DETECCIÓN Y ESTADOS
    // ==========================================

    public boolean isHalloweenActive() {
        if (forceHalloween != null) return forceHalloween;
        LocalDate now = LocalDate.now(zone);
        return now.getMonth() == Month.OCTOBER;
    }

    public boolean isBirthdayActive() {
        if (forceBirthday != null) return forceBirthday;
        LocalDate now = LocalDate.now(zone);
        return now.getMonth() == Month.NOVEMBER && now.getDayOfMonth() == 29;
    }

    public boolean isWinterActive() {
        if (forceWinter != null) return forceWinter;
        LocalDate now = LocalDate.now(zone);
        return now.getMonth() == Month.DECEMBER;
    }

    public boolean isValentinesActive() {
        if (forceValentines != null) return forceValentines;
        LocalDate now = LocalDate.now(zone);
        return now.getMonth() == Month.FEBRUARY;
    }

    public void setForceHalloween(Boolean state) { this.forceHalloween = state; }
    public void setForceBirthday(Boolean state) { this.forceBirthday = state; }
    public void setForceWinter(Boolean state) { this.forceWinter = state; }
    public void setForceValentines(Boolean state) { this.forceValentines = state; }

    private boolean isWearingTartarusPiece(Player player) {
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (isTartarusItem(armor)) return true;
        }
        return false;
    }

    private boolean isWearingFullTartarus(Player player) {
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (!isTartarusItem(armor)) return false;
        }
        return true;
    }

    private boolean isTartarusItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (pdc.has(new NamespacedKey("odysseia", "tartaro_armor"), PersistentDataType.STRING)) return true;

        String name = item.getItemMeta().getDisplayName().toLowerCase(Locale.ROOT);
        return name.contains("tártaro") || name.contains("tartaro") || name.contains("aqueronte") || name.contains("castigo eterno");
    }

    private boolean isCharonScythe(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (pdc.has(new NamespacedKey("odysseia", "charon_scythe"), PersistentDataType.STRING)) return true;

        String name = item.getItemMeta().getDisplayName().toLowerCase(Locale.ROOT);
        return name.contains("guadaña") || name.contains("guadana") || name.contains("caronte");
    }

    private ZoneId configuredZone() {
        try {
            return ZoneId.of(plugin.getConfig().getString("chatgames.timezone", "America/Santiago"));
        } catch (Exception ignored) {
            return ZoneId.of("America/Santiago");
        }
    }
}
