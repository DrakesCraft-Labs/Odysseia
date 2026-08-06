package org.metamechanists.odysseia.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * El anuncio de compra tenia el webhook de Discord validado al principio y con {@code return}, asi
 * que un webhook sin configurar --el estado por defecto-- dejaba al comprador sin su anuncio en el
 * chat aunque el producto se hubiera entregado bien. Rojo lo vio el 2026-08-06.
 *
 * Estas pruebas fijan la separacion: el motivo por el que Discord no esta disponible se calcula
 * aparte y no puede volver a decidir si el jugador ve su anuncio.
 */
class StoreAnnouncementTest {

    private static final String TEXTO = "{player} compro {product}";
    private static final String VALIDO = "https://discord.com/api/webhooks/123456789/abcdefghijklmnop";

    @Test
    void elValorPorDefectoDeLaConfigCuentaComoNoConfigurado() {
        assertNotNull(StoreManager.motivoDiscordNoDisponible("REPLACE_ME_PURCHASE_WEBHOOK", TEXTO));
    }

    @Test
    void unaUrlVaciaONulaNoEsUsable() {
        assertNotNull(StoreManager.motivoDiscordNoDisponible(null, TEXTO));
        assertNotNull(StoreManager.motivoDiscordNoDisponible("   ", TEXTO));
    }

    @Test
    void rechazaUrlsQueNoSonWebhooksDeDiscord() {
        assertNotNull(StoreManager.motivoDiscordNoDisponible("https://example.com/webhook", TEXTO));
        assertNotNull(StoreManager.motivoDiscordNoDisponible("http://discord.com/api/webhooks/1/x", TEXTO));
    }

    @Test
    void faltarElTextoTambienImpideElEnvio() {
        assertNotNull(StoreManager.motivoDiscordNoDisponible(VALIDO, ""));
        assertNotNull(StoreManager.motivoDiscordNoDisponible(VALIDO, null));
    }

    @Test
    void conWebhookYTextoValidosNoHayMotivo() {
        assertNull(StoreManager.motivoDiscordNoDisponible(VALIDO, TEXTO));
    }
}
