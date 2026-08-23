package org.metamechanists.odysseia.listeners;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La lista blanca del laboratorio, comprobada contra el config.yml que se publica de verdad.
 *
 * El valor de estas pruebas no esta en la logica --que es la misma de siempre-- sino en el dato:
 * si alguien anade manana un comando a la lista blanca del laboratorio sin pensar en que abre un
 * puente hacia la economia, esto lo para antes de que llegue al servidor.
 */
class SandboxWhitelistTest {

    private static List<List<String>> whitelist() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.load(new File("src/main/resources/config.yml"));
        List<List<String>> patterns = new ArrayList<>();
        for (String value : config.getStringList("modalidades.guard.comandos-lista-blanca.laboratorio")) {
            patterns.add(ModalityStorageGuardListener.tokens(value.toLowerCase(Locale.ROOT)));
        }
        return patterns;
    }

    private static boolean permitido(String command) throws Exception {
        return ModalityStorageGuardListener.matchesAllowlist(whitelist(), ModalityStorageGuardListener.tokens(command));
    }

    @Test
    void slimefunYSusSubcomandosFuncionanDentro() throws Exception {
        assertTrue(permitido("sf"));
        assertTrue(permitido("sf cheat"));
        assertTrue(permitido("sf guide"));
        assertTrue(permitido("slimefun:sf cheat"), "la forma plugin:comando no debe saltarse la lista blanca");
    }

    @Test
    void slimefunAdministrativoNuncaQuedaAutorizadoPorElPrefijo() throws Exception {
        for (String command : List.of(
                "sf give Jack DIAMOND", "sf research Jack all", "sf timings", "sf teleporter",
                "sf backpack Jack", "sf charge Jack 1000", "sf debug", "sf repair",
                "sf native", "slimefun:sf give Jack DIAMOND", "slimefun research Jack all")) {
            assertFalse(permitido(command), "el laboratorio no debe autorizar /" + command);
        }
    }

    @Test
    void sePuedeSalirDelLaboratorio() throws Exception {
        assertTrue(permitido("spawn"));
        assertTrue(permitido("modalidades"));
        assertTrue(permitido("survival"));
    }

    @Test
    void laEconomiaNoEntra() throws Exception {
        // Ni el comando ni ninguno de sus alias conocidos.
        for (String command : List.of(
                "sellall", "sell", "vender", "shop", "tienda", "ah", "subasta", "auction",
                "pay", "pagar", "bal", "balance", "money", "dinero", "baltop", "eco",
                "sfmercado", "qs", "quickshop", "buy", "comprar", "trade", "market")) {
            assertFalse(permitido(command), "no debe permitirse /" + command + " en el laboratorio");
            assertFalse(permitido("essentials:" + command),
                    "no debe permitirse /essentials:" + command + " en el laboratorio");
        }
    }

    @Test
    void noSePuedeSacarNadaPorAlmacenamientoNiHogares() throws Exception {
        for (String command : List.of(
                "home", "homes", "sethome", "ec", "enderchest", "pv", "vault", "backpack",
                "mochila", "kit", "kits", "back", "warp", "tpahere", "team echest", "ps get")) {
            assertFalse(permitido(command), "no debe permitirse /" + command + " en el laboratorio");
        }
    }

    @Test
    void losRangosNoSeTocanDesdeDentro() throws Exception {
        for (String command : List.of("lp", "luckperms", "rango", "rank", "vip", "perm", "permissions")) {
            assertFalse(permitido(command), "no debe permitirse /" + command + " en el laboratorio");
        }
    }

    @Test
    void elLaboratorioEstaDeclaradoComoAisladoYDeListaBlanca() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.load(new File("src/main/resources/config.yml"));
        assertTrue(config.getStringList("modalidades.guard.modalidades-aisladas").contains("laboratorio"));
        assertTrue(config.getStringList("modalidades.guard.modalidades-lista-blanca").contains("laboratorio"));
        assertFalse(config.getStringList("modalidades.modos.laboratorio.mundos").isEmpty(),
                "sin mundos declarados el aislamiento no se activa");
    }
}
