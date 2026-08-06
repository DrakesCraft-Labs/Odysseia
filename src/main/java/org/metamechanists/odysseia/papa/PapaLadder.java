package org.metamechanists.odysseia.papa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * La escalera de canje, ordenada de mas barato a mas caro.
 *
 * Vive aparte del resto para poder comprobar las reglas sin levantar un servidor: cuanto falta
 * para el siguiente peldano, cuales estan al alcance y cuales ya se gastaron.
 */
public final class PapaLadder {

    private final List<PapaTier> niveles;

    public PapaLadder(Collection<PapaTier> niveles) {
        List<PapaTier> ordenados = new ArrayList<>(niveles);
        ordenados.sort(Comparator.comparingInt(PapaTier::coste));
        this.niveles = List.copyOf(ordenados);
    }

    public List<PapaTier> todos() {
        return niveles;
    }

    public Optional<PapaTier> porId(String id) {
        return niveles.stream().filter(nivel -> nivel.id().equalsIgnoreCase(id)).findFirst();
    }

    /**
     * Si el jugador puede canjear este nivel ahora mismo.
     *
     * @param yaCanjeados ids de los niveles de una sola vez que ya gasto
     */
    public boolean disponible(PapaTier nivel, int papas, Set<String> yaCanjeados) {
        if (nivel.unica() && yaCanjeados.contains(nivel.id())) return false;
        return papas >= nivel.coste();
    }

    /** Los niveles que puede canjear ya. */
    public List<PapaTier> alcanzables(int papas, Set<String> yaCanjeados) {
        return niveles.stream().filter(nivel -> disponible(nivel, papas, yaCanjeados)).toList();
    }

    /**
     * El siguiente peldano al que aspirar: el mas barato que todavia no puede pagar.
     *
     * Los de una sola vez ya gastados no cuentan: proponerle otra vez algo que ya tiene seria
     * ruido.
     */
    public Optional<PapaTier> siguiente(int papas, Set<String> yaCanjeados) {
        return niveles.stream()
                .filter(nivel -> !(nivel.unica() && yaCanjeados.contains(nivel.id())))
                .filter(nivel -> papas < nivel.coste())
                .findFirst();
    }

    /** Cuantas papas le faltan para el siguiente peldano, o 0 si ya lo tiene todo al alcance. */
    public int faltanParaSiguiente(int papas, Set<String> yaCanjeados) {
        return siguiente(papas, yaCanjeados).map(nivel -> nivel.coste() - papas).orElse(0);
    }
}
