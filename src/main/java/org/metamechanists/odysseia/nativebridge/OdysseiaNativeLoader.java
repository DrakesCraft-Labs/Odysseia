package org.metamechanists.odysseia.nativebridge;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Cargador Nativo FFM (Java 21) para vincular el motor Odysseia-Rust (.dll / .so)
 * directamente con Purpur/Paper 1.21.11 en DrakesCraft.
 */
public final class OdysseiaNativeLoader {

    private static boolean loaded = false;
    private static MethodHandle isBossNaturalHandle;
    private static MethodHandle checkBossAuraHandle;
    private static MethodHandle checkRedstoneClockHandle;
    private static MethodHandle formatTpsMessageHandle;

    public static synchronized void load() {
        if (loaded) return;
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String libName = os.contains("win") ? "odysseia_ffi.dll" : "libodysseia_ffi.so";
            Path tempLib = Files.createTempFile("odysseia_native_", os.contains("win") ? ".dll" : ".so");
            tempLib.toFile().deleteOnExit();

            try (InputStream is = OdysseiaNativeLoader.class.getResourceAsStream("/natives/" + libName)) {
                if (is != null) {
                    Files.copy(is, tempLib, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    System.loadLibrary("odysseia_ffi");
                    loaded = true;
                    return;
                }
            }

            SymbolLookup lookup = SymbolLookup.libraryLookup(tempLib, Arena.global());
            Linker linker = Linker.nativeLinker();

            lookup.find("odysseia_is_boss_natural").ifPresent(sym ->
                isBossNaturalHandle = linker.downcallHandle(sym, FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS))
            );

            lookup.find("odysseia_check_boss_aura").ifPresent(sym ->
                checkBossAuraHandle = linker.downcallHandle(sym, FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE,
                    ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE,
                    ValueLayout.JAVA_DOUBLE
                ))
            );

            lookup.find("odysseia_check_redstone_clock").ifPresent(sym ->
                checkRedstoneClockHandle = linker.downcallHandle(sym, FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT
                ))
            );

            loaded = true;
            System.out.println("[Odysseia-Rust] Motor Nativo FFM Java 21 cargado exitosamente.");
        } catch (Throwable t) {
            System.err.println("[Odysseia-Rust] Error al cargar motor nativo FFM: " + t.getMessage());
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }
}
