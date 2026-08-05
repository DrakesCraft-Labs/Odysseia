package org.metamechanists.odysseia.purchase;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Entrega piedras de ProtectionStones identificandolas por su alias.
 *
 * El comando de consola {@code /ps give} identifica el bloque por su MATERIAL, no por el alias,
 * asi que no sirve para entregar: rechaza los alias con "Invalid protection block" y, aunque se
 * le pasara el material, cuatro pares de bloques comparten material y entregaria el equivocado
 * (novato/vipthor, olympos/titanjapeto, olympia/titanhiperion, thera/titancronos).
 *
 * La API si distingue por alias, que es unico. La reflexion mantiene ProtectionStones como
 * dependencia opcional sin acoplar el JAR al classpath de compilacion de Odysseia.
 */
public final class ProtectionStoneDelivery {

    private ProtectionStoneDelivery() {
    }

    /** Entrega {@code amount} piedras del alias indicado, respetando el espacio del inventario. */
    public static ActionResult give(Player player, String alias, int amount) {
        try {
            Class<?> protectionStones = Class.forName("dev.espi.protectionstones.ProtectionStones");
            Object block = protectionStones.getMethod("getProtectBlockFromAlias", String.class).invoke(null, alias);
            if (block == null) return ActionResult.manual("Alias ProtectionStones inexistente: " + alias);
            ItemStack prototype = (ItemStack) block.getClass().getMethod("createItem").invoke(block);
            if (prototype == null || prototype.getType().isAir()) {
                return ActionResult.manual("ProtectionStones no creó un ítem para " + alias);
            }
            if (!hasInventoryCapacity(player, prototype, amount)) {
                return ActionResult.waiting("Inventario sin espacio para ProtectionStone " + alias);
            }

            int remaining = amount;
            while (remaining > 0) {
                ItemStack item = prototype.clone();
                int stack = Math.min(remaining, item.getMaxStackSize());
                item.setAmount(stack);
                if (!player.getInventory().addItem(item).isEmpty()) {
                    return ActionResult.retryable("Inventario cambió durante la entrega de " + alias);
                }
                remaining -= stack;
            }
            return ActionResult.completed("alias=" + alias + ";amount=" + amount);
        } catch (ReflectiveOperationException error) {
            return ActionResult.retryable("API ProtectionStones no disponible: " + error.getClass().getSimpleName());
        }
    }

    static boolean hasInventoryCapacity(Player player, ItemStack prototype, int amount) {
        int capacity = 0;
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType().isAir()) capacity += prototype.getMaxStackSize();
            else if (slot.isSimilar(prototype)) capacity += Math.max(0, slot.getMaxStackSize() - slot.getAmount());
            if (capacity >= amount) return true;
        }
        return false;
    }
}
