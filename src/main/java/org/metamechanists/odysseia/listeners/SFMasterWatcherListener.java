package org.metamechanists.odysseia.listeners;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class SFMasterWatcherListener implements Listener {

    private final NamespacedKey sfMasterKey = NamespacedKey.fromString("odysseia:sfmaster_item");

    private boolean isSFMasterItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (sfMasterKey == null) return false;
        return pdc.has(sfMasterKey, PersistentDataType.BYTE);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isSFMasterItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cNo puedes soltar ítems generados por el modo SFMaster.");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
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
}
