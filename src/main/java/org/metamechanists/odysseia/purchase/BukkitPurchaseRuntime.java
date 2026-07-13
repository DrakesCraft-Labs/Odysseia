package org.metamechanists.odysseia.purchase;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.metamechanists.odysseia.Odysseia;
import org.metamechanists.odysseia.items.OdysseyItemManager;
import org.metamechanists.odysseia.utils.StoreManager;

import java.lang.reflect.Method;
import java.time.*;
import java.util.*;

/** Adaptadores de producción para las acciones tipadas del catálogo. */
public final class BukkitPurchaseRuntime implements PurchaseActionRuntime {
    private final Odysseia plugin;
    private final KitDeliveryService kits;

    public BukkitPurchaseRuntime(Odysseia plugin) { this.plugin = plugin; this.kits = new KitDeliveryService(plugin); }
    /**
     * Compatibilidad legacy cerrada: nunca crea una identidad offline por nombre.
     * La ruta productiva usa PlayerIdentityResolver y sus identidades observadas.
     */
    @Override public UUID resolveUuid(String player) {
        Player target = Bukkit.getPlayerExact(player);
        return target != null && target.isOnline() ? target.getUniqueId() : null;
    }
    @Override public boolean isOnline(String player) { Player target = Bukkit.getPlayerExact(player); return target != null && target.isOnline(); }

    @Override public ActionResult execute(ExecutionContext context, ProductAction action) {
        try {
            return switch (action.type()) {
                case LUCKPERMS_TEMPORARY, SFMASTER_PASS -> temporaryGroup(context, action);
                case LUCKPERMS_PERMANENT -> permanentGroup(context, action);
                case TEMPORARY_PERMISSION, AURA, POWER, COSMETIC -> permission(context, action);
                case ECONOMY -> economy(context, action);
                case KIT -> online(context, player -> kits.deliver(player, action.parameters().get("kit"), context.transaction()));
                case PROTECTION_STONE -> protectionStone(context, action);
                case WEAPON -> weapon(context, action);
                case NOTIFICATION -> online(context, player -> { player.sendMessage(color(action.parameters().getOrDefault("message", "&aCompra entregada."))); return ActionResult.completed("notified"); });
                case ANNOUNCEMENT -> { StoreManager.announcePurchase(plugin, context.player(), action.parameters().getOrDefault("product", context.product().name())); yield ActionResult.completed("announced"); }
                case MANUAL -> ActionResult.manual(action.parameters().getOrDefault("reason", "Revisión manual"));
                case CONSOLE_COMMAND -> controlledCommand(context, action);
            };
        } catch (Exception error) { return ActionResult.retryable(error.getClass().getSimpleName() + ": " + error.getMessage()); }
    }

    @Override public ActionResult revoke(ExecutionContext context, ProductAction action, String previousResult) {
        try {
            return switch (action.type()) {
                case LUCKPERMS_TEMPORARY, SFMASTER_PASS -> restoreTemporaryGroup(context, action, previousResult);
                case LUCKPERMS_PERMANENT -> revokePermanentGroup(context, action, previousResult);
                case TEMPORARY_PERMISSION, AURA, POWER, COSMETIC -> revokePermission(context, action, previousResult);
                default -> ActionResult.manual("Acción no revocable automáticamente: " + action.type());
            };
        } catch (Exception error) { return ActionResult.retryable(error.getMessage()); }
    }

    private ActionResult temporaryGroup(ExecutionContext context, ProductAction action) {
        LuckPerms api = luckPerms(); if (api == null) return ActionResult.retryable("LuckPerms no disponible");
        User user = api.getUserManager().loadUser(context.uuid()).join();
        String group = action.parameters().get("group"); Duration duration = duration(action.parameters().get("duration"));
        if (api.getGroupManager().getGroup(group) == null) return ActionResult.manual("Grupo inexistente: " + group);
        long now = Instant.now().getEpochSecond(); long previous = 0;
        List<InheritanceNode> existing = user.getNodes().stream().filter(InheritanceNode.class::isInstance).map(InheritanceNode.class::cast)
                .filter(node -> node.getGroupName().equalsIgnoreCase(group)).toList();
        for (InheritanceNode node : existing) {
            if (!node.hasExpiry()) return ActionResult.completed("group=" + group + ";permanent=true");
            previous = Math.max(previous, node.getExpiry().getEpochSecond()); user.data().remove(node);
        }
        long expiry = Math.max(now, previous) + duration.getSeconds();
        user.data().add(InheritanceNode.builder(group).expiry(Instant.ofEpochSecond(expiry)).build()); api.getUserManager().saveUser(user).join();
        return ActionResult.completed("group=" + group + ";previous=" + previous + ";new=" + expiry);
    }

    private ActionResult restoreTemporaryGroup(ExecutionContext context, ProductAction action, String result) {
        if (result != null && result.contains("permanent=true")) return ActionResult.completed("Grupo permanente previo conservado");
        LuckPerms api = luckPerms(); if (api == null) return ActionResult.retryable("LuckPerms no disponible");
        User user = api.getUserManager().loadUser(context.uuid()).join(); String group = action.parameters().get("group");
        long created = value(result, "new"); long previous = value(result, "previous");
        for (InheritanceNode node : user.getNodes().stream().filter(InheritanceNode.class::isInstance).map(InheritanceNode.class::cast).filter(node -> node.getGroupName().equalsIgnoreCase(group)).toList()) {
            if (node.hasExpiry() && node.getExpiry().getEpochSecond() == created) user.data().remove(node);
        }
        if (previous > Instant.now().getEpochSecond()) user.data().add(InheritanceNode.builder(group).expiry(Instant.ofEpochSecond(previous)).build());
        api.getUserManager().saveUser(user).join(); return ActionResult.completed("Grupo temporal revertido");
    }

    private ActionResult permanentGroup(ExecutionContext context, ProductAction action) {
        LuckPerms api = luckPerms(); if (api == null) return ActionResult.retryable("LuckPerms no disponible");
        User user = api.getUserManager().loadUser(context.uuid()).join(); String group = action.parameters().get("group");
        boolean existed = user.getNodes().stream().filter(InheritanceNode.class::isInstance).map(InheritanceNode.class::cast).anyMatch(node -> node.getGroupName().equalsIgnoreCase(group) && !node.hasExpiry());
        if (!existed) { user.data().add(InheritanceNode.builder(group).build()); api.getUserManager().saveUser(user).join(); }
        return ActionResult.completed("group=" + group + ";preexisting=" + existed);
    }

    private ActionResult revokePermanentGroup(ExecutionContext context, ProductAction action, String result) {
        if (result != null && result.contains("preexisting=true")) return ActionResult.completed("Grupo previo conservado");
        LuckPerms api = luckPerms(); if (api == null) return ActionResult.retryable("LuckPerms no disponible");
        User user = api.getUserManager().loadUser(context.uuid()).join(); String group = action.parameters().get("group");
        user.data().remove(InheritanceNode.builder(group).build()); api.getUserManager().saveUser(user).join(); return ActionResult.completed("Grupo revocado");
    }

    private ActionResult permission(ExecutionContext context, ProductAction action) {
        String permission = action.parameters().get("permission");
        if (permission == null) return ActionResult.manual("Permiso no configurado para " + action.type());
        LuckPerms api = luckPerms(); if (api == null) return ActionResult.retryable("LuckPerms no disponible");
        User user = api.getUserManager().loadUser(context.uuid()).join(); Node node = PermissionNode.builder(permission).build();
        boolean existed = user.getNodes().contains(node); if (!existed) { user.data().add(node); api.getUserManager().saveUser(user).join(); }
        return ActionResult.completed("permission=" + permission + ";preexisting=" + existed);
    }

    private ActionResult revokePermission(ExecutionContext context, ProductAction action, String result) {
        if (result != null && result.contains("preexisting=true")) return ActionResult.completed("Permiso previo conservado");
        LuckPerms api = luckPerms(); if (api == null) return ActionResult.retryable("LuckPerms no disponible");
        User user = api.getUserManager().loadUser(context.uuid()).join(); user.data().remove(PermissionNode.builder(action.parameters().get("permission")).build());
        api.getUserManager().saveUser(user).join(); return ActionResult.completed("Permiso revocado");
    }

    private ActionResult economy(ExecutionContext context, ProductAction action) {
        RegisteredServiceProvider<Economy> registration = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (registration == null) return ActionResult.retryable("Vault Economy no disponible");
        double amount = Double.parseDouble(action.parameters().get("amount"));
        Economy economy = registration.getProvider();
        OfflinePlayer target = Bukkit.getOfflinePlayer(context.uuid());
        if (!economy.hasAccount(target) && !economy.createPlayerAccount(target)) {
            return ActionResult.retryable("Vault no pudo crear cuenta para " + context.player());
        }
        var response = economy.depositPlayer(target, amount);
        return response.transactionSuccess() ? ActionResult.completed("amount=" + amount + ";balance=" + response.balance) : ActionResult.retryable(response.errorMessage);
    }

    private ActionResult protectionStone(ExecutionContext context, ProductAction action) {
        if (!Bukkit.getPluginManager().isPluginEnabled("ProtectionStones")) return ActionResult.retryable("ProtectionStones no disponible");
        return online(context, player -> {
            String key = action.parameters().get("alias");
            String alias = plugin.getConfig().getString("protectionstones.aliases." + key, key);
            int amount = Integer.parseInt(action.parameters().getOrDefault("amount", "1"));
            if (amount < 1 || amount > 2304) return ActionResult.manual("Cantidad ProtectionStones inválida");
            return giveProtectionStone(player, alias, amount);
        });
    }

    /**
     * Usa la API real de ProtectionStones en vez de aceptar ciegamente un comando
     * de consola. La reflexión mantiene el plugin opcional sin acoplar el JAR al
     * classpath de compilación de Odysseia.
     */
    private ActionResult giveProtectionStone(Player player, String alias, int amount) {
        try {
            Class<?> protectionStones = Class.forName("dev.espi.protectionstones.ProtectionStones");
            Object block = protectionStones.getMethod("getProtectBlockFromAlias", String.class).invoke(null, alias);
            if (block == null) return ActionResult.manual("Alias ProtectionStones inexistente: " + alias);
            ItemStack prototype = (ItemStack) block.getClass().getMethod("createItem").invoke(block);
            if (prototype == null || prototype.getType().isAir()) return ActionResult.manual("ProtectionStones no creó un ítem para " + alias);
            if (!hasInventoryCapacity(player, prototype, amount)) return ActionResult.waiting("Inventario sin espacio para ProtectionStone " + alias);

            int remaining = amount;
            while (remaining > 0) {
                ItemStack item = prototype.clone();
                int stack = Math.min(remaining, item.getMaxStackSize());
                item.setAmount(stack);
                if (!player.getInventory().addItem(item).isEmpty()) return ActionResult.retryable("Inventario cambió durante la entrega de " + alias);
                remaining -= stack;
            }
            return ActionResult.completed("alias=" + alias + ";amount=" + amount);
        } catch (ReflectiveOperationException error) {
            return ActionResult.retryable("API ProtectionStones no disponible: " + error.getClass().getSimpleName());
        }
    }

    private boolean hasInventoryCapacity(Player player, ItemStack prototype, int amount) {
        int capacity = 0;
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType().isAir()) capacity += prototype.getMaxStackSize();
            else if (slot.isSimilar(prototype)) capacity += Math.max(0, slot.getMaxStackSize() - slot.getAmount());
            if (capacity >= amount) return true;
        }
        return false;
    }

    private ActionResult weapon(ExecutionContext context, ProductAction action) {
        return online(context, player -> {
            try {
                String factory = action.parameters().get("factory");
                if (factory == null || !factory.matches("create[A-Za-z0-9]+")) return ActionResult.manual("Factory de arma inválida");
                Method method = OdysseyItemManager.class.getMethod(factory);
                ItemStack item = (ItemStack) method.invoke(null);
                if (player.getInventory().firstEmpty() < 0) return ActionResult.waiting("Inventario lleno");
                player.getInventory().addItem(item);
                return ActionResult.completed("factory=" + factory);
            } catch (ReflectiveOperationException error) {
                return ActionResult.manual("Factory de arma no disponible: " + error.getMessage());
            }
        });
    }

    private ActionResult controlledCommand(ExecutionContext context, ProductAction action) {
        String command = action.parameters().getOrDefault("command", "").replace("{player}", context.player());
        if (!command.matches("(?:ps give|odykitgive|lp user|msg) [A-Za-z0-9_ .&áéíóúÁÉÍÓÚ-]+")) return ActionResult.manual("Comando fuera de allowlist");
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command) ? ActionResult.completed("command-dispatched") : ActionResult.retryable("Comando rechazado");
    }

    private ActionResult online(ExecutionContext context, java.util.function.Function<Player, ActionResult> operation) {
        Player player = Bukkit.getPlayerExact(context.player()); return player == null ? ActionResult.waiting("Jugador offline") : operation.apply(player);
    }
    private LuckPerms luckPerms() { RegisteredServiceProvider<LuckPerms> registration = Bukkit.getServicesManager().getRegistration(LuckPerms.class); return registration == null ? null : registration.getProvider(); }
    private Duration duration(String value) { long amount = Long.parseLong(value.substring(0, value.length()-1)); return switch(value.charAt(value.length()-1)){case 'm'->Duration.ofMinutes(amount);case 'h'->Duration.ofHours(amount);case 'd'->Duration.ofDays(amount);default->throw new IllegalArgumentException("Duración inválida");}; }
    private long value(String result, String key) { if (result == null) return 0; for(String part:result.split(";")) if(part.startsWith(key+"=")) return Long.parseLong(part.substring(key.length()+1)); return 0; }
    private String color(String value) { return ChatColor.translateAlternateColorCodes('&', value); }
}
