package org.metamechanists.odysseia.dragon;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.metamechanists.odysseia.Odysseia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Monturas dracónicas privadas con selección y vuelo protegido. */
public class DragonMountService implements CommandExecutor, TabCompleter, Listener {

    private static final String SELECTOR_TITLE = ChatColor.DARK_PURPLE + "Elige tu dragón";
    private static final List<String> BREATH_TYPES = List.of("fuego", "rayos", "arboles", "estrellas", "hielo", "vacío");
    private static final TreeType[] TREE_TYPES = {
        TreeType.TREE, TreeType.BIRCH, TreeType.BIG_TREE, TreeType.JUNGLE,
        TreeType.ACACIA, TreeType.DARK_OAK, TreeType.CHERRY, TreeType.AZALEA
    };

    private final Odysseia plugin;
    private final Random random = new Random();
    private final Map<UUID, FlightSession> activeFlights = new HashMap<>();
    private final Map<UUID, Double> playerSpeeds = new HashMap<>();
    private final Map<UUID, String> playerBreaths = new HashMap<>();
    private final Map<Inventory, Map<Integer, DragonVariant>> selectors =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private BukkitTask flightTask;

    public DragonMountService(Odysseia plugin) {
        this.plugin = plugin;
        startFlightLoop();
    }

    private void startFlightLoop() {
        flightTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (activeFlights.isEmpty()) return;

            for (Map.Entry<UUID, FlightSession> entry : new ArrayList<>(activeFlights.entrySet())) {
                Player player = Bukkit.getPlayer(entry.getKey());
                FlightSession session = entry.getValue();
                if (!isFlightValid(player, session)) {
                    if (activeFlights.remove(entry.getKey(), session)) cleanupFlight(player, session);
                    continue;
                }

                updateFlight(player, session);
            }
        }, 1L, 1L);
    }

    private boolean isFlightValid(Player player, FlightSession session) {
        return player != null
                && player.isOnline()
                && session.dragon() != null
                && !session.dragon().isDead()
                && session.carrier() != null
                && !session.carrier().isDead()
                && session.carrier().getPassengers().contains(player);
    }

    /** Moves the invisible carrier and keeps the visual dragon detached from passenger physics. */
    private void updateFlight(Player player, FlightSession session) {
        Location current = session.carrier().getLocation();
        Vector direction = player.getEyeLocation().getDirection().normalize();
        double speed = DragonFlightPolicy.clampSpeed(playerSpeeds.getOrDefault(
                player.getUniqueId(), session.variant().defaultSpeed()));
        Location target = current.clone().add(direction.multiply(speed));
        World world = target.getWorld();
        if (world == null) return;

        double safeY = DragonFlightPolicy.clampY(target.getY(), world.getMinHeight(), world.getMaxHeight());
        target.setY(safeY);
        int chunkX = target.getBlockX() >> 4;
        int chunkZ = target.getBlockZ() >> 4;

        if (!world.getWorldBorder().isInside(target)) {
            session.carrier().setVelocity(new Vector());
            player.sendActionBar(ChatColor.RED + "Límite del mundo alcanzado");
            return;
        }
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            session.carrier().setVelocity(new Vector());
            if (session.requestChunk(chunkX, chunkZ)) {
                world.getChunkAtAsync(chunkX, chunkZ, true);
            }
            player.sendActionBar(ChatColor.YELLOW + "Cargando ruta segura...");
            return;
        }
        session.clearPendingChunk();

        target.setYaw(player.getEyeLocation().getYaw());
        target.setPitch(player.getEyeLocation().getPitch());
        if (!session.carrier().teleport(target)) {
            recoverPlayer(player, session);
            return;
        }
        session.setLastSafe(target);

        Location visual = target.clone();
        visual.setYaw(target.getYaw() + 180F);
        session.dragon().teleport(visual);
        spawnTrail(session.variant(), visual);
    }

    private void spawnTrail(DragonVariant variant, Location location) {
        World world = location.getWorld();
        if (world == null) return;
        if (variant == DragonVariant.KIKA) {
            world.spawnParticle(Particle.HAPPY_VILLAGER, location, 8, 0.6, 0.4, 0.6, 0.05);
            world.spawnParticle(Particle.COMPOSTER, location, 5, 0.5, 0.3, 0.5, 0.02);
        } else {
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, location, 8, 0.8, 0.5, 0.8, 0.05);
            world.spawnParticle(Particle.DRAGON_BREATH, location, 6, 0.8, 0.5, 0.8, 0.02);
            world.spawnParticle(Particle.END_ROD, location, 4, 0.5, 0.5, 0.5, 0.05);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo un jugador puede usar este comando.");
            return true;
        }
        if (!canUseAnyDragon(player)) {
            player.sendMessage(ChatColor.RED + "Reservado exclusivamente para JackStar6677 y Kika.");
            return true;
        }

        if (args.length > 0) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("jack") || sub.equals("supremo")) {
                summonSelected(player, DragonVariant.JACK);
                return true;
            }
            if (sub.equals("kika") || sub.equals("esmeralda")) {
                summonSelected(player, DragonVariant.KIKA);
                return true;
            }
            if (sub.equals("speed") || sub.equals("velocidad")) {
                updateSpeed(player, args);
                return true;
            }
            if (sub.equals("aliento") || sub.equals("breath") || sub.equals("element")) {
                updateBreath(player, args);
                return true;
            }
        }

        FlightSession existing = activeFlights.remove(player.getUniqueId());
        if (existing != null) {
            cleanupFlight(player, existing);
            player.sendMessage(ChatColor.YELLOW + "Has desmontado de tu dragón.");
            return true;
        }

        boolean jack = canUse(player, DragonVariant.JACK);
        boolean kika = canUse(player, DragonVariant.KIKA);
        if (jack && kika) {
            openSelector(player);
        } else {
            summon(player, jack ? DragonVariant.JACK : DragonVariant.KIKA);
        }
        return true;
    }

    private void updateSpeed(Player player, String[] args) {
        DragonVariant variant = activeFlights.containsKey(player.getUniqueId())
                ? activeFlights.get(player.getUniqueId()).variant() : DragonVariant.JACK;
        if (args.length < 2) {
            player.sendMessage(ChatColor.GOLD + "Velocidad actual: "
                    + playerSpeeds.getOrDefault(player.getUniqueId(), variant.defaultSpeed()));
            player.sendMessage(ChatColor.YELLOW + "Uso seguro: /dragon speed <0.5-1.5>");
            return;
        }
        try {
            double requested = Double.parseDouble(args[1]);
            double speed = DragonFlightPolicy.clampSpeed(requested);
            playerSpeeds.put(player.getUniqueId(), speed);
            player.sendMessage(ChatColor.GREEN + "Velocidad segura establecida en " + ChatColor.AQUA + speed + "x");
            if (speed != requested) {
                player.sendMessage(ChatColor.YELLOW + "El valor fue limitado para evitar expulsiones y caídas del mundo.");
            }
        } catch (NumberFormatException ex) {
            player.sendMessage(ChatColor.RED + "Ingresa un número entre 0.5 y 1.5.");
        }
    }

    private void updateBreath(Player player, String[] args) {
        DragonVariant variant = activeFlights.containsKey(player.getUniqueId())
                ? activeFlights.get(player.getUniqueId()).variant() : DragonVariant.JACK;
        if (args.length < 2) {
            player.sendMessage(ChatColor.GOLD + "Aliento actual: "
                    + playerBreaths.getOrDefault(player.getUniqueId(), variant.defaultBreath()));
            player.sendMessage(ChatColor.YELLOW + "Opciones: " + String.join(", ", BREATH_TYPES));
            return;
        }
        String breath = args[1].toLowerCase(Locale.ROOT);
        if (!BREATH_TYPES.contains(breath)) {
            player.sendMessage(ChatColor.RED + "Aliento no válido. Opciones: " + String.join(", ", BREATH_TYPES));
            return;
        }
        playerBreaths.put(player.getUniqueId(), breath);
        player.sendMessage(ChatColor.GREEN + "Aliento cambiado a " + ChatColor.LIGHT_PURPLE + breath.toUpperCase(Locale.ROOT));
    }

    private void openSelector(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, SELECTOR_TITLE);
        ItemStack filler = namedItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler);
        inventory.setItem(11, namedItem(Material.DRAGON_HEAD, "&d&lDragón Supremo de Jack",
                List.of("&7Aliento inicial: fuego", "&7Vuelo poderoso y estable", "&eClic para montar")));
        inventory.setItem(15, namedItem(Material.EMERALD, "&a&lDragón Esmeralda de Kika",
                List.of("&7Aliento inicial: árboles", "&7Vuelo ágil y estable", "&eClic para montar")));
        selectors.put(inventory, Map.of(11, DragonVariant.JACK, 15, DragonVariant.KIKA));
        player.openInventory(inventory);
    }

    private ItemStack namedItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            meta.setLore(lore.stream().map(this::color).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    @EventHandler
    public void onSelectorClick(InventoryClickEvent event) {
        Map<Integer, DragonVariant> options = selectors.get(event.getInventory());
        if (options == null) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        DragonVariant selected = options.get(event.getRawSlot());
        if (selected == null) return;
        player.closeInventory();
        summonSelected(player, selected);
    }

    @EventHandler
    public void onSelectorClose(InventoryCloseEvent event) {
        selectors.remove(event.getInventory());
    }

    private void summonSelected(Player player, DragonVariant variant) {
        if (!canUse(player, variant)) {
            player.sendMessage(ChatColor.RED + "No tienes acceso a ese dragón.");
            return;
        }
        FlightSession previous = activeFlights.remove(player.getUniqueId());
        if (previous != null) cleanupFlight(player, previous);
        summon(player, variant);
    }

    /** Creates a stable carrier and a separate visual dragon. */
    private void summon(Player player, DragonVariant variant) {
        Location origin = player.getLocation().clone();
        World world = origin.getWorld();
        if (world == null) return;

        Interaction carrier = (Interaction) world.spawnEntity(origin, EntityType.INTERACTION);
        carrier.setInteractionWidth(1.0F);
        carrier.setInteractionHeight(1.0F);
        carrier.setGravity(false);
        carrier.setInvulnerable(true);
        carrier.setPersistent(false);

        EnderDragon dragon = (EnderDragon) world.spawnEntity(origin, EntityType.ENDER_DRAGON);
        dragon.setAI(false);
        dragon.setInvulnerable(true);
        dragon.setSilent(true);
        dragon.setCollidable(false);
        dragon.setPersistent(false);
        dragon.setPhase(EnderDragon.Phase.HOVER);

        var scale = dragon.getAttribute(Attribute.SCALE);
        if (scale != null) scale.setBaseValue(variant.scale());
        dragon.setCustomName(color(variant.displayName(player.getName())));
        dragon.setCustomNameVisible(true);

        FlightSession session = new FlightSession(variant, dragon, carrier, origin);
        activeFlights.put(player.getUniqueId(), session);
        playerBreaths.putIfAbsent(player.getUniqueId(), variant.defaultBreath());
        if (!carrier.addPassenger(player)) {
            activeFlights.remove(player.getUniqueId());
            cleanupFlight(player, session);
            player.sendMessage(ChatColor.RED + "No se pudo iniciar el vuelo. Inténtalo de nuevo en suelo firme.");
            return;
        }
        player.playSound(origin, Sound.ENTITY_ENDER_DRAGON_GROWL, variant.volume(), variant.pitch());
        player.sendMessage(color(variant.summonMessage()));
    }

    private boolean canUseAnyDragon(Player player) {
        return canUse(player, DragonVariant.JACK) || canUse(player, DragonVariant.KIKA);
    }

    private boolean canUse(Player player, DragonVariant variant) {
        String name = player.getName().toLowerCase(Locale.ROOT);
        return switch (variant) {
            case JACK -> name.contains("jackstar") || player.hasPermission("odysseia.dragon.owner")
                    || player.hasPermission("odysseia.dragon.use");
            case KIKA -> name.contains("kika") || player.hasPermission("odysseia.dragon.kika");
        };
    }

    /** Executes the selected breath on left click. */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        FlightSession session = activeFlights.get(player.getUniqueId());
        if (session == null) return;
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        String breath = playerBreaths.getOrDefault(player.getUniqueId(), session.variant().defaultBreath());
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection();
        switch (breath) {
            case "arboles" -> growTree(player);
            case "rayos" -> strikeLightning(player, eye, direction);
            case "estrellas" -> {
                eye.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, eye.clone().add(direction.clone().multiply(2)), 40, 0.8, 0.8, 0.8, 0.1);
                eye.getWorld().spawnParticle(Particle.FIREWORK, eye.clone().add(direction.clone().multiply(2)), 20, 0.5, 0.5, 0.5, 0.1);
                player.playSound(eye, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.5f, 1.4f);
            }
            case "hielo" -> particleBeam(eye, direction, Particle.SNOWFLAKE, 0.05);
            case "vacío" -> particleBeam(eye, direction, Particle.DRAGON_BREATH, 0.02);
            default -> {
                DragonFireball fireball = (DragonFireball) eye.getWorld().spawnEntity(
                        eye.clone().add(direction.clone().multiply(1.8)), EntityType.DRAGON_FIREBALL);
                fireball.setDirection(direction.multiply(1.8));
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.8f, 0.8f);
            }
        }
    }

    private void growTree(Player player) {
        org.bukkit.block.Block target = player.getTargetBlockExact(50);
        if (target == null || target.getType() == Material.AIR) return;
        Location location = target.getLocation().add(0, 1, 0);
        location.getWorld().generateTree(location, TREE_TYPES[random.nextInt(TREE_TYPES.length)]);
        location.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, location, 40, 1, 1, 1, 0.1);
        player.playSound(player.getLocation(), Sound.ITEM_BONE_MEAL_USE, 1.5f, 1.2f);
    }

    private void strikeLightning(Player player, Location eye, Vector direction) {
        org.bukkit.block.Block target = player.getTargetBlockExact(60);
        Location location = target != null ? target.getLocation() : eye.clone().add(direction.multiply(30));
        location.getWorld().strikeLightningEffect(location);
        location.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, location, 1, 0, 0, 0, 0);
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.8f, 0.8f);
    }

    private void particleBeam(Location eye, Vector direction, Particle particle, double extra) {
        for (double distance = 1; distance <= 20; distance++) {
            Location point = eye.clone().add(direction.clone().multiply(distance));
            point.getWorld().spawnParticle(particle, point, 15, 0.4, 0.4, 0.4, extra);
        }
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        FlightSession session = activeFlights.get(player.getUniqueId());
        if (session == null || !event.getDismounted().getUniqueId().equals(session.carrier().getUniqueId())) return;
        activeFlights.remove(player.getUniqueId());
        cleanupFlight(player, session);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        FlightSession session = activeFlights.remove(event.getPlayer().getUniqueId());
        if (session != null) cleanupFlight(event.getPlayer(), session);
    }

    private void cleanupFlight(Player player, FlightSession session) {
        if (session == null) return;
        if (session.carrier() != null && !session.carrier().isDead()) session.carrier().remove();
        if (session.dragon() != null && !session.dragon().isDead()) session.dragon().remove();
        if (player != null && player.isOnline()) {
            recoverPlayer(player, session);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 120, 0, false, false));
        }
    }

    /** Returns a player to the last valid position if passenger physics left world bounds. */
    private void recoverPlayer(Player player, FlightSession session) {
        Location current = player.getLocation();
        World world = current.getWorld();
        if (world == null) return;
        boolean invalidY = current.getY() < world.getMinHeight() + 2 || current.getY() > world.getMaxHeight() - 2;
        if (invalidY || !world.getWorldBorder().isInside(current)) {
            player.teleport(session.lastSafe());
        }
    }

    public void shutdown() {
        if (flightTask != null) flightTask.cancel();
        for (FlightSession session : new ArrayList<>(activeFlights.values())) cleanupFlight(null, session);
        activeFlights.clear();
        selectors.clear();
        playerSpeeds.clear();
        playerBreaths.clear();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("jack", "kika", "speed", "aliento").stream()
                    .filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("speed") || args[0].equalsIgnoreCase("velocidad"))) {
            return List.of("0.5", "0.8", "1.0", "1.2", "1.5");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("aliento") || args[0].equalsIgnoreCase("breath"))) {
            return BREATH_TYPES.stream().filter(value -> value.startsWith(args[1].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }

    private enum DragonVariant {
        JACK(1.4, 0.8, "fuego"),
        KIKA(0.65, 0.7, "arboles");

        private final double scale;
        private final double defaultSpeed;
        private final String defaultBreath;

        DragonVariant(double scale, double defaultSpeed, String defaultBreath) {
            this.scale = scale;
            this.defaultSpeed = defaultSpeed;
            this.defaultBreath = defaultBreath;
        }

        double scale() { return scale; }
        double defaultSpeed() { return defaultSpeed; }
        String defaultBreath() { return defaultBreath; }
        float volume() { return this == KIKA ? 1.2F : 2.0F; }
        float pitch() { return this == KIKA ? 1.8F : 0.6F; }
        String displayName(String player) {
            return this == KIKA ? "&a&lDragón Esmeralda Cute de " + player : "&d&lDragón Supremo de " + player;
        }
        String summonMessage() {
            return this == KIKA
                    ? "&aDragón Esmeralda invocado. Clic izquierdo activa el aliento."
                    : "&dDragón Supremo invocado. Clic izquierdo activa el aliento.";
        }
    }

    private static final class FlightSession {
        private final DragonVariant variant;
        private final EnderDragon dragon;
        private final Interaction carrier;
        private Location lastSafe;
        private long pendingChunk = Long.MIN_VALUE;

        private FlightSession(DragonVariant variant, EnderDragon dragon, Interaction carrier, Location lastSafe) {
            this.variant = variant;
            this.dragon = dragon;
            this.carrier = carrier;
            this.lastSafe = lastSafe.clone();
        }

        DragonVariant variant() { return variant; }
        EnderDragon dragon() { return dragon; }
        Interaction carrier() { return carrier; }
        Location lastSafe() { return lastSafe.clone(); }
        void setLastSafe(Location location) { this.lastSafe = location.clone(); }
        boolean requestChunk(int chunkX, int chunkZ) {
            long key = ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
            if (pendingChunk == key) return false;
            pendingChunk = key;
            return true;
        }
        void clearPendingChunk() { pendingChunk = Long.MIN_VALUE; }
    }
}
