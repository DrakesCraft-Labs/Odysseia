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

    @Test void exactJavaIdentityWinsOverNormalizedBedrockAlias() throws Exception {
        UUID javaUuid = UUID.randomUUID();
        UUID bedrockUuid = UUID.randomUUID();
        resolver.observe(javaUuid, "JackStar");
        resolver.observe(bedrockUuid, ".JackStar");
        assertEquals(javaUuid, resolver.resolve("JackStar").uuid());
        assertEquals(bedrockUuid, resolver.resolve(".JackStar").uuid());
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

    @Test void sameIdentityRejoiningDoesNotDisplaceItself() throws Exception {
        UUID uuid = UUID.randomUUID();
        assertTrue(resolver.observe(uuid, "Mr_Em1lio").isEmpty());
        assertTrue(resolver.observe(uuid, "Mr_Em1lio").isEmpty());
        assertEquals(uuid, resolver.resolve("Mr_Em1lio").uuid());
    }

    @Test void nickClaimedByAnotherUuidNoLongerBreaksIdentityRegistration() throws Exception {
        UUID previous = UUID.randomUUID();
        UUID current = UUID.randomUUID();
        resolver.observe(previous, "Mr_Em1lio");
        assertEquals(List.of(previous), resolver.observe(current, "Mr_Em1lio"));
        assertEquals(current, resolver.resolve("Mr_Em1lio").uuid());
        assertTrue(repository.findIdentityByUuid(previous).isPresent(), "la identidad desplazada no se borra");
        assertTrue(repository.findByAlias("Mr_Em1lio").stream().anyMatch(identity -> identity.uuid().equals(previous)),
                "el nick en claro sobrevive como alias del titular anterior");
    }

    @Test void displacedIdentityKeepsItsPendingDeliveryReachableByUuid() throws Exception {
        UUID previous = UUID.randomUUID();
        ProductDefinition product = new ProductDefinition("test", 1, "Test", "test", "test", "test", 1,
                VerificationState.VERIFIED_PRODUCTION, List.of(), List.of());
        resolver.observe(previous, "Mr_Em1lio");
        repository.createOrLoad("TEBEX", "txn-displaced", "Mr_Em1lio", previous, product, "test");
        resolver.observe(UUID.randomUUID(), "Mr_Em1lio");
        assertEquals("txn-displaced", repository.findPendingForIdentity(previous, "Mr_Em1lio").getFirst().transaction());
    }

    @Test void newNickClaimantCannotResumeDeliveryBoundToDisplacedUuid() throws Exception {
        UUID previous = UUID.randomUUID();
        UUID current = UUID.randomUUID();
        ProductDefinition product = new ProductDefinition("test", 1, "Test", "test", "test", "test", 1,
                VerificationState.VERIFIED_PRODUCTION, List.of(), List.of());
        resolver.observe(previous, "Mr_Em1lio");
        repository.createOrLoad("TEBEX", "txn-bound-previous", "Mr_Em1lio", previous, product, "test");

        resolver.observe(current, "Mr_Em1lio");

        assertTrue(repository.findPendingForIdentity(current, "Mr_Em1lio").isEmpty(),
                "una coincidencia de nick no puede reasignar una entrega que ya tiene UUID");
        assertEquals("txn-bound-previous",
                repository.findPendingForIdentity(previous, "Mr_Em1lio").getFirst().transaction());
    }

    @Test void caseOnlyNickChangeStaysOnTheSameIdentity() throws Exception {
        UUID uuid = UUID.randomUUID();
        resolver.observe(uuid, "Mr_Em1lio");
        assertTrue(resolver.observe(uuid, "MR_EM1LIO").isEmpty());
        assertEquals(1, repository.findByAlias("Mr_Em1lio").size());
    }

    private static void delete(File file) { File[] children = file.listFiles(); if (children != null) for (File child : children) delete(child); file.delete(); }
}
