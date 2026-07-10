package org.metamechanists.odysseia.purchase;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.metamechanists.odysseia.Odysseia;

import java.io.File;
import java.util.*;

/** Carga y valida la fuente canónica de productos entregables. */
public final class ProductCatalog {
    private final Map<String, ProductDefinition> products;
    private final int expectedCount;

    public ProductCatalog(Odysseia plugin) {
        File file = new File(plugin.getDataFolder(), "purchases.yml");
        if (!file.exists()) plugin.saveResource("purchases.yml", false);
        this.products = Collections.unmodifiableMap(load(file));
        this.expectedCount = 23;
        ensureValid();
    }

    public ProductCatalog(File file) {
        this.products = Collections.unmodifiableMap(load(file));
        this.expectedCount = 23;
        ensureValid();
    }

    public ProductCatalog(Collection<ProductDefinition> definitions) {
        Map<String, ProductDefinition> loaded = new LinkedHashMap<>();
        definitions.forEach(product -> loaded.put(product.id(), product));
        this.products = Collections.unmodifiableMap(loaded);
        this.expectedCount = -1;
        ensureValid();
    }

    private void ensureValid() {
        List<String> errors = validate();
        if (!errors.isEmpty()) throw new IllegalStateException("Catálogo inválido: " + String.join("; ", errors));
    }

    private Map<String, ProductDefinition> load(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("products");
        if (root == null) throw new IllegalStateException("Falta products en purchases.yml");
        Map<String, ProductDefinition> result = new LinkedHashMap<>();
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;
            List<ProductAction> actions = new ArrayList<>();
            for (Map<?, ?> raw : section.getMapList("actions")) {
                String actionId = text(raw.get("id"));
                ActionType type = ActionType.valueOf(text(raw.get("type")).toUpperCase(Locale.ROOT));
                boolean online = Boolean.parseBoolean(text(raw.containsKey("requires-online") ? raw.get("requires-online") : false));
                boolean required = !"false".equalsIgnoreCase(text(raw.containsKey("required") ? raw.get("required") : true));
                RefundPolicy refund = RefundPolicy.valueOf(text(raw.containsKey("refund") ? raw.get("refund") : "MANUAL_REVIEW").toUpperCase(Locale.ROOT));
                Map<String, String> parameters = new LinkedHashMap<>();
                Object params = raw.get("parameters");
                if (params instanceof Map<?, ?> map) map.forEach((key, value) -> parameters.put(text(key), text(value)));
                actions.add(new ProductAction(actionId, type, Map.copyOf(parameters), online, refund, required));
            }
            result.put(id, new ProductDefinition(id, section.getInt("tebex-package-id"), section.getString("name", id),
                    section.getString("category", "other"), section.getString("description", ""),
                    section.getString("source", "unknown"), section.getInt("version", 1),
                    VerificationState.valueOf(section.getString("verification", "UNVERIFIED_PRODUCTION")),
                    List.copyOf(section.getStringList("dependencies")), List.copyOf(actions)));
        }
        return result;
    }

    private String text(Object value) { return String.valueOf(value == null ? "" : value); }
    public ProductDefinition get(String id) { return products.get(id); }
    public Collection<ProductDefinition> all() { return products.values(); }

    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (expectedCount > 0 && products.size() != expectedCount) errors.add("se esperaban " + expectedCount + " productos y hay " + products.size());
        Set<Integer> packageIds = new HashSet<>();
        for (ProductDefinition product : products.values()) {
            if (!product.id().matches("[a-z0-9_]+")) errors.add(product.id() + ": id inestable");
            if (product.tebexPackageId() <= 0 || !packageIds.add(product.tebexPackageId())) errors.add(product.id() + ": package Tebex inválido/duplicado");
            if (product.actions().isEmpty()) errors.add(product.id() + ": sin acciones");
            Set<String> actionIds = new HashSet<>();
            for (ProductAction action : product.actions()) if (action.id().isBlank() || !actionIds.add(action.id())) errors.add(product.id() + ": action id vacío/duplicado");
        }
        return errors;
    }
}
