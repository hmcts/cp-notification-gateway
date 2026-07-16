package uk.gov.hmcts.cp.notification.integration.stubs;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import uk.gov.hmcts.cp.notification.integration.stubs.support.AzuriteContainerSupport;

public final class AzureBlobFileStoreStub {
    private static final String DEFAULT_CONTAINER = "mi-reportdata";

    private final String container;

    private AzureBlobFileStoreStub(final String container) {
        this.container = container;
    }

    public static AzureBlobFileStoreStub anAzureBlobFileStore() {
        return new AzureBlobFileStoreStub(DEFAULT_CONTAINER);
    }

    public static AzureBlobFileStoreStub anAzureBlobFileStore(final String container) {
        return new AzureBlobFileStoreStub(container);
    }

    public AzureBlobFileStoreStub containing(final String blobName, final byte[] content) {
        containerClient().getBlobClient(blobName).upload(BinaryData.fromBytes(content), true);
        return this;
    }

    public String uriOf(final String blobName) {
        return containerClient().getBlobClient(blobName).getBlobUrl();
    }

    private BlobContainerClient containerClient() {
        final BlobContainerClient client = new BlobServiceClientBuilder()
                .connectionString(AzuriteContainerSupport.getConnectionString())
                .buildClient()
                .getBlobContainerClient(container);
        client.createIfNotExists();
        return client;
    }
}
