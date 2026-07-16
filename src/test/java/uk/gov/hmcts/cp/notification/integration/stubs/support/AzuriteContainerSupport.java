package uk.gov.hmcts.cp.notification.integration.stubs.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import uk.gov.hmcts.cp.notification.integration.stubs.AzureBlobFileStoreStub;

public final class AzuriteContainerSupport {
    private static final int BLOB_PORT = 10_000;
    private static final String ACCOUNT = "devstoreaccount1";

    private static final String ACCOUNT_KEY =
            "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";

    @SuppressWarnings("resource")
    private static final GenericContainer<?> AZURITE =
            new GenericContainer<>(DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:3.35.0"))
                    .withExposedPorts(BLOB_PORT)
                    .withCommand("azurite-blob", "--blobHost", "0.0.0.0", "--skipApiVersionCheck");

    static {
        AZURITE.start();
    }

    private AzuriteContainerSupport() {
    }

    public static String getConnectionString() {
        final String endpoint = "http://" + AZURITE.getHost() + ":" + AZURITE.getMappedPort(BLOB_PORT) + "/" + ACCOUNT;
        return "DefaultEndpointsProtocol=http;"
                + "AccountName=" + ACCOUNT + ";"
                + "AccountKey=" + ACCOUNT_KEY + ";"
                + "BlobEndpoint=" + endpoint + ";";
    }

    public static void registerProperties(final DynamicPropertyRegistry registry) {
        registry.add("cp.notification.blob.connection-string", AzuriteContainerSupport::getConnectionString);
    }
}
