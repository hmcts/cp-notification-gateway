package uk.gov.hmcts.cp.notification.blob;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.BlobUrlParts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BlobClientFactory {
    private final String connectionString;
    private final TokenCredential credential;
    private final Map<String, BlobServiceClient> serviceClientsByAccount = new ConcurrentHashMap<>();

    public BlobClientFactory(
            @Value("${cp.notification.blob.connection-string:}") final String connectionString) {
        this.connectionString = connectionString;
        this.credential = StringUtils.hasText(connectionString)
                ? null
                : new DefaultAzureCredentialBuilder().build();
    }

    public BlobClient blobClientFor(final String fileUri) {
        final BlobUrlParts parts = BlobUrlParts.parse(fileUri);
        final String accountEndpoint = parts.getScheme() + "://" + parts.getHost();
        final BlobServiceClient serviceClient =
                serviceClientsByAccount.computeIfAbsent(accountEndpoint, this::buildServiceClient);
        return serviceClient
                .getBlobContainerClient(parts.getBlobContainerName())
                .getBlobClient(parts.getBlobName());
    }

    private BlobServiceClient buildServiceClient(final String accountEndpoint) {
        if (StringUtils.hasText(connectionString)) {
            return new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();
        }
        return new BlobServiceClientBuilder()
                .credential(credential)
                .endpoint(accountEndpoint)
                .buildClient();
    }
}
