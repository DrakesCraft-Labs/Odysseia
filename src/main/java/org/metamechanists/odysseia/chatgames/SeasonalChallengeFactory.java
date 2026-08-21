package org.metamechanists.odysseia.chatgames;

import java.util.List;
import java.util.Random;

/** Generates five genuinely different weekly formats rather than repainting one trivia loop. */
final class SeasonalChallengeFactory {
    private static final List<String> RUNES = List.of("SLIMEFUN", "ODYSSEIA", "DRAGMAS", "NETHERITE", "CONVERGENCIA", "AUTOMATIZACION",
            "BOSSWARP", "PROTECCION", "MODALIDADES", "SKYBLOCK", "ONEBLOCK", "CLASICO", "LABORATORIO",
            "CARTOGRAFO", "TERRALITH", "QUANTUM", "CARGO", "INFINITY", "MULTIVERSO", "DRAKESCRAFT");
    /*
     * Preguntas del servidor.
     *
     * Ninguna pregunta CUANTAS modalidades hay, ni cuantos addons, ni nada que se cuente. Ese
     * tipo de pregunta caduca sola: el menu de modalidades tenia escrito "las tres" y siguio
     * diciendolo con cinco cargadas, hasta que un jugador lo reporto. Lo que se pregunta aqui son
     * comandos y reglas, que cambian cuando alguien decide cambiarlos y no a espaldas de nadie.
     */
    private static final List<String[]> TRIVIA = List.of(
            new String[]{"Que comando abre el menu principal del servidor?", "/menu", "El menu principal se abre con /menu."},
            new String[]{"Que comando abre la guia de Slimefun?", "/sf guide", "La guia se abre con /sf guide."},
            new String[]{"Que moneda usa DrakesCraft?", "dragmas", "La moneda del servidor se llama Dragmas."},
            new String[]{"Que dios gobierna los mares en el Olimpo?", "poseidon", "Poseidon es el patron de mares y terremotos."},
            new String[]{"Cuantos bloques mide un chunk por lado?", "16", "Un chunk mide 16 por 16 bloques."},
            new String[]{"Que comando abre el menu de modalidades?", "/modalidades", "Con /modalidades saltas entre las modalidades del servidor."},
            new String[]{"Que comando te devuelve al lobby?", "/lobby", "El /lobby te lleva al hub para elegir modalidad."},
            new String[]{"En que modalidad NO funciona Slimefun?", "clasico", "Clasico es vanilla a proposito: sin maquinas ni mobs personalizados."},
            new String[]{"Que comando abre tu isla de SkyBlock?", "/is", "La isla de SkyBlock se abre con /is. La de OneBlock es /ob."},
            new String[]{"Que comando te lleva lejos a empezar una base?", "/rtp", "El /rtp te suelta lejos del spawn para que construyas tranquilo."},
            new String[]{"Que comando abre tus bovedas personales?", "/pv", "Las bovedas van por modalidad: lo que guardas en una no aparece en otra."},
            new String[]{"Que se conserva al cambiar de modalidad: el rango o el inventario?", "el rango", "El rango, el dinero y los kits te siguen; el inventario y las bovedas no."},
            new String[]{"Que comando abre la subasta entre jugadores?", "/ah", "En /ah pones tus cosas al precio que quieras."},
            new String[]{"Como se llama el plugin de maquinas del servidor?", "slimefun", "Slimefun es la base tecnica de la modalidad principal."}
    );
    private static final List<String[]> REFLEXES = List.of(
            new String[]{"RAYO DORADO", "RAYO DORADO"}, new String[]{"CARGO LISTO", "CARGO LISTO"},
            new String[]{"OLIMPO DESPIERTA", "OLIMPO DESPIERTA"}, new String[]{"20 TPS", "20 TPS"}
    );
    private final Random random = new Random();

    ChatGameChallenge create(SeasonalGameMode mode) {
        return switch (mode) {
            case FORGE_MATH -> forge();
            case RUNIC_SCRAMBLE -> runes();
            case ORACLE_TRIVIA -> trivia();
            case HERMES_REFLEX -> reflex();
            case CARTOGRAPHER_CODE -> cartographer();
        };
    }

    private ChatGameChallenge forge() {
        int left = 12 + random.nextInt(69);
        int right = 7 + random.nextInt(42);
        int multiplier = 2 + random.nextInt(7);
        int answer = (left + right) * multiplier;
        return new ChatGameChallenge(SeasonalGameMode.FORGE_MATH, "(" + left + " + " + right + ") x " + multiplier + " = ?", String.valueOf(answer),
                "(" + left + " + " + right + ") x " + multiplier + " = " + answer + ".");
    }

    private ChatGameChallenge runes() {
        String word = RUNES.get(random.nextInt(RUNES.size()));
        char[] letters = word.toCharArray();
        for (int index = letters.length - 1; index > 0; index--) {
            int swap = random.nextInt(index + 1);
            char value = letters[index]; letters[index] = letters[swap]; letters[swap] = value;
        }
        String scrambled = new String(letters);
        if (scrambled.equals(word) && word.length() > 1) scrambled = word.substring(1) + word.charAt(0);
        return new ChatGameChallenge(SeasonalGameMode.RUNIC_SCRAMBLE, "Ordena las runas: " + scrambled, word,
                "Las runas formaban " + word + ".");
    }

    private ChatGameChallenge trivia() {
        String[] question = TRIVIA.get(random.nextInt(TRIVIA.size()));
        return new ChatGameChallenge(SeasonalGameMode.ORACLE_TRIVIA, question[0], question[1], question[2]);
    }

    private ChatGameChallenge reflex() {
        String[] challenge = REFLEXES.get(random.nextInt(REFLEXES.size()));
        return new ChatGameChallenge(SeasonalGameMode.HERMES_REFLEX, "Escribe exactamente: " + challenge[0], challenge[1],
                "La frase exacta era " + challenge[1] + ".");
    }

    private ChatGameChallenge cartographer() {
        int x = 2 + random.nextInt(7);
        int z = 2 + random.nextInt(7);
        int scale = 16;
        int answer = x * scale + z;
        return new ChatGameChallenge(SeasonalGameMode.CARTOGRAPHER_CODE,
                "Un mapa usa X=" + x + " chunks y Z=" + z + " bloques. Si cada chunk mide " + scale + ", responde Xx" + scale + "+Z.",
                String.valueOf(answer), "La clave es " + x + " x " + scale + " + " + z + " = " + answer + ".");
    }
}
