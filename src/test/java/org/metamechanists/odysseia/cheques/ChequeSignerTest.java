package org.metamechanists.odysseia.cheques;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChequeSignerTest {

    private final ChequeSigner signer = new ChequeSigner(new byte[32]);

    @Test
    void acceptsAuthenticCheque() {
        byte[] signature = signer.sign(10_000, "serial-unico");
        assertTrue(signer.verify(10_000, "serial-unico", signature));
    }

    @Test
    void rejectsTamperedAmountOrSerial() {
        byte[] signature = signer.sign(10_000, "serial-unico");
        assertFalse(signer.verify(25_000, "serial-unico", signature));
        assertFalse(signer.verify(10_000, "otra-serie", signature));
    }

    @Test
    void rejectsUnapprovedDenomination() {
        assertThrows(IllegalArgumentException.class, () -> signer.sign(30_000, "serial"));
    }
}
