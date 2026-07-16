package org.metamechanists.odysseia.integrations;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Level;

/** Bridges the optional Slimefun Cheat guide without linking its JAR at compile time. */
public final class SlimefunGuideBridge {
    private final Plugin plugin;
    private Method getGuideItem;
    private Object cheatMode;
    private boolean unavailableLogged;

    public SlimefunGuideBridge(Plugin plugin) {
        this.plugin = plugin;
        try {
            Class<?> guideMode = Class.forName("com.github.drakescraft_labs.slimefun4.core.guide.SlimefunGuideMode");
            Class<?> guide = Class.forName("com.github.drakescraft_labs.slimefun4.core.guide.SlimefunGuide");
            this.cheatMode = Enum.valueOf(guideMode.asSubclass(Enum.class), "CHEAT_MODE");
            this.getGuideItem = guide.getMethod("getItem", guideMode);
        } catch (ReflectiveOperationException exception) {
            logUnavailable(exception);
        }
    }

    public ItemStack createCheatGuide() {
        if (getGuideItem == null || cheatMode == null) {
            return null;
        }

        try {
            ItemStack guide = (ItemStack) getGuideItem.invoke(null, cheatMode);
            return guide == null ? null : guide.clone();
        } catch (ReflectiveOperationException exception) {
            logUnavailable(exception);
            return null;
        }
    }

    public boolean isCheatGuide(ItemStack candidate) {
        ItemStack template = createCheatGuide();
        return candidate != null && template != null && candidate.isSimilar(template);
    }

    private void logUnavailable(ReflectiveOperationException exception) {
        if (!unavailableLogged) {
            unavailableLogged = true;
            plugin.getLogger().log(Level.WARNING, "[SFMaster] No se pudo enlazar la guía Cheat de Slimefun.", exception);
        }
    }
}
