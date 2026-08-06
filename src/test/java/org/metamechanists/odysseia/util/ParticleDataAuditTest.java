package org.metamechanists.odysseia.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Ninguna particula con dato obligatorio puede llamarse sin el.
 *
 * En Paper 1.21.11 estas particulas exigen un argumento extra y sin el lanzan
 * IllegalArgumentException, que mata la tarea entera donde se ejecuta. Ya paso tres veces:
 * DRAGON_BREATH en la aura de dragon, FLASH en los altares de FNAmplifications y FLASH en la
 * llamarada solar de los jefes. El error solo aparece cuando un jugador dispara ese efecto, asi
 * que nunca lo caza una prueba de arranque.
 */
class ParticleDataAuditTest {

    /** Particula -> tipo de dato que exige. */
    private static final Map<String, String> EXIGEN_DATO = Map.of(
            "DUST", "DustOptions",
            "DUST_COLOR_TRANSITION", "DustTransition",
            "BLOCK", "BlockData",
            "BLOCK_MARKER", "BlockData",
            "FALLING_DUST", "BlockData",
            "ITEM", "ItemStack",
            "ENTITY_EFFECT", "Color",
            "SCULK_CHARGE", "Float",
            "SHRIEK", "Integer",
            "FLASH", "Color");

    private static final Pattern LLAMADA = Pattern.compile("spawnParticle\\s*\\(\\s*Particle\\.([A-Z_]+)");

    /** Un ultimo argumento que construye o nombra un dato valido. */
    private static final Pattern ES_DATO = Pattern.compile(
            "new\\s+(Particle\\.)?(DustOptions|DustTransition)|createBlockData|getBlockData"
                    + "|Color\\.|fromRGB|ItemStack|\\d+\\.?\\d*[fF]\\b"
                    + "|\\b(data|dato|options|dust|polvo|blockData|particleData|color|velocity)\\b",
            Pattern.CASE_INSENSITIVE);

    @Test
    void everyParticleWithRequiredDataReceivesIt() throws IOException {
        List<String> rotas = new ArrayList<>();
        Path fuentes = Paths.get("src/main/java");

        try (Stream<Path> ficheros = Files.walk(fuentes)) {
            for (Path fichero : ficheros.filter(p -> p.toString().endsWith(".java")).toList()) {
                String texto = Files.readString(fichero, StandardCharsets.UTF_8);
                Matcher matcher = LLAMADA.matcher(texto);
                while (matcher.find()) {
                    String particula = matcher.group(1);
                    if (!EXIGEN_DATO.containsKey(particula)) continue;

                    String ultimo = ultimoArgumento(texto, texto.indexOf('(', matcher.start()));
                    if (ultimo == null || ES_DATO.matcher(ultimo).find()) continue;

                    int linea = (int) texto.substring(0, matcher.start()).chars().filter(c -> c == '\n').count() + 1;
                    rotas.add(fichero.getFileName() + ":" + linea + " " + particula
                            + " necesita " + EXIGEN_DATO.get(particula) + ", recibe '" + ultimo + "'");
                }
            }
        }

        assertEquals(List.of(), rotas,
                "hay particulas sin su dato obligatorio; revientan al ejecutarse, no al arrancar");
    }

    /** Devuelve el ultimo argumento de nivel superior de la llamada, o null si no se puede leer. */
    private static String ultimoArgumento(String texto, int abre) {
        if (abre < 0) return null;
        int profundidad = 0;
        StringBuilder actual = new StringBuilder();
        String ultimo = null;
        for (int i = abre; i < Math.min(texto.length(), abre + 4000); i++) {
            char c = texto.charAt(i);
            if (c == '(' || c == '[' || c == '{') {
                profundidad++;
                if (profundidad == 1) continue;
            } else if (c == ')' || c == ']' || c == '}') {
                profundidad--;
                if (profundidad == 0) return actual.toString().trim();
            } else if (c == ',' && profundidad == 1) {
                ultimo = actual.toString().trim();
                actual.setLength(0);
                continue;
            }
            if (profundidad >= 1) actual.append(c);
        }
        return ultimo;
    }
}
