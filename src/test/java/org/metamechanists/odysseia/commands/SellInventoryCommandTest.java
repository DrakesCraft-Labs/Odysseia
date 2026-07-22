package org.metamechanists.odysseia.commands;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SellInventoryCommandTest {
    @Test
    void automatedMineralsCannotBeSold() {
        SellInventoryCommand command = new SellInventoryCommand(null);
        Set<Material> minerals = Set.of(
            Material.COAL, Material.CHARCOAL,
            Material.RAW_IRON, Material.IRON_INGOT,
            Material.RAW_GOLD, Material.GOLD_INGOT,
            Material.RAW_COPPER, Material.COPPER_INGOT,
            Material.REDSTONE, Material.LAPIS_LAZULI,
            Material.DIAMOND, Material.EMERALD,
            Material.NETHERITE_INGOT, Material.QUARTZ
        );

        minerals.forEach(material -> assertFalse(command.isSellable(material), material.name()));
    }

    @Test
    void ordinarySurvivalDropsRemainSellable() {
        SellInventoryCommand command = new SellInventoryCommand(null);

        assertTrue(command.isSellable(Material.ROTTEN_FLESH));
        assertTrue(command.isSellable(Material.WHEAT));
        assertTrue(command.isSellable(Material.OAK_LOG));
    }
}
