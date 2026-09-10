package org.metamechanists.odysseia.services;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Contrato estricto del manifiesto que genera la integración de despliegues. */
final class ReleaseManifest {

    private static final Gson GSON = new Gson();
    private static final Set<String> ENVELOPE_FIELDS = Set.of("payload", "signature");
    private static final Set<String> PAYLOAD_FIELDS = Set.of(
            "schema", "releaseId", "state", "playerSummary", "technicalDetails", "health");
    private static final Set<String> DETAIL_FIELDS = Set.of(
            "ticket", "commit", "artifact", "sha256", "backupVerified", "validation");
    private static final Set<String> HEALTH_FIELDS = Set.of("status", "summary");
    private static final Pattern RELEASE_ID = Pattern.compile("[a-z0-9][a-z0-9._-]{7,79}");
    private static final Pattern COMMIT = Pattern.compile("[0-9a-f]{7,40}");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final Pattern UUID = Pattern.compile(
            "(?i).*\\b[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\\b.*");
    private static final Pattern IPV4 = Pattern.compile(".*\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b.*");
    private static final Pattern COORDINATES = Pattern.compile(
            "(?i).*(?:\\bx\\s*[:=]|\\by\\s*[:=]|\\bz\\s*[:=]|\\b-?\\d{1,8}\\s*,\\s*-?\\d{1,4}\\s*,\\s*-?\\d{1,8}\\b).*");
    private static final Pattern PRIVATE_PATH = Pattern.compile(
            "(?i).*(?:/home/|/plugins/|[a-z]:\\\\|discord(?:app)?\\.com/api/webhooks/).*" );

    final String releaseId;
    final String playerSummary;
    final List<Detail> technicalDetails;
    final Health health;

    private ReleaseManifest(String releaseId, String playerSummary, List<Detail> technicalDetails, Health health) {
        this.releaseId = releaseId;
        this.playerSummary = playerSummary;
        this.technicalDetails = List.copyOf(technicalDetails);
        this.health = health;
    }

    static Verification verify(String json, byte[] secret) {
        if (secret == null || secret.length < 32) {
            return Verification.invalid("secreto ausente o menor de 32 bytes");
        }
        try {
            JsonObject envelope = JsonParser.parseString(json).getAsJsonObject();
            requireOnly(envelope, ENVELOPE_FIELDS);
            JsonObject payload = envelope.getAsJsonObject("payload");
            String signature = string(envelope, "signature");
            if (payload == null || !SHA256.matcher(signature).matches()) {
                return Verification.invalid("envoltorio incompleto");
            }
            requireOnly(payload, PAYLOAD_FIELDS);
            if (payload.get("schema").getAsInt() != 1) {
                return Verification.invalid("schema no soportado");
            }

            String canonical = canonical(payload);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] expected = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
            byte[] supplied = HexFormat.of().parseHex(signature);
            if (!MessageDigest.isEqual(expected, supplied)) {
                return Verification.invalid("firma HMAC invalida");
            }

            String state = string(payload, "state");
            if (!"VERIFIED".equals(state)) {
                return Verification.ignored("estado " + state + ", solo se publica VERIFIED");
            }
            String releaseId = string(payload, "releaseId");
            if (!RELEASE_ID.matcher(releaseId).matches()) {
                return Verification.invalid("release-id invalido");
            }
            String playerSummary = safeText(string(payload, "playerSummary"), 600);
            JsonObject healthJson = payload.getAsJsonObject("health");
            requireOnly(healthJson, HEALTH_FIELDS);
            Health health = new Health(
                    safeEnum(string(healthJson, "status"), Set.of("HEALTHY", "DEGRADED")),
                    safeText(string(healthJson, "summary"), 300));

            List<Detail> details = new ArrayList<>();
            for (JsonElement element : payload.getAsJsonArray("technicalDetails")) {
                JsonObject detail = element.getAsJsonObject();
                requireOnly(detail, DETAIL_FIELDS);
                int ticket = detail.get("ticket").getAsInt();
                String commit = string(detail, "commit");
                String artifact = safeArtifact(string(detail, "artifact"));
                String sha256 = string(detail, "sha256");
                if (ticket <= 0 || !COMMIT.matcher(commit).matches() || !SHA256.matcher(sha256).matches()) {
                    throw new IllegalArgumentException("detalle tecnico invalido");
                }
                details.add(new Detail(ticket, commit, artifact, sha256,
                        detail.get("backupVerified").getAsBoolean(),
                        safeText(string(detail, "validation"), 300)));
            }
            if (details.isEmpty() || details.size() > 20) {
                return Verification.invalid("cantidad de detalles invalida");
            }
            return Verification.verified(new ReleaseManifest(releaseId, playerSummary, details, health));
        } catch (Exception e) {
            return Verification.invalid("manifiesto malformado: " + e.getClass().getSimpleName());
        }
    }

    /** Representación estable que debe firmar también el productor. */
    static String canonical(JsonObject payload) {
        JsonObject stable = new JsonObject();
        stable.add("schema", payload.get("schema"));
        stable.add("releaseId", payload.get("releaseId"));
        stable.add("state", payload.get("state"));
        stable.add("playerSummary", payload.get("playerSummary"));
        var stableDetails = new com.google.gson.JsonArray();
        for (JsonElement element : payload.getAsJsonArray("technicalDetails")) {
            JsonObject source = element.getAsJsonObject();
            JsonObject detail = new JsonObject();
            detail.add("ticket", source.get("ticket"));
            detail.add("commit", source.get("commit"));
            detail.add("artifact", source.get("artifact"));
            detail.add("sha256", source.get("sha256"));
            detail.add("backupVerified", source.get("backupVerified"));
            detail.add("validation", source.get("validation"));
            stableDetails.add(detail);
        }
        stable.add("technicalDetails", stableDetails);
        JsonObject healthSource = payload.getAsJsonObject("health");
        JsonObject health = new JsonObject();
        health.add("status", healthSource.get("status"));
        health.add("summary", healthSource.get("summary"));
        stable.add("health", health);
        return GSON.toJson(stable);
    }

    private static void requireOnly(JsonObject object, Set<String> allowed) {
        if (object == null || !allowed.containsAll(object.keySet()) || !object.keySet().containsAll(allowed)) {
            throw new IllegalArgumentException("campos inesperados o ausentes");
        }
    }

    private static String string(JsonObject object, String key) {
        String value = object.get(key).getAsString();
        if (value == null || value.isBlank()) throw new IllegalArgumentException("campo vacio");
        return value;
    }

    private static String safeEnum(String value, Set<String> allowed) {
        if (!allowed.contains(value)) throw new IllegalArgumentException("valor no permitido");
        return value;
    }

    private static String safeArtifact(String value) {
        if (value.length() > 100 || value.contains("/") || value.contains("\\") || !value.endsWith(".jar")) {
            throw new IllegalArgumentException("artefacto invalido");
        }
        return safeText(value, 100);
    }

    private static String safeText(String value, int maxLength) {
        if (value.length() > maxLength || value.chars().anyMatch(c -> Character.isISOControl(c) && c != '\n')
                || UUID.matcher(value).matches() || IPV4.matcher(value).matches()
                || COORDINATES.matcher(value).matches() || PRIVATE_PATH.matcher(value).matches()) {
            throw new IllegalArgumentException("texto potencialmente privado");
        }
        return value;
    }

    record Detail(int ticket, String commit, String artifact, String sha256,
                  boolean backupVerified, String validation) {}
    record Health(String status, String summary) {}

    record Verification(Status status, ReleaseManifest manifest, String reason) {
        enum Status { VERIFIED, IGNORED, INVALID }
        static Verification verified(ReleaseManifest manifest) {
            return new Verification(Status.VERIFIED, manifest, null);
        }
        static Verification ignored(String reason) {
            return new Verification(Status.IGNORED, null, reason);
        }
        static Verification invalid(String reason) {
            return new Verification(Status.INVALID, null, reason);
        }
    }
}
