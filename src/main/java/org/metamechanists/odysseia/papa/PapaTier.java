package org.metamechanists.odysseia.papa;

import java.util.List;

/**
 * Un peldano de la escalera de canje de la Papa de mar.
 *
 * @param id          identificador interno, el que se guarda cuando el canje es de una sola vez
 * @param nombre      lo que ve el jugador
 * @param coste       cuantas papas cuesta
 * @param icono       material del icono en el menu
 * @param descripcion lineas de lore que explican que se lleva
 * @param comandos    lo que se ejecuta en consola al canjear; admite {jugador}
 * @param unica       si true, solo se puede canjear una vez por jugador
 */
public record PapaTier(String id, String nombre, int coste, String icono,
                       List<String> descripcion, List<String> comandos, boolean unica) {

    public PapaTier {
        descripcion = List.copyOf(descripcion);
        comandos = List.copyOf(comandos);
    }

    /** El coste expresado en stacks y unidades sueltas, para el menu. */
    public String costeLegible() {
        int stacks = coste / 64;
        int sueltas = coste % 64;
        if (stacks == 0) return coste + (coste == 1 ? " papa" : " papas");
        String texto = stacks + (stacks == 1 ? " stack" : " stacks");
        return sueltas == 0 ? texto : texto + " y " + sueltas;
    }
}
