package org.metamechanists.odysseia.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.metamechanists.odysseia.integrations.SlimefunGuideBridge;
import org.metamechanists.odysseia.integrations.SFMasterPassExpiry;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.UUID;
import java.lang.reflect.Method;

public class SFMasterWatcherListener implements Listener {

    private static final String SFMASTER_CHEAT_PERMISSION = "slimefun.cheat.items";
    private static final String SFMASTER_ACTIVE_PERMISSION = "odysseia.sfmaster.active";
    private static final String SFMASTER_BYPASS_PERMISSION = "odysseia.sfmaster.bypass_marking";
    private static final String SFMASTER_MARKER_LORE = "§cGenerado por SFMaster - No comerciable";

    private final Plugin plugin;
    private final NamespacedKey sfMasterKey;
    
    private final File blocksFile;
    private final YamlConfiguration blocksConfig;
    private final Set<String> sfMasterBlocks = new HashSet<>();
    private final Map<String, Long> brokenSFMasterBlocks = new HashMap<>();
    private final Map<UUID, Deque<Long>> claimHistory = new HashMap<>();
    private final Set<String> blockedAddons;
    private final Set<String> blockedIdPrefixes;
    private final Set<String> blockedIdFragments;
    private final Set<String> blockedMaterials;
    private final int maxClaims;
    private final long claimWindowMillis;
    private final Method slimefunGetByItem;
    private final SlimefunGuideBridge slimefunGuide;
    private boolean reflectionWarningLogged;

    public SFMasterWatcherListener(Plugin plugin) {
        this.plugin = plugin;
        this.sfMasterKey = new NamespacedKey(plugin, "sfmaster_item");
        this.blocksFile = new File(plugin.getDataFolder(), "sfmaster_blocks.yml");
        this.blocksConfig = YamlConfiguration.loadConfiguration(blocksFile);
        this.blockedAddons = normalizedConfigSet("sfmaster-policy.blocked-addons");
        this.blockedIdPrefixes = normalizedConfigSet("sfmaster-policy.blocked-id-prefixes");
        this.blockedIdFragments = normalizedConfigSet("sfmaster-policy.blocked-id-fragments");
        this.blockedMaterials = normalizedConfigSet("sfmaster-policy.blocked-materials");
        this.maxClaims = Math.max(1, plugin.getConfig().getInt("sfmaster-policy.max-claims", 12));
        this.claimWindowMillis = Math.max(1, plugin.getConfig().getInt("sfmaster-policy.window-minutes", 60)) * 60_000L;
        this.slimefunGetByItem = findSlimefunGetByItem();
        this.slimefunGuide = new SlimefunGuideBridge(plugin);
        loadBlocks();
    }

    private Set<String> normalizedConfigSet(String path) {
        Set<String> values = new HashSet<>();
        for (String value : plugin.getConfig().getStringList(path)) {
            values.add(value.toUpperCase(Locale.ROOT));
        }
        return values;
    }

    private Method findSlimefunGetByItem() {
        try {
            Class<?> slimefunItem = Class.forName("com.github.drakescraft_labs.slimefun4.api.items.SlimefunItem");
            return slimefunItem.getMethod("getByItem", ItemStack.class);
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().severe("No se pudo enlazar la API de Slimefun para proteger SFMaster: " + exception.getMessage());
            return null;
        }
    }

    private SfItemDescriptor describeSlimefunItem(ItemStack item) {
        if (slimefunGetByItem == null || item == null) {
            return null;
        }

        try {
            Object slimefunItem = slimefunGetByItem.invoke(null, item);
            if (slimefunItem == null) {
                return null;
            }
            String id = String.valueOf(slimefunItem.getClass().getMethod("getId").invoke(slimefunItem));
            Object addon = slimefunItem.getClass().getMethod("getAddon").invoke(slimefunItem);
            String addonName = addon == null ? "" : String.valueOf(addon.getClass().getMethod("getName").invoke(addon));
            return new SfItemDescriptor(id.toUpperCase(Locale.ROOT), addonName.toUpperCase(Locale.ROOT));
        } catch (ReflectiveOperationException exception) {
            if (!reflectionWarningLogged) {
                reflectionWarningLogged = true;
                plugin.getLogger().warning("No se pudo identificar un item de SFMaster: " + exception.getMessage());
            }
            return null;
        }
    }

    private String blockedReason(ItemStack item) {
        String material = item.getType().name();
        if (blockedMaterials.contains(material) || item.getType().getMaxDurability() > 0) {
            return "herramientas, armas, armaduras y materiales de alto valor estan bloqueados";
        }

        SfItemDescriptor descriptor = describeSlimefunItem(item);
        if (descriptor == null) {
            return "el item no pudo validarse de forma segura";
        }
        if (blockedAddons.stream().anyMatch(descriptor.addon()::contains)) {
            return "ese addon esta bloqueado para SFMaster";
        }
        if (blockedIdPrefixes.stream().anyMatch(descriptor.id()::startsWith)
                || blockedIdFragments.stream().anyMatch(descriptor.id()::contains)) {
            return "ese tipo de item esta bloqueado para SFMaster";
        }
        return null;
    }

    private boolean consumeClaim(Player player) {
        long now = System.currentTimeMillis();
        Deque<Long> claims = claimHistory.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>());
        while (!claims.isEmpty() && now - claims.peekFirst() >= claimWindowMillis) {
            claims.removeFirst();
        }
        if (claims.size() >= maxClaims) {
            return false;
        }
        claims.addLast(now);
        return true;
    }

    private void loadBlocks() {
        sfMasterBlocks.clear();
        List<String> list = blocksConfig.getStringList("blocks");
        if (list != null) {
            sfMasterBlocks.addAll(list);
        }
    }

    private void saveBlocks() {
        blocksConfig.set("blocks", new ArrayList<>(sfMasterBlocks));
        try {
            blocksConfig.save(blocksFile);
        } catch (IOException e) {
            plugin.getLogger().severe("No se pudo guardar sfmaster_blocks.yml: " + e.getMessage());
        }
    }

    private boolean isSFMasterItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(sfMasterKey, PersistentDataType.BYTE);
    }

    private boolean isSfMasterActive(Player player) {
        return (player.hasPermission(SFMASTER_CHEAT_PERMISSION) || player.hasPermission(SFMASTER_ACTIVE_PERMISSION))
                && !player.hasPermission(SFMASTER_BYPASS_PERMISSION);
    }

    private boolean hasSfMasterAccess(Player player) {
        return player.hasPermission(SFMASTER_CHEAT_PERMISSION)
                || player.hasPermission(SFMASTER_ACTIVE_PERMISSION)
                || player.hasPermission("slimefun.cheat.items.bypass");
    }

    public void deliverGuidesToOnlinePassHolders() {
        Bukkit.getOnlinePlayers().forEach(this::ensureCheatGuide);
    }

    /** Runs on the main thread and only touches inventories belonging to online players. */
    public void startGuideCleanup() {
        long seconds = Math.max(10L, plugin.getConfig().getLong("sfmaster-guide.cleanup-interval-seconds", 30L));
        Bukkit.getScheduler().runTaskTimer(plugin, () -> Bukkit.getOnlinePlayers().forEach(this::purgeExpiredGuides), seconds * 20L, seconds * 20L);
    }

    private void ensureCheatGuide(Player player) {
        if (!plugin.getConfig().getBoolean("sfmaster-guide.enabled", true) || !isSfMasterActive(player)) {
            return;
        }
        if (hasOwnedGuide(player.getInventory(), player.getUniqueId()) || hasOwnedGuide(player.getEnderChest(), player.getUniqueId())) return;
        ItemStack guide = slimefunGuide.createOwnedCheatGuide(player.getUniqueId(), SFMasterPassExpiry.forPlayer(plugin, player));
        if (guide == null) {
            return;
        }
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(guide);
        if (!leftovers.isEmpty()) {
            player.sendMessage("§eSFMaster activo, pero necesitas un espacio libre para recibir la guía Cheat.");
        }
    }

    private boolean hasOwnedGuide(Inventory inventory, UUID owner) {
        for (ItemStack item : inventory.getContents()) {
            if (slimefunGuide.ownerOf(item).filter(owner::equals).isPresent()) return true;
        }
        return false;
    }

    private void purgeExpiredGuides(Player player) {
        boolean ownerLostPass = !isSfMasterActive(player);
        purgeInventory(player.getInventory(), player.getUniqueId(), ownerLostPass, 0);
        purgeInventory(player.getEnderChest(), player.getUniqueId(), ownerLostPass, 0);
    }

    /** Recursively removes only owned guides that are expired or whose owner lost the pass. */
    private int purgeInventory(Inventory inventory, UUID onlineOwner, boolean ownerLostPass, int depth) {
        if (inventory == null || depth > 4) return 0;
        int removed = 0;
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType().isAir()) continue;
            UUID owner = slimefunGuide.ownerOf(item).orElse(null);
            if (owner != null && (slimefunGuide.isExpired(item) || (ownerLostPass && owner.equals(onlineOwner)))) {
                inventory.setItem(slot, null);
                removed++;
                continue;
            }
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof BlockStateMeta stateMeta && stateMeta.getBlockState() instanceof ShulkerBox shulker) {
                removed += purgeInventory(shulker.getInventory(), onlineOwner, ownerLostPass, depth + 1);
                stateMeta.setBlockState(shulker);
                item.setItemMeta(stateMeta);
                inventory.setItem(slot, item);
            }
        }
        return removed;
    }

    private void removeHeldGuide(Player player, PlayerInteractEvent event) {
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) player.getInventory().setItemInOffHand(null);
        else player.getInventory().setItemInMainHand(null);
    }

    private boolean isCheatGuideView(InventoryClickEvent event) {
        return ChatColor.stripColor(event.getView().getTitle()).contains("Cheat Sheet");
    }

    private boolean matchesTemplate(ItemStack candidate, ItemStack template) {
        if (candidate == null || template == null) {
            return false;
        }

        ItemStack left = candidate.clone();
        ItemStack right = template.clone();
        left.setAmount(1);
        right.setAmount(1);
        return left.isSimilar(right);
    }

    private void markItem(ItemStack item) {
        if (item == null) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.getPersistentDataContainer().set(sfMasterKey, PersistentDataType.BYTE, (byte) 1);
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        if (!lore.contains(SFMASTER_MARKER_LORE)) {
            lore.add("");
            lore.add(SFMASTER_MARKER_LORE);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private Map<Integer, Integer> snapshotMatchingSlots(Player player, ItemStack template) {
        Map<Integer, Integer> snapshot = new HashMap<>();
        ItemStack[] contents = player.getInventory().getStorageContents();

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (matchesTemplate(item, template) && !isSFMasterItem(item)) {
                snapshot.put(slot, item.getAmount());
            }
        }

        return snapshot;
    }

    private void markNewCheatItems(Player player, ItemStack template, Map<Integer, Integer> before) {
        ItemStack[] contents = player.getInventory().getStorageContents();

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack current = contents[slot];
            if (!matchesTemplate(current, template) || isSFMasterItem(current)) {
                continue;
            }

            int previousAmount = before.getOrDefault(slot, 0);
            if (current.getAmount() <= previousAmount) {
                continue;
            }

            int delta = current.getAmount() - previousAmount;
            if (previousAmount <= 0) {
                markItem(current);
                player.getInventory().setItem(slot, current);
                continue;
            }

            ItemStack originalStack = current.clone();
            originalStack.setAmount(previousAmount);
            player.getInventory().setItem(slot, originalStack);

            ItemStack markedStack = current.clone();
            markedStack.setAmount(delta);
            markItem(markedStack);
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(markedStack);
            leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isSFMasterItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cNo puedes soltar ítems generados por el modo SFMaster.");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (isSFMasterItem(item)) {
            Block block = event.getBlockPlaced();
            String locKey = block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ();
            sfMasterBlocks.add(locKey);
            saveBlocks();
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        String locKey = block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ();
        if (sfMasterBlocks.contains(locKey)) {
            sfMasterBlocks.remove(locKey);
            saveBlocks();
            brokenSFMasterBlocks.put(locKey, System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        Item itemEntity = event.getEntity();
        Location loc = itemEntity.getLocation();
        String targetKey = null;
        long now = System.currentTimeMillis();

        for (Map.Entry<String, Long> entry : brokenSFMasterBlocks.entrySet()) {
            if (now - entry.getValue() > 1000) {
                continue;
            }
            String[] parts = entry.getKey().split(",");
            if (parts[0].equals(loc.getWorld().getName())) {
                double bx = Double.parseDouble(parts[1]) + 0.5;
                double by = Double.parseDouble(parts[2]) + 0.5;
                double bz = Double.parseDouble(parts[3]) + 0.5;
                Location bLoc = new Location(loc.getWorld(), bx, by, bz);
                if (loc.distanceSquared(bLoc) < 2.25) { // 1.5 bloques de radio
                    targetKey = entry.getKey();
                    break;
                }
            }
        }

        if (targetKey != null) {
            ItemStack item = itemEntity.getItemStack();
            markItem(item);
            itemEntity.setItemStack(item);
            brokenSFMasterBlocks.remove(targetKey);
        }
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage().toLowerCase();
        if (isSfMasterActive(event.getPlayer())
                && plugin.getConfig().getBoolean("sfmaster-guide.block-cheat-command", true)
                && (msg.equals("/sf cheat") || msg.equals("/slimefun cheat"))) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§eUsa la guía SFMaster que recibiste para abrir la Cheat Sheet.");
            return;
        }
        if (isSfMasterActive(event.getPlayer())
                && (msg.startsWith("/sf give") || msg.startsWith("/slimefun give"))) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cSFMaster solo permite reclamar desde la Cheat Sheet controlada.");
            return;
        }
        if (msg.startsWith("/ah sell") || msg.startsWith("/ah sellbid") || msg.startsWith("/ahca sell") || msg.startsWith("/crazyauctions sell")) {
            Player player = event.getPlayer();
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (isSFMasterItem(hand)) {
                event.setCancelled(true);
                player.sendMessage("§cNo puedes vender ítems de SFMaster en la subasta.");
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCheatGuideUse(PlayerInteractEvent event) {
        ItemStack guide = event.getItem();
        if (!slimefunGuide.isCheatGuide(guide)) return;
        Player player = event.getPlayer();
        var owner = slimefunGuide.ownerOf(guide);
        if (owner.isPresent()) {
            event.setCancelled(true);
            if (!owner.get().equals(player.getUniqueId()) || slimefunGuide.isExpired(guide) || !isSfMasterActive(player)) {
                removeHeldGuide(player, event);
                player.sendMessage("§cTu guía SFMaster venció o no te pertenece y fue eliminada.");
                return;
            }
            slimefunGuide.openOwnedCheatGuide(player);
            return;
        }
        if (!hasSfMasterAccess(player)) {
            event.setCancelled(true);
            player.sendMessage("§cTu pase SFMaster expiró. Esta guía Cheat ya no se puede usar.");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            purgeExpiredGuides(event.getPlayer());
            ensureCheatGuide(event.getPlayer());
        });
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(item -> isSFMasterItem(item));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            purgeInventory(event.getView().getTopInventory(), player.getUniqueId(), false, 0);
            purgeExpiredGuides(player);
        }
        if (event.getWhoClicked() instanceof Player player
                && isSfMasterActive(player)
                && isCheatGuideView(event)
                && event.getClickedInventory() != null
                && event.getClickedInventory().getType() != InventoryType.PLAYER
                && event.getCurrentItem() != null
                && !event.isCancelled()) {
            ItemStack template = event.getCurrentItem().clone();
            String reason = blockedReason(template);
            if (reason != null) {
                event.setCancelled(true);
                player.sendMessage("§cNo puedes reclamar este item: " + reason + ".");
                return;
            }
            if (!consumeClaim(player)) {
                event.setCancelled(true);
                player.sendMessage("§cLimite SFMaster alcanzado: " + maxClaims + " reclamaciones por ventana.");
                return;
            }
            Map<Integer, Integer> before = snapshotMatchingSlots(player, template);
            Bukkit.getScheduler().runTask(plugin, () -> markNewCheatItems(player, template, before));
        }

        // Permitir interacciones puramente dentro del inventario del jugador
        if (event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.PLAYER) {
            // Permitir moverse cosas dentro del propio inventario, EXCEPTO si están haciendo shift-click hacia otro inventario no permitido
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                if (event.getInventory().getType() != InventoryType.PLAYER && event.getInventory().getType() != InventoryType.CRAFTING) {
                    if (isSFMasterItem(event.getCurrentItem())) {
                        event.setCancelled(true);
                        if (event.getWhoClicked() instanceof Player) {
                            ((Player) event.getWhoClicked()).sendMessage("§cNo puedes guardar ítems de SFMaster aquí.");
                        }
                    }
                }
            }
            return;
        }

        // Si están interactuando con otro inventario (cofre, tradeo, etc.)
        if (isSFMasterItem(event.getCurrentItem()) || isSFMasterItem(event.getCursor())) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player) {
                ((Player) event.getWhoClicked()).sendMessage("§cNo puedes mover ítems de SFMaster a este inventario.");
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory == null) {
            return;
        }

        boolean touchesExternalInventory = event.getRawSlots().stream()
                .anyMatch(slot -> slot < inventory.getSize());

        if (touchesExternalInventory && isSFMasterItem(event.getOldCursor())) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                player.sendMessage("§cNo puedes arrastrar ítems de SFMaster a este inventario.");
            }
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof ItemFrame || event.getRightClicked().getType().name().equals("ARMOR_STAND")) {
            ItemStack hand = event.getPlayer().getInventory().getItemInMainHand();
            if (isSFMasterItem(hand)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cNo puedes colocar ítems de SFMaster aquí.");
            }
            ItemStack offhand = event.getPlayer().getInventory().getItemInOffHand();
            if (isSFMasterItem(offhand)) {
                event.setCancelled(true);
            }
        }
    }

    private record SfItemDescriptor(String id, String addon) {}
}
