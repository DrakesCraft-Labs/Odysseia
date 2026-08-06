package org.metamechanists.odysseia.vaults;

import org.bukkit.inventory.InventoryHolder;

/**
 * Marca un inventario como boveda de Odysseia.
 *
 * Existe para que los guards puedan reconocer una boveda sin conocer la clase concreta que la
 * implementa ni tener que compararla por titulo, que cambia con el idioma y con la configuracion.
 */
public interface VaultInventory extends InventoryHolder {
}
