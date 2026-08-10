package org.metamechanists.odysseia.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El aviso de claves muertas.
 *
 * Una opcion que se mueve de sitio deja la vieja en el archivo desplegado sin dar ningun error.
 * El caso que motivo esto: el webhook de compras vive en purchase-engine.announcements.webhook-url,
 * pero en el config sigue store.announcement-webhook-url, pegada a store.api-key --que si se lee--,
 * asi que configurar la equivocada es el error natural.
 */
class ConfigLegacyKeysTest {

    @Test
    void unConfigLimpioNoGeneraAvisos() {
        assertTrue(ConfigLegacyKeys.avisos(Set.of("purchase-engine.announcements.webhook-url")).isEmpty());
    }

    @Test
    void laClaveViejaDelWebhookApuntaALaNueva() {
        List<String> avisos = ConfigLegacyKeys.avisos(Set.of("store.announcement-webhook-url"));
        assertEquals(1, avisos.size());
        assertTrue(avisos.get(0).contains("purchase-engine.announcements.webhook-url"), avisos.get(0));
    }

    @Test
    void lasQueYaNoTienenEquivalenteSeMandanBorrar() {
        List<String> avisos = ConfigLegacyKeys.avisos(Set.of("protectionstones.give-command"));
        assertEquals(1, avisos.size());
        assertTrue(avisos.get(0).contains("borrarla"), avisos.get(0));
    }

    @Test
    void seAvisaDeTodasLasQueHaya() {
        assertEquals(ConfigLegacyKeys.vigiladas().size(),
                ConfigLegacyKeys.avisos(ConfigLegacyKeys.vigiladas()).size());
    }

    /**
     * Las claves vigiladas tienen que estar muertas de verdad. Si alguna vuelve a usarse en el
     * codigo y nadie la quita de aqui, el aviso mandaria a la gente a cambiar algo que si funciona.
     */
    @Test
    void ningunaClaveVigiladaSeConfundeConLaViva() {
        for (String clave : ConfigLegacyKeys.vigiladas()) {
            assertEquals(1, ConfigLegacyKeys.avisos(Set.of(clave)).size(), clave);
        }
    }

    @Test
    void unConfigLimpioNoTieneNadaQueBorrar() {
        assertTrue(ConfigLegacyKeys.aBorrar(Set.of("purchase-engine.announcements.enabled")).isEmpty());
    }

    @Test
    void seBorranTodasLasMuertasQueHaya() {
        assertEquals(ConfigLegacyKeys.vigiladas().size(),
                ConfigLegacyKeys.aBorrar(ConfigLegacyKeys.vigiladas()).size());
    }

    /**
     * Lo que se avisa y lo que se borra tiene que ser exactamente lo mismo. Si se separaran,
     * quedarian claves de las que se avisa para siempre porque nadie las quita, que es justo el
     * problema que esto viene a resolver.
     */
    @Test
    void seBorraExactamenteLoQueSeAvisa() {
        Set<String> todas = ConfigLegacyKeys.vigiladas();
        assertEquals(ConfigLegacyKeys.avisos(todas).size(), ConfigLegacyKeys.aBorrar(todas).size());

        for (String clave : todas) {
            assertEquals(List.of(clave), ConfigLegacyKeys.aBorrar(Set.of(clave)), clave);
        }
    }

    /**
     * Borrar no puede llevarse por delante una clave viva: 'store.api-key' vive pegada a las
     * muertas y sin ella la tienda deja de entregar compras.
     */
    @Test
    void noSeBorranClavesVivas() {
        Set<String> config = Set.of("store.api-key", "store.api-url",
                "store.poll-interval-seconds", "store.enabled");

        assertEquals(List.of("store.enabled"), ConfigLegacyKeys.aBorrar(config));
    }
}
