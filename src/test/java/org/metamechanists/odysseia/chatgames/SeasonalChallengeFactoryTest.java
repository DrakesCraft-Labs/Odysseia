package org.metamechanists.odysseia.chatgames;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Calidad de los retos del chat.
 *
 * Lo que mas se vigila aqui no es que la logica funcione, sino que no vuelvan a colarse
 * preguntas cuya respuesta caduca sola. El menu de modalidades tenia escrito "las tres" y siguio
 * diciendolo con cinco cargadas hasta que un jugador lo reporto en el chat; una pregunta del tipo
 * "cuantas modalidades hay" envejece igual de mal y encima premia la respuesta equivocada.
 */
class SeasonalChallengeFactoryTest {

    private static final int VUELTAS = 400;

    private final SeasonalChallengeFactory factory = new SeasonalChallengeFactory();

    /** Recoge todo lo que la fabrica sabe generar para un modo. */
    private Set<ChatGameChallenge> generar(SeasonalGameMode modo) {
        Set<ChatGameChallenge> vistos = new HashSet<>();
        for (int i = 0; i < VUELTAS; i++) vistos.add(factory.create(modo));
        return vistos;
    }

    @Test
    void ningunRetoSaleVacioNiSinRespuesta() {
        for (SeasonalGameMode modo : SeasonalGameMode.values()) {
            for (ChatGameChallenge reto : generar(modo)) {
                assertNotNull(reto.prompt());
                assertFalse(reto.prompt().isBlank(), modo + ": hay un enunciado vacio");
                assertFalse(reto.answer().isBlank(), modo + ": hay un reto sin respuesta");
                assertFalse(reto.explanation().isBlank(), modo + ": hay un reto sin explicacion");
            }
        }
    }

    @Test
    void laRespuestaSiempreSeAceptaASiMisma() {
        for (SeasonalGameMode modo : SeasonalGameMode.values()) {
            for (ChatGameChallenge reto : generar(modo)) {
                assertTrue(reto.matches(reto.answer()),
                        modo + ": la respuesta correcta no se acepta -> " + reto.prompt());
                assertTrue(reto.matches("  " + reto.answer().toUpperCase(Locale.ROOT) + " "),
                        modo + ": no tolera mayusculas ni espacios -> " + reto.prompt());
            }
        }
    }

    @Test
    void elTriviaNoPreguntaPorCantidadesQueCambian() {
        for (ChatGameChallenge reto : generar(SeasonalGameMode.ORACLE_TRIVIA)) {
            String p = reto.prompt().toLowerCase(Locale.ROOT);
            boolean cuenta = p.startsWith("cuantos") || p.startsWith("cuantas");
            if (!cuenta) continue;
            // Un chunk mide 16 y siempre medira 16: contar cosas de Minecraft vale.
            // Contar cosas NUESTRAS --modalidades, addons, rangos-- no, porque crecen.
            for (String volatil : new String[]{"modalidad", "addon", "rango", "plugin", "mundo", "jefe"}) {
                assertFalse(p.contains(volatil),
                        "pregunta que caducara sola al crecer el servidor: " + reto.prompt());
            }
        }
    }

    @Test
    void elTriviaTieneVariedadSuficiente() {
        Set<String> enunciados = new HashSet<>();
        for (ChatGameChallenge reto : generar(SeasonalGameMode.ORACLE_TRIVIA)) enunciados.add(reto.prompt());
        assertTrue(enunciados.size() >= 10,
                "con pocas preguntas el juego se vuelve repetitivo; hay " + enunciados.size());
    }

    @Test
    void lasRunasNuncaSalenYaOrdenadas() {
        for (ChatGameChallenge reto : generar(SeasonalGameMode.RUNIC_SCRAMBLE)) {
            String mostrado = reto.prompt().replace("Ordena las runas: ", "");
            assertFalse(mostrado.equals(reto.answer()),
                    "la runa salio sin desordenar y se regala la respuesta: " + reto.answer());
        }
    }
}
