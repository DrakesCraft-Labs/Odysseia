package org.metamechanists.odysseia.listeners;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.metamechanists.odysseia.Odysseia;

import java.util.Set;

/** Keeps normal players on Polis after temporary groups are removed by LuckPerms. */
public final class PolisBaselineListener implements Listener {
    private final Odysseia plugin;
    private final LuckPerms luckPerms;

    public PolisBaselineListener(Odysseia plugin) {
        this.plugin = plugin;
        RegisteredServiceProvider<LuckPerms> registration = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        this.luckPerms = registration == null ? null : registration.getProvider();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        reconcile(event.getPlayer());
    }

    /** Covers temporary-rank expiries that occur while a player stays online. */
    public void reconcileOnlinePlayers() {
        Bukkit.getOnlinePlayers().forEach(this::reconcile);
    }

    private void reconcile(Player player) {
        if (luckPerms == null || !plugin.getConfig().getBoolean("polis-baseline.enabled", true)) {
            return;
        }

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return;
        }

        String defaultGroup = plugin.getConfig().getString("polis-baseline.default-group", "default");
        String baseGroup = plugin.getConfig().getString("polis-baseline.base-group", "polis");
        Set<String> permanentParents = user.getNodes().stream()
                .filter(InheritanceNode.class::isInstance)
                .map(InheritanceNode.class::cast)
                .filter(node -> !node.hasExpiry())
                .map(node -> node.getGroupName().toLowerCase())
                .collect(java.util.stream.Collectors.toSet());

        if (!permanentParents.contains(defaultGroup.toLowerCase())
                || permanentParents.stream().anyMatch(group -> !group.equalsIgnoreCase(defaultGroup) && !group.equalsIgnoreCase(baseGroup))) {
            return;
        }

        boolean changed = false;
        if (!permanentParents.contains(baseGroup.toLowerCase())) {
            user.data().add(InheritanceNode.builder(baseGroup).build());
            changed = true;
        }
        if (user.getPrimaryGroup().equalsIgnoreCase(defaultGroup)) {
            user.setPrimaryGroup(baseGroup);
            changed = true;
        }
        if (changed) {
            luckPerms.getUserManager().saveUser(user);
        }
    }
}
