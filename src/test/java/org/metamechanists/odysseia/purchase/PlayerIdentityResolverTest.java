package org.metamechanists.odysseia.purchase;

import org.junit.jupiter.api.*;
import java.io.File;
import java.nio.file.Files;
import java.util.UUID;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PlayerIdentityResolverTest {
    private File directory;
    private PurchaseRepository repository;
    private PlayerIdentityResolver resolver;

    @BeforeEach void setUp() throws Exception {
        directory = Files.createTempDirectory("odysseia-identities").toFile();
        repository = new PurchaseRepository(new File(directory, "purchases.db"));
        resolver = new PlayerIdentityResolver(repository);
    }
    @AfterEach void tearDown() throws Exception { repository.close(); delete(directory); }

    @Test void resolvesKnownBedrockWithAndWithoutPrefix() throws Exception {
        UUID uuid = UUID.randomUUID(); resolver.observe(uuid, ".AngelicVr6991");
        assertEquals(uuid, resolver.resolve(".AngelicVr6991").uuid());
        assertEquals(uuid, resolver.resolve("AngelicVr6991").uuid());
    }

    @Test void rejectsAmbiguousJavaAndBedrockAliases() throws Exception {
        resolver.observe(UUID.randomUUID(), "JackStar");
        resolver.observe(UUID.randomUUID(), ".JackStar");
        assertEquals(IdentityResolutionStatus.AMBIGUOUS, resolver.resolve("JackStar").status());
    }

    @Test void keepsUnknownAndInvalidNamesOutOfDelivery() throws Exception {
        assertEquals(IdentityResolutionStatus.MANUAL_REVIEW, resolver.resolve("UnknownPlayer").status());
        assertEquals(IdentityResolutionStatus.INVALID_INPUT, resolver.resolve("bad name").status());
    }

    @Test void migrationCreatesIdentityTables() throws Exception {
        repository.observeIdentity(UUID.randomUUID(), "KnownJava", "JAVA", "TEST", "HIGH");
        assertTrue(repository.findIdentityByCanonical("KnownJava").isPresent());
    }

    @Test void bedrockJoinFindsPendingPurchaseStoredWithoutPrefix() throws Exception {
        UUID uuid = UUID.randomUUID(); resolver.observe(uuid, ".AngelicVr6991");
        ProductDefinition product = new ProductDefinition("test", 1, "Test", "test", "test", "test", 1,
                VerificationState.VERIFIED_PRODUCTION, List.of(), List.of());
        repository.createOrLoad("TEBEX", "txn-bedrock", "AngelicVr6991", null, product, "test");
        assertEquals("txn-bedrock", repository.findPendingForIdentity(uuid, ".AngelicVr6991").getFirst().transaction());
    }

    private static void delete(File file) { File[] children = file.listFiles(); if (children != null) for (File child : children) delete(child); file.delete(); }
}
