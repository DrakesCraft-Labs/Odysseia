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
}
