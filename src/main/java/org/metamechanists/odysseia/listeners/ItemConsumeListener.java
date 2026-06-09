package org.metamechanists.odysseia.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.metamechanists.odysseia.Odysseia;

public final class ItemConsumeListener implements Listener {

    private final Odysseia plugin;

    public ItemConsumeListener(Odysseia plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent e) {
        ItemStack item = e.getItem();
        if (isPapaDeMar(item)) {
            Player p = e.getPlayer();
            int food = plugin.getConfig().getInt("papa-de-mar.food-restored", 1);
            double sat = plugin.getConfig().getDouble("papa-de-mar.saturation-restored", 8.4);
            
            p.setFoodLevel(Math.min(20, p.getFoodLevel() + food));
            p.setSaturation(Math.min(20.0f, p.getSaturation() + (float) sat));
            
            // Spawn fancy golden particles around the player
            p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(0, 1, 0), 15, 0.4, 0.4, 0.4, 0.1);
        }
    }

    @SuppressWarnings("deprecation")
    private boolean isPapaDeMar(ItemStack item) {
        if (item == null || item.getType() != Material.BAKED_POTATO) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        String name = item.getItemMeta().getDisplayName();
        if (name == null) {
            return false;
        }
        String stripped = ChatColor.stripColor(name);
        return stripped.contains("✦ Papa de mar ✦") || stripped.contains("Papa de mar");
    }
}
