package org.metamechanists.odysseia.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReleaseManifestTest {

    private static final byte[] SECRET = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);

    @Test
    void aceptaVerifiedFirmadoYExponeSoloCamposTecnicosAcotados() throws Exception {
        ReleaseManifest.Verification result = ReleaseManifest.verify(envelope(payload("VERIFIED")), SECRET);

        assertEquals(ReleaseManifest.Verification.Status.VERIFIED, result.status());
        assertNotNull(result.manifest());
        assertEquals("saori-2026.09.10-001", result.manifest().releaseId);
        assertEquals(375, result.manifest().technicalDetails.getFirst().ticket());
        assertEquals("0123456789ab", result.manifest().technicalDetails.getFirst().sha256().substring(0, 12));
    }

    @Test
    void noPublicaStagedAunqueLaFirmaSeaValida() throws Exception {
        ReleaseManifest.Verification result = ReleaseManifest.verify(envelope(payload("STAGED")), SECRET);

        assertEquals(ReleaseManifest.Verification.Status.IGNORED, result.status());
        assertNull(result.manifest());
    }

    @Test
    void rechazaFirmaAlterada() throws Exception {
        String envelope = envelope(payload("VERIFIED")).replace("Cambio seguro", "Cambio adulterado");

        assertEquals(ReleaseManifest.Verification.Status.INVALID,
                ReleaseManifest.verify(envelope, SECRET).status());
    }

    @Test
    void rechazaUuidCoordenadasRutasYCamposDeJugador() throws Exception {
        String uuid = payload("VERIFIED").replace("Cambio seguro",
                "UUID 123e4567-e89b-42d3-a456-426614174000");
        String coordinates = payload("VERIFIED").replace("Cambio seguro", "bloque x=12 y=64 z=-8");
        String path = payload("VERIFIED").replace("Cambio seguro", "respaldo /plugins/Privado.jar");
        String playerField = payload("VERIFIED").replace(
                "\"playerSummary\":\"Cambio seguro\"",
                "\"playerSummary\":\"Cambio seguro\",\"player\":\"identificador\"");

        assertEquals(ReleaseManifest.Verification.Status.INVALID,
                ReleaseManifest.verify(envelope(uuid), SECRET).status());
        assertEquals(ReleaseManifest.Verification.Status.INVALID,
                ReleaseManifest.verify(envelope(coordinates), SECRET).status());
        assertEquals(ReleaseManifest.Verification.Status.INVALID,
                ReleaseManifest.verify(envelope(path), SECRET).status());
        assertEquals(ReleaseManifest.Verification.Status.INVALID,
                ReleaseManifest.verify(envelope(playerField), SECRET).status());
    }

    @Test
    void firmaCanonicaNoDependeDelOrdenDeLasClaves() throws Exception {
        String original = payload("VERIFIED");
        String reordered = original.replace(
                "\"schema\":1,\"releaseId\":\"saori-2026.09.10-001\",\"state\":\"VERIFIED\"",
                "\"state\":\"VERIFIED\",\"releaseId\":\"saori-2026.09.10-001\",\"schema\":1");

        String signature = signature(JsonParser.parseString(original).getAsJsonObject());
        JsonObject envelope = new JsonObject();
        envelope.add("payload", JsonParser.parseString(reordered));
        envelope.addProperty("signature", signature);

        assertEquals(ReleaseManifest.Verification.Status.VERIFIED,
                ReleaseManifest.verify(envelope.toString(), SECRET).status());
    }

    private static String payload(String state) {
        return "{" +
                "\"schema\":1," +
                "\"releaseId\":\"saori-2026.09.10-001\"," +
                "\"state\":\"" + state + "\"," +
                "\"playerSummary\":\"Cambio seguro\"," +
                "\"technicalDetails\":[{" +
                "\"ticket\":375," +
                "\"commit\":\"abcdef1\"," +
                "\"artifact\":\"Example.jar\"," +
                "\"sha256\":\"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef\"," +
                "\"backupVerified\":true," +
                "\"validation\":\"12 pruebas locales OK\"}]," +
                "\"health\":{\"status\":\"HEALTHY\",\"summary\":\"Arranque y checks OK\"}" +
                "}";
    }

    private static String envelope(String payload) throws Exception {
        JsonObject payloadObject = JsonParser.parseString(payload).getAsJsonObject();
        JsonObject envelope = new JsonObject();
        envelope.add("payload", payloadObject);
        envelope.addProperty("signature", signature(payloadObject));
        return envelope.toString();
    }

    private static String signature(JsonObject payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET, "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(
                ReleaseManifest.canonical(payload).getBytes(StandardCharsets.UTF_8)));
    }
}
