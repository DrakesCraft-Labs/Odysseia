package org.metamechanists.odysseia.cosmetics;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La geometria de los cosmeticos con forma.
 *
 * Lo que se protege aqui es que las formas **se lean como lo que son**: un aro que se incline deja
 * de parecer un halo, unas alas que no sigan la espalda parecen un error, y una orbita que no gire
 * es un anillo pintado. Todo eso se puede comprobar con numeros, sin servidor.
 */
class CosmeticShapesTest {

    private static final double MARGEN = 1e-9;

    // ── Halo ────────────────────────────────────────────────────

    @Test
    void elHaloEsPlanoYSiempreALaMismaAltura() {
        List<Vector> aro = CosmeticShapes.halo(0.5D, 2.3D, 12);
        assertEquals(12, aro.size());
        for (Vector punto : aro) {
            assertEquals(2.3D, punto.getY(), MARGEN, "un halo inclinado deja de parecer un halo");
        }
    }

    @Test
    void todosLosPuntosDelHaloEstanAlMismoRadio() {
        for (Vector punto : CosmeticShapes.halo(0.5D, 2.3D, 16)) {
            double radio = Math.hypot(punto.getX(), punto.getZ());
            assertEquals(0.5D, radio, 1e-9);
        }
    }

    // ── Alas ────────────────────────────────────────────────────

    @Test
    void lasAlasSonSimetricas() {
        List<Vector> alas = CosmeticShapes.alas(0, 0, 8);
        assertEquals(16, alas.size(), "ocho puntos por ala, dos alas");
        // Sin rotacion, cada punto del ala izquierda tiene su espejo en la derecha.
        for (int i = 0; i < 8; i++) {
            Vector izq = alas.get(i);
            Vector der = alas.get(i + 8);
            assertEquals(-izq.getX(), der.getX(), MARGEN);
            assertEquals(izq.getY(), der.getY(), MARGEN);
            assertEquals(izq.getZ(), der.getZ(), MARGEN);
        }
    }

    @Test
    void lasAlasVanDetrasDelJugador() {
        // Sin rotacion, "detras" es Z negativa.
        for (Vector punto : CosmeticShapes.alas(0, 0, 8)) {
            assertTrue(punto.getZ() < 0, "un ala delante del pecho no es un ala");
        }
    }

    @Test
    void lasAlasBaten() {
        // Dos instantes distintos del ciclo tienen que dar aperturas distintas.
        Vector puntaAhora = CosmeticShapes.alas(0, 0, 8).get(7);
        Vector puntaDespues = CosmeticShapes.alas(0, 10, 8).get(7);
        assertNotEquals(puntaAhora.getX(), puntaDespues.getX(), "las alas no se mueven");
    }

    @Test
    void lasAlasSiguenLaEspaldaAlGirar() {
        Vector sinGirar = CosmeticShapes.alas(0, 0, 8).get(0);
        Vector girado90 = CosmeticShapes.alas(Math.PI / 2, 0, 8).get(0);
        // Al girar 90 grados, lo que era X pasa a ser Z.
        assertEquals(sinGirar.getX(), girado90.getZ(), 1e-9);
        assertEquals(sinGirar.getY(), girado90.getY(), MARGEN);
    }

    // ── Cola ────────────────────────────────────────────────────

    @Test
    void laColaSeAlejaYCaeSegunAvanza() {
        List<Vector> cola = CosmeticShapes.cola(0, 0, 10);
        assertTrue(cola.get(9).getZ() < cola.get(0).getZ(), "la punta debe quedar mas atras");
        assertTrue(cola.get(9).getY() < cola.get(0).getY(), "la punta debe caer respecto a la base");
    }

    @Test
    void laPuntaDeLaColaSeMueveMasQueLaBase() {
        // Es lo que la hace parecer una cola y no un palo.
        var enCero = CosmeticShapes.cola(0, 0, 10);
        var despues = CosmeticShapes.cola(0, 8, 10);
        double mueveBase = Math.abs(enCero.get(0).getX() - despues.get(0).getX());
        double muevePunta = Math.abs(enCero.get(9).getX() - despues.get(9).getX());
        assertTrue(muevePunta > mueveBase, "la punta tiene que ondular mas que la base");
    }

    // ── Orbita y espiral ────────────────────────────────────────

    @Test
    void laOrbitaGiraConElTiempo() {
        Vector ahora = CosmeticShapes.orbita(0, 5, 0.9D).get(0);
        Vector despues = CosmeticShapes.orbita(20, 5, 0.9D).get(0);
        assertNotEquals(ahora.getX(), despues.getX(), "una orbita que no gira es un anillo pintado");
    }

    @Test
    void losBrillitosNoVanTodosALaMismaAltura() {
        List<Vector> brillos = CosmeticShapes.orbita(7, 5, 0.9D);
        long alturasDistintas = brillos.stream().map(Vector::getY).distinct().count();
        assertTrue(alturasDistintas > 1, "si van a la misma altura parecen un aro, no luciernagas");
    }

    @Test
    void laEspiralAsciendeYSeEstrecha() {
        List<Vector> espiral = CosmeticShapes.espiral(0, 20, 0.8D, 2.2D);
        assertTrue(espiral.get(19).getY() > espiral.get(0).getY(), "la espiral debe subir");
        double radioAbajo = Math.hypot(espiral.get(0).getX(), espiral.get(0).getZ());
        double radioArriba = Math.hypot(espiral.get(19).getX(), espiral.get(19).getZ());
        assertTrue(radioArriba < radioAbajo, "debe cerrarse en punta");
    }

    // ── Rotacion ────────────────────────────────────────────────

    @Test
    void rotarUnaVueltaCompletaDejaTodoIgual() {
        Vector original = new Vector(1, 2, 3);
        Vector vuelta = CosmeticShapes.rotarY(original, 2 * Math.PI);
        assertEquals(original.getX(), vuelta.getX(), 1e-9);
        assertEquals(original.getY(), vuelta.getY(), MARGEN);
        assertEquals(original.getZ(), vuelta.getZ(), 1e-9);
    }

    @Test
    void rotarNoCambiaLaAltura() {
        assertEquals(2.0D, CosmeticShapes.rotarY(new Vector(1, 2, 3), 1.234D).getY(), MARGEN);
    }
}
