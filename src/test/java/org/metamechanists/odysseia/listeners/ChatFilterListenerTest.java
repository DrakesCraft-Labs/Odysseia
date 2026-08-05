package org.metamechanists.odysseia.listeners;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatFilterListenerTest {

    private static boolean censura(String mensaje, String palabra) {
        return ChatFilterListener.matches(ChatFilterListener.normalize(mensaje), palabra);
    }

    @Test
    void catchesTheWordOnItsOwnAndInsideASentence() {
        assertTrue(censura("mierda", "mierda"));
        assertTrue(censura("pero que MIERDA es esto", "mierda"));
        assertTrue(censura("eres un hijo de puta", "hijo de puta"));
        assertTrue(censura("¡mierda!", "mierda"));
    }

    @Test
    void doesNotCensorLegitimateWordsThatContainTheTerm() {
        // El filtro comparaba por subcadena: cualquier termino corto censuraba palabras validas.
        assertFalse(censura("el diputado hablo", "puta"));
        assertFalse(censura("mi hermano juega", "ano"));
        assertFalse(censura("tengo un gusano", "ano"));
        assertFalse(censura("esto es un plano", "ano"));
    }

    @Test
    void seesThroughLeetAndStretchedLetters() {
        assertTrue(censura("m1erd4", "mierda"));
        assertTrue(censura("mierdaaaa", "mierda"));
        assertTrue(censura("MiErDaaa", "mierda"));
        assertTrue(censura("hdp", "hdp"));
    }

    @Test
    void sanctionsAreConfigurableAndTheMuteLastsTenMinutes() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));
        assertEquals(3, config.getInt("chat-filter.warns.mute"));
        assertEquals(5, config.getInt("chat-filter.warns.kick"));
        assertEquals(7, config.getInt("chat-filter.warns.ban"));
        assertEquals("10m", config.getString("chat-filter.duracion.mute"));
        assertTrue(config.getBoolean("chat-filter.anuncio-global"),
                "sin anuncio, el silencio parece un fallo del servidor");
    }

    @Test
    void everyConfiguredWordStillMatchesItself() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml"));
        for (String word : config.getStringList("chat-filter.words")) {
            assertTrue(censura(word, word), "la lista contiene un termino que no se detecta a si mismo: " + word);
        }
    }
}
