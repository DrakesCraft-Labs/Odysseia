package org.metamechanists.odysseia.boss.combat;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Defines the attack families available to every production boss. */
public record BossCombatProfile(Set<AttackFamily> families) {

    public enum AttackFamily {
        AERIAL,
        GROUND,
        RANGED
    }

    private static final BossCombatProfile HYBRID = profile(
            AttackFamily.AERIAL, AttackFamily.GROUND, AttackFamily.RANGED);
    private static final Map<String, BossCombatProfile> PROFILES = Map.ofEntries(
            Map.entry("circe", profile(AttackFamily.RANGED, AttackFamily.AERIAL)),
            Map.entry("polifemo", profile(AttackFamily.GROUND)),
            Map.entry("dios_corrupto", HYBRID),
            Map.entry("thor", profile(AttackFamily.GROUND, AttackFamily.RANGED)),
            Map.entry("ares", profile(AttackFamily.GROUND, AttackFamily.AERIAL)),
            Map.entry("hades", profile(AttackFamily.RANGED, AttackFamily.GROUND)),
            Map.entry("poseidon", profile(AttackFamily.RANGED, AttackFamily.AERIAL)),
            Map.entry("zeus", profile(AttackFamily.RANGED, AttackFamily.AERIAL)),
            Map.entry("loki", profile(AttackFamily.RANGED, AttackFamily.AERIAL)),
            Map.entry("odin", HYBRID),
            Map.entry("kratos", profile(AttackFamily.GROUND, AttackFamily.AERIAL)),
            Map.entry("heimdall", profile(AttackFamily.RANGED, AttackFamily.GROUND)),
            Map.entry("hidra", profile(AttackFamily.GROUND, AttackFamily.RANGED)),
            Map.entry("cerbero", profile(AttackFamily.GROUND, AttackFamily.AERIAL)),
            Map.entry("artemisa", profile(AttackFamily.RANGED, AttackFamily.AERIAL)),
            Map.entry("tifon", HYBRID),
            Map.entry("prometeo", profile(AttackFamily.AERIAL, AttackFamily.RANGED)),
            Map.entry("coloso_end", profile(AttackFamily.GROUND, AttackFamily.AERIAL)),
            Map.entry("wither_storm", HYBRID),
            Map.entry("dragon_ancestral", profile(AttackFamily.AERIAL, AttackFamily.RANGED))
    );

    public BossCombatProfile {
        families = Set.copyOf(families);
    }

    public static BossCombatProfile forBoss(String bossId) {
        return PROFILES.getOrDefault(normalize(bossId), HYBRID);
    }

    public static Set<String> configuredBossIds() {
        return PROFILES.keySet();
    }

    private static BossCombatProfile profile(AttackFamily first, AttackFamily... rest) {
        EnumSet<AttackFamily> families = EnumSet.of(first, rest);
        return new BossCombatProfile(families);
    }

    private static String normalize(String bossId) {
        return bossId == null ? "" : bossId.toLowerCase(java.util.Locale.ROOT).replace('-', '_');
    }
}
