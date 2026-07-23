package org.metamechanists.odysseia.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.metamechanists.odysseia.Odysseia;

public final class ArmorEffectsListener implements Listener {

    private static final Set<String> CONFIGURED_EFFECTS = Set.of(
            "speed", "resistance", "health-boost", "saturation", "fire-resistance",
            "jump-boost", "regeneration", "strength", "night-vision");

    private final Odysseia plugin;
    private final Map<UUID, Set<PotionEffectType>> appliedEffects = new HashMap<>();

    public ArmorEffectsListener(Odysseia plugin) {
        this.plugin = plugin;
        
        long refreshSeconds = plugin.getConfig().getLong("armor-effects.refresh-interval-seconds", 90L);
        long refreshTicks = refreshSeconds * 20L;
        
        // Run lightweight task at the configured refresh interval
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkAllPlayers, 60L, refreshTicks);
    }

    private void checkAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkAndApply(player);
        }
    }

    private void checkAndApply(Player player) {
        UUID uuid = player.getUniqueId();
        boolean hasFullDiamond = isWearingFullDiamond(player);
        boolean hasFullNetherite = isWearingFullNetherite(player);

        Set<PotionEffect> effectsToApply = new HashSet<>();

        if (hasFullDiamond && player.hasPermission("drakes.kit.hercules")) {
            addConfiguredEffects(effectsToApply, "hercules");
        } else if (hasFullNetherite && player.hasPermission("drakes.kit.zeus")) {
            addConfiguredEffects(effectsToApply, "zeus");
        } else if (hasFullNetherite && player.hasPermission("drakes.kit.afrodita")) {
            addConfiguredEffects(effectsToApply, "afrodita");
        } else if (hasFullNetherite && player.hasPermission("drakes.kit.artemisa")) {
            addConfiguredEffects(effectsToApply, "artemisa");
        } else if (hasFullNetherite && player.hasPermission("drakes.kit.hefesto")) {
            addConfiguredEffects(effectsToApply, "hefesto");
        } else if (hasFullNetherite && player.hasPermission("drakes.kit.hermes")) {
            addConfiguredEffects(effectsToApply, "hermes");
        } else if (hasFullNetherite && player.hasPermission("drakes.kit.hestia")) {
            addConfiguredEffects(effectsToApply, "hestia");
        }

        // If we have effects to apply, apply them and track them
        if (!effectsToApply.isEmpty()) {
            Set<PotionEffectType> desiredTypes = new HashSet<>();
            for (PotionEffect effect : effectsToApply) desiredTypes.add(effect.getType());
            Set<PotionEffectType> previousTypes = appliedEffects.getOrDefault(uuid, Set.of());
            for (PotionEffectType previousType : previousTypes) {
                if (!desiredTypes.contains(previousType)) removeTrackedEffect(player, previousType);
            }
            for (PotionEffect effect : effectsToApply) {
                player.addPotionEffect(effect);
            }
            appliedEffects.put(uuid, desiredTypes);
        } else {
            // Remove any previously applied rank effects
            Set<PotionEffectType> currentTracked = appliedEffects.remove(uuid);
            if (currentTracked != null) {
                for (PotionEffectType type : currentTracked) {
                    removeTrackedEffect(player, type);
                }
            }
        }
    }

    private boolean isWearingFullDiamond(Player player) {
        PlayerInventory inv = player.getInventory();
        return inv.getHelmet() != null && inv.getHelmet().getType() == Material.DIAMOND_HELMET
                && inv.getChestplate() != null && inv.getChestplate().getType() == Material.DIAMOND_CHESTPLATE
                && inv.getLeggings() != null && inv.getLeggings().getType() == Material.DIAMOND_LEGGINGS
                && inv.getBoots() != null && inv.getBoots().getType() == Material.DIAMOND_BOOTS;
    }

    private boolean isWearingFullNetherite(Player player) {
        PlayerInventory inv = player.getInventory();
        return inv.getHelmet() != null && inv.getHelmet().getType() == Material.NETHERITE_HELMET
                && inv.getChestplate() != null && inv.getChestplate().getType() == Material.NETHERITE_CHESTPLATE
                && inv.getLeggings() != null && inv.getLeggings().getType() == Material.NETHERITE_LEGGINGS
                && inv.getBoots() != null && inv.getBoots().getType() == Material.NETHERITE_BOOTS;
    }

    private PotionEffect getEffect(PotionEffectType type, int level) {
        int durationSeconds = plugin.getConfig().getInt("armor-effects.effect-duration-seconds", 180);
        int durationTicks = durationSeconds * 20;
        // ambient=true, particles=false, icon=false
        return new PotionEffect(type, durationTicks, level - 1, true, false, false);
    }

    private void addConfiguredEffects(Set<PotionEffect> effects, String rank) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("armor-effects." + rank);
        if (section == null) {
            plugin.getLogger().warning("No existe la configuración de aura para el rango " + rank);
            return;
        }
        configuredEffectLevels(section).forEach((key, level) -> effects.add(getEffect(effectType(key), level)));
    }

    static Map<String, Integer> configuredEffectLevels(ConfigurationSection section) {
        Map<String, Integer> effects = new HashMap<>();
        CONFIGURED_EFFECTS.forEach(key -> {
            Object value = section.get(key);
            int level = value instanceof Boolean enabled ? (enabled ? 1 : 0) : section.getInt(key, 0);
            if (level > 0) effects.put(key, level);
        });
        return effects;
    }

    private PotionEffectType effectType(String key) {
        return switch (key) {
            case "speed" -> PotionEffectType.SPEED;
            case "resistance" -> PotionEffectType.RESISTANCE;
            case "health-boost" -> PotionEffectType.HEALTH_BOOST;
            case "saturation" -> PotionEffectType.SATURATION;
            case "fire-resistance" -> PotionEffectType.FIRE_RESISTANCE;
            case "jump-boost" -> PotionEffectType.JUMP_BOOST;
            case "regeneration" -> PotionEffectType.REGENERATION;
            case "strength" -> PotionEffectType.STRENGTH;
            case "night-vision" -> PotionEffectType.NIGHT_VISION;
            default -> throw new IllegalArgumentException("Efecto de aura no soportado: " + key);
        };
    }

    private void removeTrackedEffect(Player player, PotionEffectType type) {
        PotionEffect active = player.getPotionEffect(type);
        int configuredDuration = plugin.getConfig().getInt("armor-effects.effect-duration-seconds", 180) * 20;
        if (active != null && active.getDuration() <= configuredDuration) player.removePotionEffect(type);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // Delay 1 tick so inventory is fully loaded before checking
        Bukkit.getScheduler().runTaskLater(plugin, () -> checkAndApply(e.getPlayer()), 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        appliedEffects.remove(e.getPlayer().getUniqueId());
    }

    // Detect armor changes via inventory clicks (drag into armor slots or shift-click)
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        boolean isArmorSlot = e.getSlotType() == InventoryType.SlotType.ARMOR;
        boolean isShiftClickFromInventory = e.isShiftClick()
                && e.getInventory().getType() == InventoryType.CRAFTING
                && e.getCurrentItem() != null
                && isArmorMaterial(e.getCurrentItem().getType());
        if (isArmorSlot || isShiftClickFromInventory) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> checkAndApply(player), 1L);
        }
    }

    // Detect right-click auto-equip
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getItem() == null) return;
        if (!isArmorMaterial(e.getItem().getType())) return;
        if (e.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                || e.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> checkAndApply(e.getPlayer()), 1L);
        }
    }

    private boolean isArmorMaterial(org.bukkit.Material m) {
        return switch (m) {
            case DIAMOND_HELMET, DIAMOND_CHESTPLATE, DIAMOND_LEGGINGS, DIAMOND_BOOTS,
                 NETHERITE_HELMET, NETHERITE_CHESTPLATE, NETHERITE_LEGGINGS, NETHERITE_BOOTS -> true;
            default -> false;
        };
    }
}
