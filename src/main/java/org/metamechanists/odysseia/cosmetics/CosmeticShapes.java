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

    /** Plumas que cuelgan de cada punto del hueso. Da grosor al ala en vez de dejarla en un trazo. */
    private static final int PLUMAS_POR_FILA = 4;

    /**
     * Dos alas simetricas a la espalda, que baten con el tiempo.
     *
     * Antes cada ala era una sola curva de puntos, asi que en el juego se veia una raya y no un ala.
     * Ahora se reparte la misma cantidad de puntos sobre una superficie: un hueso que sale del
     * hombro hacia arriba y afuera, y plumas que cuelgan de el. El largo de las plumas es corto en
     * el hombro, maximo pasado el medio y vuelve a acortarse en la punta, que es el perfil que hace
     * que la silueta se lea como un ala de verdad.
     *
     * El batido no mueve las alas enteras sino que abre y cierra el angulo, que es lo que se lee como
     * aleteo y no como un objeto rigido sacudiendose.
     *
     * @param yawRadianes  hacia donde mira el jugador; las alas van detras
     * @param fase         contador de ticks
     * @param puntosPorAla cuantos puntos dibuja cada ala; a mas puntos, membrana mas densa
     */
    public static List<Vector> alas(double yawRadianes, double fase, int puntosPorAla) {
        List<Vector> salida = new ArrayList<>(puntosPorAla * 2);
        // El batido oscila entre casi cerrado y bien abierto.
        double apertura = 0.55D + 0.35D * Math.sin(fase * 0.15D);
        // Los puntos se reparten en filas a lo largo del hueso; cada fila cuelga sus plumas.
        int filas = Math.max(1, (int) Math.ceil(puntosPorAla / (double) PLUMAS_POR_FILA));

        for (int lado = -1; lado <= 1; lado += 2) {
            for (int i = 0; i < puntosPorAla; i++) {
                int fila = i / PLUMAS_POR_FILA;
                int pluma = i % PLUMAS_POR_FILA;
                // t recorre el hueso del hombro (0) a la punta (1); s baja por la pluma.
                double t = filas > 1 ? fila / (double) (filas - 1) : 0.0D;
                double s = PLUMAS_POR_FILA > 1 ? pluma / (double) (PLUMAS_POR_FILA - 1) : 0.0D;

                // Borde superior del ala: nace junto al hombro y se aleja subiendo.
                double huesoX = (0.22D + t * 1.25D) * apertura;
                double huesoY = 1.45D + 0.5D * Math.sin(t * Math.PI * 0.55D);
                // Perfil de las plumas: nunca llega a cero, para que la punta no se deshilache.
                double largo = 1.2D * Math.sin(Math.PI * (0.12D + 0.8D * t));

                // Coordenadas locales: X hacia el lado, Z hacia atras.
                double x = (huesoX + s * largo * 0.3D) * lado;
                double y = huesoY - s * largo;
                double z = -0.28D - t * 0.42D - s * 0.12D;
                salida.add(rotarY(new Vector(x, y, z), yawRadianes));
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

    /**
     * Una corona de picos sobre la cabeza.
     *
     * Los picos alternan alto y bajo como una corona de verdad, y el aro de la base se dibuja
     * entero: sin el, los picos sueltos parecen particulas al azar y no una pieza.
     *
     * @param picos  cuantas puntas tiene la corona
     * @param radio  cuanto se separa de la cabeza
     * @param altura donde se apoya la base
     */
    public static List<Vector> corona(double fase, int picos, double radio, double altura) {
        List<Vector> salida = new ArrayList<>(picos * 3);
        // Un latido lento que sube y baja la corona entera, para que no parezca pegada.
        double flote = Math.sin(fase * 0.06D) * 0.05D;

        for (int i = 0; i < picos; i++) {
            double angulo = 2 * Math.PI * i / picos;
            double cos = Math.cos(angulo);
            double sin = Math.sin(angulo);
            // Picos alternos: los pares son las puntas largas.
            double alto = (i % 2 == 0) ? 0.34D : 0.18D;

            salida.add(new Vector(cos * radio, altura + flote, sin * radio));
            salida.add(new Vector(cos * radio, altura + flote + alto * 0.55D, sin * radio));
            salida.add(new Vector(cos * radio * 0.88D, altura + flote + alto, sin * radio * 0.88D));
        }
        return salida;
    }

    /**
     * Un anillo inclinado que gira alrededor del jugador, tipo Saturno.
     *
     * La inclinacion es fija respecto al mundo y el giro se aplica dentro del propio anillo, no
     * rotando la lista entera: asi el aro mantiene su plano y lo que se mueve son los puntos sobre
     * el, que es lo que se lee como un anillo girando y no como un aro bailando.
     *
     * @param inclinacion cuanto se levanta un lado del anillo, en bloques
     */
    public static List<Vector> anilloInclinado(double fase, int puntos, double radio, double altura,
                                               double inclinacion) {
        List<Vector> salida = new ArrayList<>(puntos);
        for (int i = 0; i < puntos; i++) {
            double angulo = 2 * Math.PI * i / puntos + fase * 0.05D;
            double cos = Math.cos(angulo);
            double sin = Math.sin(angulo);
            salida.add(new Vector(cos * radio, altura + sin * inclinacion, sin * radio));
        }
        return salida;
    }

    /**
     * Un remolino a los pies: varios brazos que giran y se cierran hacia arriba.
     *
     * Cada brazo arranca desfasado y el radio se estrecha con la altura, de modo que el conjunto
     * se lee como agua succionada y no como un cilindro de particulas.
     *
     * @param brazos      cuantas lineas forman el remolino
     * @param puntosBrazo puntos de cada linea
     */
    public static List<Vector> vortice(double fase, int brazos, int puntosBrazo, double radio,
                                       double altura) {
        List<Vector> salida = new ArrayList<>(brazos * puntosBrazo);
        for (int brazo = 0; brazo < brazos; brazo++) {
            double desfase = 2 * Math.PI * brazo / brazos;
            for (int i = 0; i < puntosBrazo; i++) {
                double t = puntosBrazo > 1 ? i / (double) (puntosBrazo - 1) : 0.0D;
                // El giro se acelera arriba: es lo que da la sensacion de succion.
                double angulo = desfase + fase * 0.16D + t * Math.PI * 1.6D;
                double r = radio * (1.0D - t * 0.7D);
                salida.add(new Vector(Math.cos(angulo) * r, t * altura, Math.sin(angulo) * r));
            }
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
