package org.metamechanists.odysseia.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.metamechanists.odysseia.Odysseia;

public final class ArmorEffectsListener implements Listener {

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

        if (hasFullDiamond) {
            if (player.hasPermission("essentials.kits.hermes")) {
                addHermes(effectsToApply);
            } else if (player.hasPermission("essentials.kits.hestia")) {
                addHestia(effectsToApply);
            } else if (player.hasPermission("essentials.kits.hercules")) {
                addHercules(effectsToApply);
            }
        }

        if (hasFullNetherite) {
            if (player.hasPermission("essentials.kits.zeus")) {
                addZeus(effectsToApply);
            } else if (player.hasPermission("essentials.kits.afrodita")) {
                addAfrodita(effectsToApply);
            } else if (player.hasPermission("essentials.kits.artemisa")) {
                addArtemisa(effectsToApply);
            } else if (player.hasPermission("essentials.kits.hefesto")) {
                addHefesto(effectsToApply);
            }
        }

        // If we have effects to apply, apply them and track them
        if (!effectsToApply.isEmpty()) {
            Set<PotionEffectType> currentTracked = appliedEffects.computeIfAbsent(uuid, k -> new HashSet<>());
            for (PotionEffect effect : effectsToApply) {
                // Apply or refresh potion effect (duration 3 minutes, hide particles, hide icon)
                player.addPotionEffect(effect);
                currentTracked.add(effect.getType());
            }
        } else {
            // Remove any previously applied rank effects
            Set<PotionEffectType> currentTracked = appliedEffects.remove(uuid);
            if (currentTracked != null) {
                for (PotionEffectType type : currentTracked) {
                    if (player.hasPotionEffect(type)) {
                        // Check if the duration is <= 180 seconds (meaning it was likely ours)
                        PotionEffect active = player.getPotionEffect(type);
                        if (active != null && active.getDuration() <= 3600) {
                            player.removePotionEffect(type);
                        }
                    }
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

    private void addHercules(Set<PotionEffect> effects) {
        effects.add(getEffect(PotionEffectType.SPEED, 1));
        effects.add(getEffect(PotionEffectType.RESISTANCE, 1));
    }

    private void addHestia(Set<PotionEffect> effects) {
        effects.add(getEffect(PotionEffectType.SPEED, 2));
        effects.add(getEffect(PotionEffectType.HEALTH_BOOST, 1));
        effects.add(getEffect(PotionEffectType.SATURATION, 1));
    }

    private void addHermes(Set<PotionEffect> effects) {
        effects.add(getEffect(PotionEffectType.SPEED, 4));
        effects.add(getEffect(PotionEffectType.HEALTH_BOOST, 1));
        effects.add(getEffect(PotionEffectType.SATURATION, 1));
    }

    private void addHefesto(Set<PotionEffect> effects) {
        effects.add(getEffect(PotionEffectType.SPEED, 2));
        effects.add(getEffect(PotionEffectType.HEALTH_BOOST, 2));
        effects.add(getEffect(PotionEffectType.FIRE_RESISTANCE, 1));
        effects.add(getEffect(PotionEffectType.SATURATION, 1));
    }

    private void addArtemisa(Set<PotionEffect> effects) {
        effects.add(getEffect(PotionEffectType.SPEED, 3));
        effects.add(getEffect(PotionEffectType.HEALTH_BOOST, 2));
        effects.add(getEffect(PotionEffectType.STRENGTH, 1));
        effects.add(getEffect(PotionEffectType.SATURATION, 1));
    }

    private void addAfrodita(Set<PotionEffect> effects) {
        effects.add(getEffect(PotionEffectType.SPEED, 3));
        effects.add(getEffect(PotionEffectType.HEALTH_BOOST, 3));
        effects.add(getEffect(PotionEffectType.STRENGTH, 1));
        effects.add(getEffect(PotionEffectType.REGENERATION, 1));
        effects.add(getEffect(PotionEffectType.SATURATION, 1));
    }

    private void addZeus(Set<PotionEffect> effects) {
        effects.add(getEffect(PotionEffectType.SPEED, 4));
        effects.add(getEffect(PotionEffectType.HEALTH_BOOST, 4));
        effects.add(getEffect(PotionEffectType.STRENGTH, 2));
        effects.add(getEffect(PotionEffectType.RESISTANCE, 1));
        effects.add(getEffect(PotionEffectType.SATURATION, 1));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        appliedEffects.remove(e.getPlayer().getUniqueId());
    }
}
