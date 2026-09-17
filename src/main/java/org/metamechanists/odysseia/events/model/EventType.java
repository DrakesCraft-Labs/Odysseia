package org.metamechanists.odysseia.events.model;

public enum EventType {
    RUSH_MINING("Minería Flash", "⛏️"),
    RUSH_FISHING("Torneo de Pesca", "🎣"),
    RUSH_HARVEST("Papa-Maratón / Cosecha", "🥔"),
    SUPPLY_DROP("Suministros del Olimpo", "📦"),
    GLOBAL_BOOST("Multiplicador Global", "⚡"),
    WORLD_BOSS("Jefe Mundial", "👑"),
    PVP_ARENA("Torneo de Gladiadores", "⚔️");

    private final String displayName;
    private final String icon;

    EventType(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }
}
