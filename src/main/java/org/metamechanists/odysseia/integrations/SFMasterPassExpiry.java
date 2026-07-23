package org.metamechanists.odysseia.integrations;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.Plugin;

/** Reads the real temporary sfmaster node so guide expiry follows renewals exactly. */
public final class SFMasterPassExpiry {
    private SFMasterPassExpiry() { }

    public static long forPlayer(Plugin plugin, Player player) {
        RegisteredServiceProvider<LuckPerms> registration = plugin.getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (registration == null) return 0L;
        User user = registration.getProvider().getUserManager().getUser(player.getUniqueId());
        if (user == null) return 0L;
        return user.getNodes().stream()
                .filter(InheritanceNode.class::isInstance)
                .map(InheritanceNode.class::cast)
                .filter(node -> node.getGroupName().equalsIgnoreCase("sfmaster") && node.hasExpiry())
                .mapToLong(node -> node.getExpiry().getEpochSecond())
                .max().orElse(0L);
    }
}
