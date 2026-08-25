package org.metamechanists.odysseia.laboratorio;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Locale;
import java.util.Set;

/**
 * Prevents the creative laboratory from granting persistent Minecraft advancements.
 *
 * <p>The criterion event is cancelled before Minecraft completes the advancement, announces it
 * or grants its rewards. Progress remains global in normal modalities, while testing in the
 * laboratory cannot contaminate a player's legitimate survival progress.</p>
 */
public final class SandboxAdvancementGuardListener implements Listener {

    private final Set<String> sandboxWorlds;

    public SandboxAdvancementGuardListener(Set<String> sandboxWorlds) {
        this.sandboxWorlds = Set.copyOf(sandboxWorlds);
    }

    /** Stops every vanilla or datapack advancement criterion originating in the laboratory. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCriterionGrant(PlayerAdvancementCriterionGrantEvent event) {
        if (isSandboxWorld(event.getPlayer().getWorld().getName(), sandboxWorlds)) {
            event.setCancelled(true);
        }
    }

    static boolean isSandboxWorld(String worldName, Set<String> sandboxWorlds) {
        return worldName != null && sandboxWorlds.contains(worldName.toLowerCase(Locale.ROOT));
    }
}
