package org.metamechanists.odysseia.purchase;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.*;

/** Resolves only server-observed identities and never creates Bukkit offline profiles. */
public final class PlayerIdentityResolver {
    private final PurchaseRepository repository;
    private final PurchaseActionRuntime legacyRuntime;

    public PlayerIdentityResolver(PurchaseRepository repository) { this.repository = repository; this.legacyRuntime = null; }
    private PlayerIdentityResolver(PurchaseActionRuntime runtime) { this.repository = null; this.legacyRuntime = runtime; }
    static PlayerIdentityResolver legacy(PurchaseActionRuntime runtime) { return new PlayerIdentityResolver(runtime); }

    public void observe(UUID uuid, String canonicalName) throws SQLException {
        if (repository == null) return;
        String platform = detectPlatform(uuid, canonicalName);
        repository.observeIdentity(uuid, canonicalName, platform, "PLAYER_JOIN", "HIGH");
        repository.observeAlias(uuid, canonicalName, "CANONICAL", "HIGH");
        if ("BEDROCK".equals(platform) && canonicalName.startsWith(".")) repository.observeAlias(uuid, canonicalName.substring(1), "BEDROCK_NORMALIZED", "HIGH");
    }

    public IdentityResolution resolve(String requested) throws SQLException {
        if (requested == null || !requested.matches("\\.?[A-Za-z0-9_]{3,16}")) return IdentityResolution.result(IdentityResolutionStatus.INVALID_INPUT, "Nick inválido", List.of());
        if (legacyRuntime != null) {
            UUID uuid = legacyRuntime.resolveUuid(requested);
            return uuid == null ? IdentityResolution.result(IdentityResolutionStatus.UNKNOWN, "UUID no disponible", List.of())
                    : new IdentityResolution(IdentityResolutionStatus.RESOLVED, uuid, requested, requested.startsWith(".") ? "BEDROCK" : "JAVA", "TEST", "LEGACY_TEST", "Runtime de prueba", List.of(uuid));
        }
        Optional<PurchaseRepository.PlayerIdentity> exact = repository.findIdentityByCanonical(requested);
        if (exact.isPresent()) {
            if (!requested.startsWith(".")) {
                List<PurchaseRepository.PlayerIdentity> bedrock = repository.findByAlias(requested, "BEDROCK_NORMALIZED");
                if (bedrock.stream().anyMatch(candidate -> !candidate.uuid().equals(exact.get().uuid()))) {
                    List<PurchaseRepository.PlayerIdentity> candidates = new ArrayList<>(bedrock); candidates.add(exact.get());
                    return ambiguous("Coinciden identidades Java y Bedrock", candidates);
                }
            }
            return IdentityResolution.resolved(exact.get(), "CANONICAL_EXACT");
        }
        List<PurchaseRepository.PlayerIdentity> aliases = repository.findByAlias(requested);
        if (aliases.size() == 1) return IdentityResolution.resolved(aliases.getFirst(), "ALIAS_EXACT");
        if (aliases.size() > 1) return ambiguous("Alias ambiguo", aliases);
        if (!requested.startsWith(".")) {
            List<PurchaseRepository.PlayerIdentity> bedrock = repository.findByAlias(requested, "BEDROCK_NORMALIZED");
            if (bedrock.size() == 1 && "HIGH".equals(bedrock.getFirst().confidence())) return IdentityResolution.resolved(bedrock.getFirst(), "BEDROCK_NORMALIZED");
            if (bedrock.size() > 1) return ambiguous("Alias Bedrock ambiguo", bedrock);
        }
        return IdentityResolution.result(IdentityResolutionStatus.MANUAL_REVIEW, "Sin evidencia suficiente para vincular el nick", List.of());
    }

    private IdentityResolution ambiguous(String detail, List<PurchaseRepository.PlayerIdentity> candidates) {
        return IdentityResolution.result(IdentityResolutionStatus.AMBIGUOUS, detail, candidates.stream().map(PurchaseRepository.PlayerIdentity::uuid).toList());
    }

    private String detectPlatform(UUID uuid, String name) {
        if (name.startsWith(".")) return "BEDROCK";
        try {
            Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            Method isFloodgate = api.getMethod("isFloodgatePlayer", UUID.class);
            return Boolean.TRUE.equals(isFloodgate.invoke(instance, uuid)) ? "BEDROCK" : "JAVA";
        } catch (ReflectiveOperationException ignored) { return "UNKNOWN"; }
    }
}
