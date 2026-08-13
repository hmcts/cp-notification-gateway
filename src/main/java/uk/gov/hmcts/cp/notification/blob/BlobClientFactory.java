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
    private final BlobHostValidator hostValidator;
    private final Map<String, BlobServiceClient> serviceClientsByAccount = new ConcurrentHashMap<>();

    public BlobClientFactory(
            @Value("${cp.notification.blob.connection-string:}") final String connectionString,
            final BlobHostValidator hostValidator) {
        this.connectionString = connectionString;
        this.hostValidator = hostValidator;
        this.credential = StringUtils.hasText(connectionString)
                ? null
                : new DefaultAzureCredentialBuilder().build();
    }

    public BlobClient blobClientFor(final String fileUri) {
        final BlobUrlParts parts = BlobUrlParts.parse(fileUri);
        if (credential != null) {
            hostValidator.validate(parts.getScheme(), parts.getHost());
        }
        final String accountEndpoint = parts.getScheme() + "://" + parts.getHost();
        final BlobServiceClient serviceClient =
                serviceClientsByAccount.computeIfAbsent(accountEndpoint, this::buildServiceClient);
        return serviceClient
                .getBlobContainerClient(parts.getBlobContainerName())
                .getBlobClient(parts.getBlobName());
    }

    private BlobServiceClient buildServiceClient(final String accountEndpoint) {
        final BlobServiceClientBuilder builder = new BlobServiceClientBuilder();
        if (StringUtils.hasText(connectionString)) {
            builder.connectionString(connectionString);
        } else {
            builder.credential(credential).endpoint(accountEndpoint);
        }
        return builder.buildClient();
    }
}
