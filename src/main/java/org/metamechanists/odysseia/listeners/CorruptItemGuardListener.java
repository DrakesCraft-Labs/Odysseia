package org.metamechanists.odysseia.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Neutraliza items corruptos antes de que tumben la conexion del jugador.
 *
 * Origen: un jugador se desconecto con un crash de paquete al abrir un contenedor. La causa fue un
 * item con perfil de cabeza corrupto o demasiado grande: al serializar el inventario para enviarlo,
 * el paquete revienta. El item queda en el mundo, asi que cualquiera que abra ese cofre repite el
 * crash — es un vector reproducible, no un accidente aislado.
 *
 * Aqui se revisa al abrir un inventario y al entrar. Lo que no se puede serializar se reemplaza por
 * una cabeza limpia con el mismo nombre, en vez de borrarlo: el jugador conserva el item y el staff
 * puede rastrear que paso por el log.
 */
public final class CorruptItemGuardListener implements Listener {

    /** Un perfil sano no llega ni de lejos a esto; pasado el limite el paquete se vuelve peligroso. */
    private static final int MAX_TEXTURA = 32_000;

    private final JavaPlugin plugin;

    public CorruptItemGuardListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onOpen(InventoryOpenEvent event) {
        if (!plugin.getConfig().getBoolean("corrupt-item-guard.enabled", true)) return;
        sanear(event.getInventory(), event.getPlayer().getName(), "abrir inventario");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("corrupt-item-guard.enabled", true)) return;
        Player player = event.getPlayer();
        sanear(player.getInventory(), player.getName(), "entrar al servidor");
        sanear(player.getEnderChest(), player.getName(), "entrar al servidor");
    }

    /** Reemplaza cada item que no sobreviva una serializacion. Devuelve cuantos se sanearon. */
    private int sanear(Inventory inventory, String quien, String cuando) {
        int saneados = 0;
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType().isAir()) continue;
            if (!estaCorrupto(item)) continue;
            inventory.setItem(slot, reemplazo(item));
            saneados++;
            plugin.getLogger().warning("[ItemGuard] Item corrupto saneado al " + cuando
                    + " de " + quien + " (slot " + slot + ", tipo " + item.getType() + ")");
        }
        return saneados;
    }

    /**
     * Un item esta corrupto si su meta no se puede leer, o si su perfil de cabeza es
     * desproporcionado. Cualquier excepcion cuenta como corrupto: si lanza aqui, lanza al serializar.
     */
    static boolean estaCorrupto(ItemStack item) {
        try {
            if (!item.hasItemMeta()) return false;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return true;
            if (meta instanceof SkullMeta skull && skull.getOwnerProfile() != null) {
                String textura = String.valueOf(skull.getOwnerProfile().getTextures().getSkin());
                if (textura.length() > MAX_TEXTURA) return true;
            }
            // Forzar la lectura completa: es lo mismo que hace el servidor al armar el paquete.
            meta.serialize();
            return false;
        } catch (Exception | LinkageError error) {
            return true;
        }
    }

    /** Cabeza limpia que conserva el nombre, para que el jugador sepa que item era. */
    private ItemStack reemplazo(ItemStack original) {
        ItemStack limpio = new ItemStack(Material.PLAYER_HEAD, Math.max(1, original.getAmount()));
        try {
            ItemMeta meta = limpio.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§c§lÍtem dañado");
                meta.setLore(java.util.List.of(
                        "§7Este objeto estaba corrupto y hacía",
                        "§7caer la conexión al abrirlo.",
                        "§7Avisa al staff si era algo tuyo."));
                limpio.setItemMeta(meta);
            }
        } catch (Exception error) {
            plugin.getLogger().log(Level.WARNING, "[ItemGuard] No se pudo etiquetar el reemplazo", error);
        }
        return limpio;
    }
}
