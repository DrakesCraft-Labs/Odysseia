package org.metamechanists.odysseia.cosmetics;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CosmeticTest {

    @Test
    void idsAreUniqueWithinEachType() {
        for (String tipo : List.of("aura", "rastro", "muerte")) {
            Set<String> vistos = new HashSet<>();
            for (Cosmetic cosmetic : Cosmetic.of(tipo)) {
                assertTrue(vistos.add(cosmetic.id()),
                        "id repetido en " + tipo + ": " + cosmetic.id() + " (haria inalcanzable a uno de los dos)");
            }
        }
    }

    @Test
    void permissionNodeMatchesTheCommandPath() {
        Cosmetic caos = Cosmetic.AURAS.stream().filter(c -> c.id().equals("caos")).findFirst().orElseThrow();
        assertEquals("drakes.cosmetics.aura.caos", caos.permiso("aura"));
    }

    @Test
    void aliasesResolveToTheSameCanonicalType() {
        assertEquals("trail", Cosmetic.tipoCanonico("rastro"));
        assertEquals("trail", Cosmetic.tipoCanonico("TRAIL"));
        assertEquals("death", Cosmetic.tipoCanonico("muerte"));
        assertEquals("death", Cosmetic.tipoCanonico("Death"));
        assertEquals("aura", Cosmetic.tipoCanonico("AURA"));
        assertTrue(Cosmetic.tipoCanonico("inventado").isEmpty());
    }

    @Test
    void everyRankFromTheStoreHasAtLeastOneCosmetic() {
        Set<String> rangos = new HashSet<>();
        for (String tipo : List.of("aura", "rastro", "muerte")) {
            Cosmetic.of(tipo).forEach(c -> rangos.add(c.rango()));
        }
        for (String rango : List.of("Hércules", "Hestia", "Hermes", "Hefesto", "Artemisa",
                "Afrodita", "Zeus", "Thor", "Anubis", "Poseidón", "Titanes", "Staff")) {
            assertTrue(rangos.contains(rango), "ningun cosmetico corresponde al rango " + rango);
        }
    }

    @Test
    void catalogGrewBeyondTheOriginalThirteen() {
        int total = Cosmetic.AURAS.size() + Cosmetic.RASTROS.size() + Cosmetic.MUERTES.size();
        assertTrue(total > 13, "el catalogo deberia haber crecido, va en " + total);
        assertTrue(Cosmetic.of("inventado").isEmpty(), "un tipo inexistente no puede devolver cosmeticos");
    }
}
