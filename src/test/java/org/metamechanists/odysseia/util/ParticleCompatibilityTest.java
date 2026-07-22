package org.metamechanists.odysseia.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParticleCompatibilityTest {
    private static final Pattern DIRECT_DRAGON_BREATH = Pattern.compile(
            "spawnParticle\\s*\\(\\s*(?:org\\.bukkit\\.)?Particle\\.DRAGON_BREATH");

    @Test
    void dragonBreathIsAlwaysSpawnedThroughCompatibilityLayer() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java");
        Path compatibilityLayer = sourceRoot.resolve(Path.of(
                "org", "metamechanists", "odysseia", "util", "ParticleCompat.java"));

        List<Path> offenders;
        try (var files = Files.walk(sourceRoot)) {
            offenders = files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.equals(compatibilityLayer))
                    .filter(this::containsDirectDragonBreath)
                    .toList();
        }
        assertEquals(List.of(), offenders,
                "Paper 1.21.11 requires Float data for DRAGON_BREATH; use ParticleCompat");
    }

    private boolean containsDirectDragonBreath(Path path) {
        try {
            return DIRECT_DRAGON_BREATH.matcher(Files.readString(path)).find();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo auditar " + path, exception);
        }
    }
}
