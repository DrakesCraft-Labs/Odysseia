package org.metamechanists.odysseia.papa;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Entrega el equipo legendario de la Papa de mar.
 *
 * La idea, en palabras de Jack: diamante que aguante como la netherita, encantado por encima de lo
 * que permite una mesa, y raro de una forma que solo tiene sentido si conoces la historia de la
 * papa.
 *
 * La dureza no se consigue cambiando el material sino con **modificadores de atributo** sobre la
 * pieza de diamante: se le suma la diferencia de resistencia y de dureza que tendria la netherita,
 * mas su resistencia al empuje. El item sigue siendo diamante --se ve azul, se repara con
 * diamantes-- pero encaja los golpes como el otro. De ahi lo bizarro.
 *
 * Es un comando de consola: lo invoca el canje, no el jugador. Sin permiso desde el juego.
 */
public final class PapaEquipoCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public PapaEquipoCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player && !sender.hasPermission("odysseia.admin")) {
            sender.sendMessage(ChatColor.RED + "Este comando lo usa el trueque, no tu.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("Uso: /papaequipo <jugador> <armadura|herramientas>");
            return true;
        }
        Player jugador = plugin.getServer().getPlayerExact(args[0]);
        if (jugador == null) {
            sender.sendMessage(ChatColor.RED + "Jugador no conectado: " + args[0]);
            return true;
        }

        List<ItemStack> entrega = switch (args[1].toLowerCase(java.util.Locale.ROOT)) {
            case "armadura" -> armadura();
            case "herramientas" -> herramientas();
            default -> List.of();
        };
        if (entrega.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Equipo desconocido: " + args[1]);
            return true;
        }

        for (ItemStack pieza : entrega) {
            // Lo que no quepa cae al suelo: es preferible a que se pierda un premio de este precio.
            jugador.getInventory().addItem(pieza).values()
                    .forEach(sobra -> jugador.getWorld().dropItemNaturally(jugador.getLocation(), sobra));
        }
        jugador.sendMessage(color("&6&l✦ &eEl mar te reconoce. Has recibido tu equipo legendario. &6&l✦"));
        plugin.getLogger().info("[Papa] Equipo '" + args[1] + "' entregado a " + jugador.getName());
        return true;
    }

    private List<ItemStack> armadura() {
        return List.of(
                pieza(Material.DIAMOND_HELMET, "Yelmo", 3.0D, 2.0D, 0.1D),
                pieza(Material.DIAMOND_CHESTPLATE, "Coraza", 8.0D, 3.0D, 0.1D),
                pieza(Material.DIAMOND_LEGGINGS, "Grebas", 6.0D, 3.0D, 0.1D),
                pieza(Material.DIAMOND_BOOTS, "Botas", 3.0D, 2.0D, 0.1D));
    }

    /**
     * Una pieza de armadura de diamante que encaja como netherita.
     *
     * Se le vuelven a declarar los atributos completos --armadura, dureza y resistencia al
     * empuje-- porque en cuanto se anade un modificador manual, Minecraft deja de aplicar los
     * valores por defecto de la pieza. Si solo se sumara la diferencia, quedaria en calzoncillos.
     */
    private ItemStack pieza(Material material, String nombre, double armadura,
                            double dureza, double empuje) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(color("&6&l" + nombre + " de la Papa de mar"));
        meta.setLore(List.of(
                color("&7Diamante por fuera. Otra cosa por dentro."),
                color("&7Aguanta como la netherita y no arde."),
                color(""),
                color("&8Forjada con dos inventarios de papas."),
                color("&8Nadie sabe como. Nadie pregunta.")));

        aplicar(meta, Attribute.ARMOR, "papa_armadura", armadura);
        aplicar(meta, Attribute.ARMOR_TOUGHNESS, "papa_dureza", dureza);
        aplicar(meta, Attribute.KNOCKBACK_RESISTANCE, "papa_empuje", empuje);

        meta.addEnchant(Enchantment.PROTECTION, 6, true);
        meta.addEnchant(Enchantment.UNBREAKING, 10, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);
        meta.addEnchant(Enchantment.THORNS, 4, true);
        if (material == Material.DIAMOND_HELMET) meta.addEnchant(Enchantment.RESPIRATION, 5, true);
        if (material == Material.DIAMOND_BOOTS) {
            meta.addEnchant(Enchantment.FEATHER_FALLING, 6, true);
            meta.addEnchant(Enchantment.DEPTH_STRIDER, 3, true);
        }

        rematar(meta);
        item.setItemMeta(meta);
        return item;
    }

    private List<ItemStack> herramientas() {
        return List.of(
                herramienta(Material.DIAMOND_PICKAXE, "Pico", Enchantment.EFFICIENCY, 8),
                herramienta(Material.DIAMOND_AXE, "Hacha", Enchantment.EFFICIENCY, 8),
                herramienta(Material.DIAMOND_SHOVEL, "Pala", Enchantment.EFFICIENCY, 8));
    }

    private ItemStack herramienta(Material material, String nombre, Enchantment principal, int nivel) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(color("&6&l" + nombre + " de la Papa de mar"));
        meta.setLore(List.of(
                color("&7Muerde la piedra como si fuera mantequilla."),
                color("&7Y nunca se gasta del todo."),
                color(""),
                color("&8Un inventario entero de papas.")));

        meta.addEnchant(principal, nivel, true);
        meta.addEnchant(Enchantment.UNBREAKING, 10, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);
        meta.addEnchant(Enchantment.FORTUNE, 5, true);

        rematar(meta);
        item.setItemMeta(meta);
        return item;
    }

    /** Un modificador de atributo fijo, sin depender del hueco donde se lleve. */
    private void aplicar(ItemMeta meta, Attribute atributo, String nombre, double valor) {
        meta.addAttributeModifier(atributo, new AttributeModifier(
                new NamespacedKey(plugin, nombre + "_" + UUID.randomUUID()),
                valor, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ARMOR));
    }

    /** Los detalles comunes: brillo limpio, sin numeros feos y a prueba de fuego. */
    private void rematar(ItemMeta meta) {
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.setUnbreakable(true);
        meta.setFireResistant(true);
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "papa_legendario"),
                org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
        if (meta instanceof Damageable damageable) damageable.setDamage(0);
    }

    private static String color(String texto) {
        return ChatColor.translateAlternateColorCodes('&', texto);
    }
}
