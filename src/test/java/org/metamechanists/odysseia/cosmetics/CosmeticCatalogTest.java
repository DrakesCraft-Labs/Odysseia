package org.metamechanists.odysseia.cosmetics;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El catalogo de cosmeticos y lo que se dibuja de verdad tienen que coincidir.
 *
 * Son dos listas separadas: {@link Cosmetic} decide lo que sale en {@code /cosmeticos} y en el
 * permiso, y el switch de {@link CosmeticService} decide lo que se pinta. Si se anade uno al
 * catalogo y se olvida el switch, el jugador lo equipa, paga el rango y **no ve nada** --sin ningun
 * error en consola--. Al reves es menos grave pero deja codigo muerto.
 *
 * Es el mismo tipo de fallo que el cosmetico de la papa: existia el permiso pero no servia.
 */
class CosmeticCatalogTest {

    private static final Path SERVICIO =
            Path.of("src/main/java/org/metamechanists/odysseia/cosmetics/CosmeticService.java");

    /** Los 'case "x"' que hay entre dos marcas del archivo. */
    private static Set<String> casosEntre(String texto, String desde, String hasta) {
        int a = texto.indexOf(desde);
        int b = texto.indexOf(hasta);
        assertTrue(a >= 0 && b > a, "no se encontro el bloque " + desde);
        Set<String> casos = new HashSet<>();
        Matcher m = Pattern.compile("case \"([a-z_]+)\"").matcher(texto.substring(a, b));
        while (m.find()) casos.add(m.group(1));
        return casos;
    }

    private static Set<String> ids(List<Cosmetic> lista) {
        Set<String> salida = new HashSet<>();
        for (Cosmetic c : lista) salida.add(c.id());
        return salida;
    }

    @Test
    void todaAuraDelCatalogoSeDibuja() throws IOException {
        String texto = Files.readString(SERVICIO, StandardCharsets.UTF_8);
        Set<String> dibujadas = casosEntre(texto, "private void tickAuras", "public void onMove");
        Set<String> catalogo = ids(Cosmetic.AURAS);
        catalogo.removeAll(dibujadas);
        assertEquals(Set.of(), catalogo,
                "estas auras se pueden equipar pero no pintan nada");
    }

    @Test
    void todoRastroDelCatalogoSeDibuja() throws IOException {
        String texto = Files.readString(SERVICIO, StandardCharsets.UTF_8);
        Set<String> dibujados = casosEntre(texto, "public void onMove", "private static double yaw");
        Set<String> catalogo = ids(Cosmetic.RASTROS);
        catalogo.removeAll(dibujados);
        assertEquals(Set.of(), catalogo,
                "estos rastros se pueden equipar pero no pintan nada");
    }

    @Test
    void todoEfectoDeMuerteDelCatalogoSeDibuja() throws IOException {
        String texto = Files.readString(SERVICIO, StandardCharsets.UTF_8);
        Set<String> dibujados = casosEntre(texto, "public void onDeath", "private void save");
        Set<String> catalogo = ids(Cosmetic.MUERTES);
        catalogo.removeAll(dibujados);
        assertEquals(Set.of(), catalogo,
                "estos efectos de muerte se pueden equipar pero no pintan nada");
    }

    @Test
    void noHayIdsRepetidosDentroDeUnTipo() {
        for (List<Cosmetic> lista : List.of(Cosmetic.AURAS, Cosmetic.RASTROS, Cosmetic.MUERTES)) {
            assertEquals(lista.size(), ids(lista).size(),
                    "hay ids repetidos: el segundo seria inalcanzable");
        }
    }

    @Test
    void todoCosmeticoTieneNombreVisible() {
        for (List<Cosmetic> lista : List.of(Cosmetic.AURAS, Cosmetic.RASTROS, Cosmetic.MUERTES)) {
            for (Cosmetic c : lista) {
                assertTrue(c.nombre() != null && !c.nombre().isBlank(),
                        "el cosmetico '" + c.id() + "' saldria sin nombre en el menu");
            }
        }
    }
}
