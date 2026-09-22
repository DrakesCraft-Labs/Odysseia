package org.metamechanists.odysseia.papa;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.metamechanists.odysseia.cosmetics.Cosmetic;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Los premios del trueque tienen que existir de verdad.
 *
 * Ya paso dos veces y las dos el jugador habria pagado sin recibir nada: los kits de oficio no
 * existian, y el cosmetico daba un permiso que sin {@code drakes.cosmetics.use} no sirve para
 * nada. Un comando de recompensa que falla no lanza ningun error: simplemente no pasa nada.
 *
 * Esta prueba comprueba lo que se puede comprobar sin servidor: que los kits nombrados existan en
 * config.yml, que los equipos de {@code /papaequipo} esten implementados, y que los ids de
 * Slimefun tengan forma de id.
 */
class PapaTraderRewardsTest {

    private static final Path TRUEQUE = Path.of("src/main/resources/papa-trader.yml");
    private static final Path CONFIG = Path.of("src/main/resources/config.yml");
    private static final Path EQUIPO =
            Path.of("src/main/java/org/metamechanists/odysseia/papa/PapaEquipoCommand.java");

    private static List<String> comandos() {
        YamlConfiguration datos = YamlConfiguration.loadConfiguration(new File(TRUEQUE.toString()));
        var seccion = datos.getConfigurationSection("niveles");
        assertTrue(seccion != null, "papa-trader.yml no tiene niveles");
        List<String> salida = new ArrayList<>();
        for (String id : seccion.getKeys(false)) {
            salida.addAll(seccion.getStringList(id + ".comandos"));
        }
        return salida;
    }

    @Test
    void todoKitPremiadoExisteEnLaConfiguracion() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(CONFIG.toString()));
        var kits = config.getConfigurationSection("kits");
        assertTrue(kits != null, "config.yml no tiene kits");
        Set<String> existentes = kits.getKeys(false);

        List<String> rotos = new ArrayList<>();
        for (String comando : comandos()) {
            Matcher m = Pattern.compile("^kitgive \\{jugador} (\\S+)").matcher(comando.trim());
            if (m.find() && !existentes.contains(m.group(1))) rotos.add(m.group(1));
        }
        assertEquals(List.of(), rotos, "el trueque promete kits que no existen; el jugador paga y no recibe nada");
    }

    @Test
    void todoEquipoPremiadoEstaImplementado() throws IOException {
        String fuente = Files.readString(EQUIPO, StandardCharsets.UTF_8);
        List<String> rotos = new ArrayList<>();
        for (String comando : comandos()) {
            Matcher m = Pattern.compile("^papaequipo \\{jugador} (\\S+)").matcher(comando.trim());
            if (m.find() && !fuente.contains("case \"" + m.group(1) + "\"")) rotos.add(m.group(1));
        }
        assertEquals(List.of(), rotos, "hay equipos premiados sin implementar en PapaEquipoCommand");
    }

    @Test
    void losIdsDeSlimefunTienenFormaDeId() {
        List<String> raros = new ArrayList<>();
        for (String comando : comandos()) {
            Matcher m = Pattern.compile("^papasf \\{jugador} (\\S+)").matcher(comando.trim());
            // Los ids de Slimefun son MAYUSCULAS_CON_GUION_BAJO. Un id en minusculas no resuelve.
            if (m.find() && !m.group(1).matches("[A-Z0-9_]+")) raros.add(m.group(1));
        }
        assertEquals(List.of(), raros, "un id de Slimefun mal escrito no entrega nada");
    }

    @Test
    void ningunNivelSeQuedaSinPremio() {
        YamlConfiguration datos = YamlConfiguration.loadConfiguration(new File(TRUEQUE.toString()));
        var seccion = datos.getConfigurationSection("niveles");
        List<String> vacios = new ArrayList<>();
        for (String id : seccion.getKeys(false)) {
            if (seccion.getStringList(id + ".comandos").isEmpty()) vacios.add(id);
            if (seccion.getInt(id + ".coste", 0) <= 0) vacios.add(id + " (sin coste)");
        }
        assertEquals(List.of(), vacios, "estos niveles cobrarian sin entregar nada");
    }

    @Test
    void elCosmeticoDaTambienElPermisoBase() {
        // Sin drakes.cosmetics.use, el menu rechaza al jugador y el rastro no se dibuja: pagaria
        // por algo que no puede usar. Fue un fallo real el 2026-08-06.
        List<String> comandos = comandos();
        boolean daRastro = comandos.stream().anyMatch(c -> c.contains("drakes.cosmetics.trail."));
        if (daRastro) {
            assertTrue(comandos.stream().anyMatch(c -> c.contains("drakes.cosmetics.use")),
                    "se regala un rastro sin el permiso base que lo hace funcionar");
        }
    }

    @Test
    void hermesCuestaCuarentaYOchoStacksYAcumulaDuracion() {
        YamlConfiguration datos = YamlConfiguration.loadConfiguration(new File(TRUEQUE.toString()));
        assertEquals(3072, datos.getInt("niveles.hermes_temporal.coste"));
        assertTrue(datos.getStringList("niveles.hermes_temporal.comandos")
                .contains("lp user {jugador} parent addtemp hermes 5d accumulate"),
                "cada canje debe sumar cinco dias en lugar de reemplazar el tiempo existente");
    }

    @Test
    void todosLosNuevosPremiosTienenCosteYEntrega() {
        YamlConfiguration datos = YamlConfiguration.loadConfiguration(new File(TRUEQUE.toString()));
        var niveles = datos.getConfigurationSection("niveles");
        assertTrue(niveles != null && niveles.getKeys(false).size() <= 45,
                "el menu actual solo dispone de 45 espacios de premios; necesita paginacion si crece mas");
        for (String id : List.of("tesoro_de_marea", "provisiones_explorador", "tesoro_abisal",
                "rastro_plumas", "alas_doradas")) {
            assertTrue(datos.getInt("niveles." + id + ".coste", 0) > 0,
                    "coste invalido o ausente: " + id);
            assertTrue(!datos.getStringList("niveles." + id + ".comandos").isEmpty(),
                    "premio sin entrega configurada: " + id);
        }
    }

    @Test
    void materialesVanillaYCosmeticosNuevosExisten() {
        Pattern give = Pattern.compile("^minecraft:give \\{jugador} minecraft:([a-z0-9_]+) \\d+$");
        List<String> materialesInvalidos = new ArrayList<>();
        for (String comando : comandos()) {
            Matcher matcher = give.matcher(comando.trim());
            if (matcher.matches() && Material.matchMaterial(matcher.group(1).toUpperCase(Locale.ROOT)) == null) {
                materialesInvalidos.add(matcher.group(1));
            }
        }
        assertEquals(List.of(), materialesInvalidos, "el trueque referencia materiales vanilla desconocidos");
        assertTrue(Cosmetic.of("aura").stream().anyMatch(c -> c.id().equals("alas")),
                "el premio debe usar un aura que exista en el catalogo de cosmeticos");
        assertTrue(Cosmetic.of("rastro").stream().anyMatch(c -> c.id().equals("plumas")),
                "el premio debe usar un rastro que exista en el catalogo de cosmeticos");
    }
}
