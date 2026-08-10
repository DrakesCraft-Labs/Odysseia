package org.metamechanists.odysseia.papa;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
}
