package org.metamechanists.odysseia.listeners;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Regression coverage for synchronous secondary damage fired by divine weapons. */
class DivineEnchantmentListenerTest {

    private ServerMock server;
    private PlayerMock attacker;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        attacker = server.addPlayer("combat-qa");

        ItemStack weapon = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = weapon.getItemMeta();
        meta.setDisplayName("Mjolnir de QA");
        weapon.setItemMeta(meta);
        attacker.getInventory().setItemInMainHand(weapon);
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void impactoSecundarioDeMjolnirNoVuelveAExpandirElArea() {
        // The listener only needs an enabled Plugin identity for event registration;
        // loading a MockBukkit plugin from a packaged JAR makes `mvn test` depend on
        // a prior package phase.
        Plugin plugin = mock(Plugin.class);
        PluginLoader pluginLoader = mock(PluginLoader.class);
        when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getPluginLoader()).thenReturn(pluginLoader);
        when(pluginLoader.createRegisteredListeners(any(Listener.class), same(plugin))).thenReturn(java.util.Map.of());
        DivineEnchantmentListener listener = new DivineEnchantmentListener(plugin);
        LivingEntity primary = livingEntityWithNearby();
        LivingEntity secondary = livingEntityWithNearby();
        LivingEntity tertiary = mock(LivingEntity.class);
        when(primary.getNearbyEntities(6.0, 3.0, 6.0)).thenReturn(java.util.List.of(secondary));
        when(secondary.getNearbyEntities(6.0, 3.0, 6.0)).thenReturn(java.util.List.of(tertiary));

        doAnswer(ignored -> {
            listener.onWeaponAttack(new EntityDamageByEntityEvent(attacker, secondary,
                    EntityDamageEvent.DamageCause.ENTITY_ATTACK, 1.0));
            return null;
        }).when(secondary).damage(8.0, attacker);

        listener.onWeaponAttack(new EntityDamageByEntityEvent(attacker, primary,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK, 1.0));

        verify(secondary).damage(8.0, attacker);
        verify(tertiary, never()).damage(anyDouble(), same(attacker));
    }

    private LivingEntity livingEntityWithNearby() {
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getLocation()).thenReturn(attacker.getLocation());
        when(entity.getNearbyEntities(6.0, 3.0, 6.0)).thenReturn(java.util.List.of());
        return entity;
    }
}
