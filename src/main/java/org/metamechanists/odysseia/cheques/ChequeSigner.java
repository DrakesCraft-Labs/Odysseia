package org.metamechanists.odysseia.cheques;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Set;

/** Firma y valida los datos inmutables de un cheque sin exponer la clave secreta. */
public final class ChequeSigner {

    public static final int VERSION = 1;
    public static final Set<Long> ALLOWED_AMOUNTS = Set.of(5_000L, 10_000L, 25_000L, 50_000L);
    private final byte[] secret;

    public ChequeSigner(byte[] secret) {
        if (secret == null || secret.length < 32) {
            throw new IllegalArgumentException("La clave de cheques debe tener al menos 32 bytes");
        }
        this.secret = secret.clone();
    }

    public byte[] sign(long amount, String serial) {
        if (!ALLOWED_AMOUNTS.contains(amount) || serial == null || serial.isBlank()) {
            throw new IllegalArgumentException("Cheque con datos no permitidos");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(payload(amount, serial));
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("No se pudo firmar el cheque", error);
        }
    }

    public boolean verify(long amount, String serial, byte[] signature) {
        if (!ALLOWED_AMOUNTS.contains(amount) || serial == null || signature == null) return false;
        try {
            return MessageDigest.isEqual(sign(amount, serial), signature);
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    private static byte[] payload(long amount, String serial) {
        return (VERSION + "|" + amount + "|" + serial).getBytes(StandardCharsets.UTF_8);
    }
}
