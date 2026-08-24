package org.metamechanists.odysseia.listeners;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModalityStorageGuardListenerTest {

    private static final List<List<String>> BLOCKED = List.of(
            ModalityStorageGuardListener.tokens("ah"),
            ModalityStorageGuardListener.tokens("crazyauctions"),
            ModalityStorageGuardListener.tokens("team echest"));

    @Test
    void extractsCommandLabelsIncludingNamespacedForms() {
        assertEquals("pv", ModalityStorageGuardListener.label("pv"));
        assertEquals("pv", ModalityStorageGuardListener.label("PV"));
        assertEquals("pv", ModalityStorageGuardListener.label("playervaultz:pv"));
        assertEquals("vault", ModalityStorageGuardListener.label("PlayerVaultZ:Vault"));
    }

    @Test
    void tokenizesNamespaceOnlyInTheFirstToken() {
        assertEquals(List.of("team", "echest"), ModalityStorageGuardListener.tokens("BetterTeams:Team ECHEST"));
        assertEquals(List.of("ah"), ModalityStorageGuardListener.tokens("  ah  "));
    }

    @Test
    void blocksRootCommandsWithAndWithoutArguments() {
        assertTrue(ModalityStorageGuardListener.matches(BLOCKED, ModalityStorageGuardListener.tokens("ah")));
        assertTrue(ModalityStorageGuardListener.matches(BLOCKED, ModalityStorageGuardListener.tokens("ah sell 100")));
        assertTrue(ModalityStorageGuardListener.matches(BLOCKED, ModalityStorageGuardListener.tokens("crazyauctions:ah")));
        assertTrue(ModalityStorageGuardListener.matches(BLOCKED, ModalityStorageGuardListener.tokens("CrazyAuctions")));
    }

    @Test
    void blocksOnlyTheStorageSubcommandOfTeam() {
        assertTrue(ModalityStorageGuardListener.matches(BLOCKED, ModalityStorageGuardListener.tokens("team echest")));
        assertTrue(ModalityStorageGuardListener.matches(BLOCKED, ModalityStorageGuardListener.tokens("team ECHEST extra")));
        assertFalse(ModalityStorageGuardListener.matches(BLOCKED, ModalityStorageGuardListener.tokens("team")));
        assertFalse(ModalityStorageGuardListener.matches(BLOCKED, ModalityStorageGuardListener.tokens("team chat hola")));
    }

    @Test
    void doesNotBlockUnrelatedCommandsThatSharePrefix() {
        assertFalse(ModalityStorageGuardListener.matches(BLOCKED, ModalityStorageGuardListener.tokens("ahorcado")));
        assertFalse(ModalityStorageGuardListener.matches(BLOCKED, ModalityStorageGuardListener.tokens("spawn")));
    }

    @Test
    void isolatesEveryModeWithASeparateInventory() {
        Set<String> isolated = Set.of("skyblock", "oneblock", "clasico");

        assertTrue(ModalityStorageGuardListener.isIsolatedModality(isolated, "skyblock"));
        assertTrue(ModalityStorageGuardListener.isIsolatedModality(isolated, "ONEBLOCK"));
        assertTrue(ModalityStorageGuardListener.isIsolatedModality(isolated, "clasico"));
        assertFalse(ModalityStorageGuardListener.isIsolatedModality(isolated, "survival"));
    }

    @Test
    void classicAllowsIsolatedAuctionsAndKeepsProtectionShop() {
        var config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.File("src/main/resources/config.yml"));
        List<List<String>> blocked = new java.util.ArrayList<>();
        for (String value : config.getStringList("modalidades.guard.comandos-bloqueados")) {
            blocked.add(ModalityStorageGuardListener.tokens(value));
        }
        List<List<String>> allowed = new java.util.ArrayList<>();
        for (String value : config.getStringList("modalidades.guard.comandos-permitidos.clasico")) {
            allowed.add(ModalityStorageGuardListener.tokens(value));
        }

        assertFalse(ModalityStorageGuardListener.matches(blocked, ModalityStorageGuardListener.tokens("ah")));
        assertFalse(ModalityStorageGuardListener.matches(allowed, ModalityStorageGuardListener.tokens("ah")));
        assertTrue(ModalityStorageGuardListener.matches(blocked, ModalityStorageGuardListener.tokens("ps get pnyx")));
        assertTrue(ModalityStorageGuardListener.matches(allowed, ModalityStorageGuardListener.tokens("ps get pnyx")));
        assertTrue(ModalityStorageGuardListener.matches(allowed, ModalityStorageGuardListener.tokens("tiendaprot")));
    }

    @Test
    void slimefunMarketExistsOnlyInTechnologicalModalities() {
        var config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.File("src/main/resources/config.yml"));

        assertTrue(config.getStringList("modalidades.guard.comandos-bloqueados").contains("sfmercado"));
        assertTrue(config.getStringList("modalidades.guard.comandos-permitidos.oneblock").contains("sfmercado"));
        assertTrue(config.getStringList("modalidades.guard.comandos-permitidos.skyblock").contains("sfmercado"));
        assertFalse(config.getStringList("modalidades.guard.comandos-permitidos.clasico").contains("sfmercado"));
        assertFalse(config.getStringList("modalidades.guard.comandos-lista-blanca.laboratorio").contains("sfmercado"));
    }

    @Test
    void classicGameplayRestrictionsCoverShortcutsButKeepHomesProtectionsAndRtp() {
        var config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.File("src/main/resources/config.yml"));
        List<List<String>> classic = new java.util.ArrayList<>();
        for (String value : config.getStringList("modalidades.guard.comandos-restringidos.clasico")) {
            classic.add(ModalityStorageGuardListener.tokens(value));
        }

        assertTrue(ModalityStorageGuardListener.matches(classic, ModalityStorageGuardListener.tokens("sf guide")));
        assertTrue(ModalityStorageGuardListener.matches(classic, ModalityStorageGuardListener.tokens("essentials:warp spawn")));
        assertTrue(ModalityStorageGuardListener.matches(classic, ModalityStorageGuardListener.tokens("kit hermes")));
        assertFalse(ModalityStorageGuardListener.matches(classic, ModalityStorageGuardListener.tokens("home base")));
        assertFalse(ModalityStorageGuardListener.matches(classic, ModalityStorageGuardListener.tokens("homes")));
        assertFalse(ModalityStorageGuardListener.matches(classic, ModalityStorageGuardListener.tokens("sethome base")));
        assertFalse(ModalityStorageGuardListener.matches(classic, ModalityStorageGuardListener.tokens("delhome base")));
        assertFalse(ModalityStorageGuardListener.matches(classic, ModalityStorageGuardListener.tokens("ps get pnyx")));
        assertFalse(ModalityStorageGuardListener.matches(classic, ModalityStorageGuardListener.tokens("ps home")));
        assertFalse(ModalityStorageGuardListener.matches(classic, ModalityStorageGuardListener.tokens("tpa jugador")));
        assertFalse(ModalityStorageGuardListener.matches(classic, ModalityStorageGuardListener.tokens("tpaccept")));
        assertFalse(ModalityStorageGuardListener.matches(classic, ModalityStorageGuardListener.tokens("tpyes")));
        assertFalse(ModalityStorageGuardListener.matches(classic, ModalityStorageGuardListener.tokens("tpdeny")));
        assertTrue(ModalityStorageGuardListener.matches(classic, ModalityStorageGuardListener.tokens("tpahere jugador")));
        assertFalse(ModalityStorageGuardListener.matches(classic, ModalityStorageGuardListener.tokens("rtp")));
    }

    @Test
    void blocksBuyingProtectionInsideAnIslandButKeepsTheRestOfPs() {
        // Un jugador compro una piedra dentro de su isla de OneBlock y no la pudo colocar:
        // el world_list_type de cada .toml solo admite los mundos del Survival.
        List<List<String>> reales = new java.util.ArrayList<>();
        var config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.File("src/main/resources/config.yml"));
        for (String value : config.getStringList("modalidades.guard.comandos-bloqueados")) {
            reales.add(ModalityStorageGuardListener.tokens(value));
        }

        assertTrue(ModalityStorageGuardListener.matches(reales, ModalityStorageGuardListener.tokens("ps get pnyx")));
        assertTrue(ModalityStorageGuardListener.matches(reales, ModalityStorageGuardListener.tokens("tiendaprot")));
        assertFalse(ModalityStorageGuardListener.matches(reales, ModalityStorageGuardListener.tokens("ps home")),
                "el resto de /ps tiene que seguir funcionando dentro de la isla");
        assertFalse(ModalityStorageGuardListener.matches(reales, ModalityStorageGuardListener.tokens("ps info")));
    }

    @Test
    void everyBlockedPatternWithItsOwnMessageIsActuallyBlocked() {
        var config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.File("src/main/resources/config.yml"));
        List<List<String>> reales = new java.util.ArrayList<>();
        for (String value : config.getStringList("modalidades.guard.comandos-bloqueados")) {
            reales.add(ModalityStorageGuardListener.tokens(value));
        }
        var mensajes = config.getConfigurationSection("modalidades.guard.mensajes");
        for (String key : mensajes.getKeys(false)) {
            List<String> tokens = ModalityStorageGuardListener.tokens(key.replace('-', ' '));
            assertTrue(ModalityStorageGuardListener.matches(reales, tokens),
                    "hay un mensaje para '" + key + "' pero ese comando no esta bloqueado");
        }
    }

    @Test
    void nativeAuctionButtonTargetsACommandAllowedInEveryModality() {
        var config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new java.io.File("src/main/resources/config.yml"));
        List<List<String>> blocked = new java.util.ArrayList<>();
        for (String value : config.getStringList("modalidades.guard.comandos-bloqueados")) {
            blocked.add(ModalityStorageGuardListener.tokens(value));
        }

        List<String> auctionActions = config.getStringList("native-menus.shop.entries.subastas.commands");
        assertFalse(auctionActions.isEmpty(), "el boton de subastas debe tener una accion");
        assertTrue(auctionActions.stream().noneMatch(action -> {
                    String target = action.startsWith("odysseia:survival-command:")
                            ? action.substring("odysseia:survival-command:".length()) : action;
                    return ModalityStorageGuardListener.matches(blocked, ModalityStorageGuardListener.tokens(target));
                }),
                "el boton de subastas no debe quedar bloqueado por el guard de modalidades");
    }
}
