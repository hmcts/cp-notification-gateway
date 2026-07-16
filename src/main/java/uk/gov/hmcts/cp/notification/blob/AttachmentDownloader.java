package uk.gov.hmcts.cp.notification.blob;

import com.azure.storage.blob.models.BlobStorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttachmentDownloader {
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;

    private final BlobClientFactory blobClientFactory;

    public byte[] download(final String fileUri) {
        try {
            return blobClientFactory.blobClientFor(fileUri).downloadContent().toBytes();
        } catch (final BlobStorageException e) {
            final int status = e.getStatusCode();
            if (status == HTTP_NOT_FOUND || status == HTTP_FORBIDDEN) {
                throw new PermanentBlobException(
                        "Permanent blob failure (HTTP " + status + ") for " + fileUri, status, e);
            }
            throw e;
        }
    }
}
