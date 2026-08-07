package org.metamechanists.odysseia.cosmetics;

import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * La geometria de los cosmeticos con forma: aros, alas, colas y orbitas.
 *
 * Las auras de antes solo soltaban particulas dentro de una caja sobre la cabeza. Eso vale para una
 * neblina, pero no para un ala: un ala necesita saber donde esta la espalda del jugador, y un aro
 * necesita mantenerse plano mientras el jugador gira.
 *
 * Aqui solo hay matematicas. Se devuelven **desplazamientos** respecto al jugador, en bloques, y
 * quien llame decide donde nacen las particulas. Asi se puede comprobar la forma sin levantar un
 * servidor, que es justo lo que no se podia hacer con el switch anterior.
 *
 * El parametro {@code fase} es el tiempo: se le pasa un contador de ticks y las formas que se
 * mueven --las alas al batir, las orbitas al girar-- avanzan solas.
 */
public final class CosmeticShapes {

    private CosmeticShapes() {
    }

    /**
     * Un aro horizontal sobre la cabeza.
     *
     * No gira con el jugador a proposito: un halo que se incline al mirar al suelo deja de leerse
     * como un halo. Se queda plano y a la misma altura pase lo que pase.
     */
    public static List<Vector> halo(double radio, double altura, int puntos) {
        List<Vector> salida = new ArrayList<>(puntos);
        for (int i = 0; i < puntos; i++) {
            double angulo = 2 * Math.PI * i / puntos;
            salida.add(new Vector(Math.cos(angulo) * radio, altura, Math.sin(angulo) * radio));
        }
        return salida;
    }

    /**
     * Dos alas simetricas a la espalda, que baten con el tiempo.
     *
     * La forma de cada ala es media parabola: ancha arriba y estrecha abajo, como una pluma. El
     * batido no mueve las alas enteras sino que abre y cierra el angulo, que es lo que se lee como
     * aleteo y no como un objeto rigido sacudiendose.
     *
     * @param yawRadianes hacia donde mira el jugador; las alas van detras
     * @param fase        contador de ticks
     */
    public static List<Vector> alas(double yawRadianes, double fase, int puntosPorAla) {
        List<Vector> salida = new ArrayList<>(puntosPorAla * 2);
        // El batido oscila entre casi cerrado y bien abierto.
        double apertura = 0.55D + 0.35D * Math.sin(fase * 0.15D);

        for (int lado = -1; lado <= 1; lado += 2) {
            for (int i = 0; i < puntosPorAla; i++) {
                double t = i / (double) puntosPorAla;      // 0 en el hombro, 1 en la punta
                double ancho = t * 1.3D * apertura;
                double alto = 1.1D - (t * t * 1.4D);        // parabola: sube y luego cae
                // Coordenadas locales: X hacia el lado, Z hacia atras.
                double x = ancho * lado;
                double z = -0.25D - t * 0.35D;
                salida.add(rotarY(new Vector(x, alto, z), yawRadianes));
            }
        }
        return salida;
    }

    /**
     * Una cola que sale de la cintura hacia atras y ondula.
     *
     * La ondulacion depende de la distancia al cuerpo, no solo del tiempo: asi la punta se mueve
     * mas que la base, que es como se mueve una cola de verdad.
     */
    public static List<Vector> cola(double yawRadianes, double fase, int puntos) {
        List<Vector> salida = new ArrayList<>(puntos);
        for (int i = 0; i < puntos; i++) {
            double t = i / (double) puntos;
            double largo = 0.3D + t * 1.5D;
            double onda = Math.sin(fase * 0.2D - t * 3.0D) * 0.45D * t;
            double caida = 0.9D - t * 0.7D;
            salida.add(rotarY(new Vector(onda, caida, -largo), yawRadianes));
        }
        return salida;
    }

    /**
     * Brillitos orbitando alrededor del jugador, a distintas alturas.
     *
     * Cada uno lleva su propio desfase y su propia velocidad vertical, para que no parezcan un
     * anillo rigido sino luciernagas sueltas.
     */
    public static List<Vector> orbita(double fase, int cuantos, double radio) {
        List<Vector> salida = new ArrayList<>(cuantos);
        for (int i = 0; i < cuantos; i++) {
            double desfase = 2 * Math.PI * i / cuantos;
            double angulo = fase * 0.12D + desfase;
            // Cada brillo sube y baja a su ritmo; el 1.7 evita que todos coincidan.
            double altura = 1.0D + Math.sin(fase * 0.08D + desfase * 1.7D) * 0.6D;
            salida.add(new Vector(Math.cos(angulo) * radio, altura, Math.sin(angulo) * radio));
        }
        return salida;
    }

    /**
     * Una espiral que sube alrededor del jugador, como una constelacion en marcha.
     *
     * Es la forma de "Star": no rodea, asciende.
     */
    public static List<Vector> espiral(double fase, int puntos, double radio, double altura) {
        List<Vector> salida = new ArrayList<>(puntos);
        for (int i = 0; i < puntos; i++) {
            double t = i / (double) puntos;
            double angulo = fase * 0.1D + t * Math.PI * 4;   // dos vueltas completas
            // El radio se estrecha arriba: da sensacion de que la espiral se cierra en punta.
            double r = radio * (1.0D - t * 0.55D);
            salida.add(new Vector(Math.cos(angulo) * r, t * altura, Math.sin(angulo) * r));
        }
        return salida;
    }

    /** Gira un desplazamiento local alrededor del eje Y, para orientarlo segun mire el jugador. */
    static Vector rotarY(Vector local, double radianes) {
        double cos = Math.cos(radianes);
        double sin = Math.sin(radianes);
        return new Vector(
                local.getX() * cos - local.getZ() * sin,
                local.getY(),
                local.getX() * sin + local.getZ() * cos);
    }
}
