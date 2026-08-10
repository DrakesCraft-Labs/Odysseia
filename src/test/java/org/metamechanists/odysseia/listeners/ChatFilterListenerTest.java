package org.metamechanists.odysseia.listeners;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

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

    /**
     * El fallo que se comio a ZurielWiz: colapsar tambien las letras dobles convertia "coon" en
     * "con", y a partir de ahi cualquiera que usara la preposicion se llevaba un warn.
     */
    @Test
    void laPreposicionConNoSeConfundeConElInsultoIngles() {
        assertFalse(ChatFilterListener.matches(
                ChatFilterListener.normalize("Rojo, con lo que ha pasado, que estes mejor"), "coon"));
        assertFalse(ChatFilterListener.matches(ChatFilterListener.normalize("voy con jack"), "coon"));
        assertFalse(ChatFilterListener.matches(ChatFilterListener.normalize("con"), "coon"));
    }

    @Test
    void elInsultoInglesSiSeSigueDetectando() {
        assertTrue(ChatFilterListener.matches(ChatFilterListener.normalize("you coon"), "coon"));
    }

    /** Otras dobles legitimas que el colapso agresivo tambien rompia. */
    @Test
    void lasDoblesLegitimasSeRespetan() {
        assertFalse(ChatFilterListener.matches(ChatFilterListener.normalize("una zora del bosque"), "zorra"));
        assertTrue(ChatFilterListener.matches(ChatFilterListener.normalize("eres una zorra"), "zorra"));
    }

    /** Los alargamientos de verdad se siguen pillando: quien evade no escribe dos letras. */
    @Test
    void losAlargamientosSiguenContando() {
        assertTrue(ChatFilterListener.matches(ChatFilterListener.normalize("mierdaaaa"), "mierda"));
        assertTrue(ChatFilterListener.matches(ChatFilterListener.normalize("mieeeerda"), "mierda"));
        assertTrue(ChatFilterListener.matches(ChatFilterListener.normalize("m1erd4"), "mierda"));
    }

    /**
     * Ninguna palabra de la lista puede normalizar a algo tan corto que choque con vocabulario
     * corriente. Es la clase de fallo que solo se ve cuando ya ha baneado a alguien.
     */
    @Test
    void ningunaPalabraDeLaListaAtrapaVocabularioCorriente() {
        List<String> lista = List.of("ctm", "culiao", "mierda", "maricon", "coon", "zorra",
                "cabron", "pendejo", "chucha", "puta madre", "fuck", "shit", "bitch", "retard");
        List<String> corrientes = List.of("con", "como", "cosa", "casa", "para", "pero", "todo",
                "cuando", "porque", "mejor", "suerte", "gracias", "hola", "juego", "cofre",
                "chico", "campo", "corre", "coro", "cono", "zona", "cabo", "pena");

        for (String palabra : lista) {
            for (String normal : corrientes) {
                assertFalse(ChatFilterListener.matches(ChatFilterListener.normalize(normal), palabra),
                        "'" + palabra + "' censura la palabra corriente '" + normal + "'");
            }
        }
    }
}
