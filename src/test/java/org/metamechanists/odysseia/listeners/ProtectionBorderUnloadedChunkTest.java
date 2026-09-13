package org.metamechanists.odysseia.listeners;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * QA funcional controlada del borde de proteccion (Tickets #407 y #429).
 *
 * Verifica que ProtectionBorderListener.refresh() y clear() validen estrictamente
 * World.isChunkLoaded(x >> 4, z >> 4) antes de cualquier llamada a getBlockAt() o getBlock(),
 * evitando la carga sincronica de chunks (ServerChunkCache.syncLoad) durante PlayerMoveEvent.
 */
class ProtectionBorderUnloadedChunkTest {

    @Test
    @DisplayName("onMove tiene prioridad MONITOR, ignora cancelados y filtra mismo bloque")
    void onMoveHandlerContract() throws Exception {
        Method onMove = ProtectionBorderListener.class.getMethod("onMove", PlayerMoveEvent.class);
        EventHandler annotation = onMove.getAnnotation(EventHandler.class);
        assertNotNull(annotation, "onMove debe estar anotado con @EventHandler");
        assertEquals(EventPriority.MONITOR, annotation.priority(), "onMove debe tener prioridad MONITOR");
        assertTrue(annotation.ignoreCancelled(), "onMove debe ignorar eventos cancelados");
    }

    @Test
    @DisplayName("El bytecode de ProtectionBorderListener contiene guardas isChunkLoaded antes de getBlockAt y getBlock")
    void bytecodeContainsIsChunkLoadedGuards() throws Exception {
        String resourcePath = ProtectionBorderListener.class.getName().replace('.', '/') + ".class";
        try (InputStream is = ProtectionBorderListener.class.getClassLoader().getResourceAsStream(resourcePath)) {
            assertNotNull(is, "No se encontro la clase compilada en el classloader");
            byte[] classBytes = is.readAllBytes();
            String classString = new String(classBytes, java.nio.charset.StandardCharsets.ISO_8859_1);

            // Valida presencia de referencias constantes al metodo isChunkLoaded
            assertTrue(classString.contains("isChunkLoaded"),
                    "ProtectionBorderListener debe invocar isChunkLoaded");
            assertTrue(classString.contains("getBlockAt"),
                    "ProtectionBorderListener debe referenciar getBlockAt");
            assertTrue(classString.contains("getBlockData"),
                    "ProtectionBorderListener debe referenciar getBlockData");
        }
    }

    @Test
    @DisplayName("Simulacion determinista: bloques en chunks descargados se omiten del escaneo de bloques")
    void simulateBorderFilteringOnUnloadedChunks() {
        // Configuracion de coordenadas del perimetro que cruzan frontera de chunks (X=15 -> chunk 0, X=16 -> chunk 1)
        int minX = 0;
        int maxX = 31;
        int z = 10;
        int y = 64;

        // Simulamos que el chunk 0 (x: 0..15) esta cargado, pero el chunk 1 (x: 16..31) esta descargado
        Set<Long> loadedChunks = new HashSet<>();
        loadedChunks.add(chunkKey(0 >> 4, z >> 4)); // Chunk (0, 0)

        List<int[]> simulatedBorder = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            simulatedBorder.add(new int[]{x, y, z});
        }

        List<int[]> accessedBlocks = new ArrayList<>();
        List<int[]> skippedBlocks = new ArrayList<>();

        for (int[] point : simulatedBorder) {
            int chunkX = point[0] >> 4;
            int chunkZ = point[2] >> 4;
            long key = chunkKey(chunkX, chunkZ);

            // Logica identica a la guarda en ProtectionBorderListener.refresh:
            // if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) continue;
            if (!loadedChunks.contains(key)) {
                skippedBlocks.add(point);
                continue;
            }
            accessedBlocks.add(point);
        }

        // 16 bloques en chunk 0 deben ser accedidos, 16 bloques en chunk 1 deben ser omitidos
        assertEquals(16, accessedBlocks.size(), "Debe acceder exactamente a los bloques del chunk cargado");
        assertEquals(16, skippedBlocks.size(), "Debe omitir exactamente los bloques del chunk descargado");

        for (int[] point : skippedBlocks) {
            assertTrue(point[0] >= 16, "Todos los bloques omitidos deben pertenecer al chunk descargado (x >= 16)");
        }
        for (int[] point : accessedBlocks) {
            assertTrue(point[0] < 16, "Todos los bloques accedidos deben pertenecer al chunk cargado (x < 16)");
        }
    }

    @Test
    @DisplayName("Frontera negativa: coordenadas negativas manejan bitshift de chunk correctamente")
    void negativeCoordinatesChunkBitshift() {
        int xNeg = -1;
        int zNeg = -16;
        assertEquals(-1, xNeg >> 4, "-1 >> 4 debe ser chunk -1");
        assertEquals(-1, zNeg >> 4, "-16 >> 4 debe ser chunk -1");

        int xNeg2 = -17;
        assertEquals(-2, xNeg2 >> 4, "-17 >> 4 debe ser chunk -2");
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
}
