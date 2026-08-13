package uk.gov.hmcts.cp.notification.blob;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AttachmentDownloader {
    private static final int HTTP_FORBIDDEN = 403;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int HTTP_PAYLOAD_TOO_LARGE = 413;

    private final BlobClientFactory blobClientFactory;
    private final long maxAttachmentBytes;

    public AttachmentDownloader(
            final BlobClientFactory blobClientFactory,
            @Value("${cp.notification.blob.max-attachment-bytes:15728640}") final long maxAttachmentBytes) {
        this.blobClientFactory = blobClientFactory;
        this.maxAttachmentBytes = maxAttachmentBytes;
    }

    public byte[] download(final String fileUri) {
        try {
            final BlobClient blobClient = blobClientFactory.blobClientFor(fileUri);
            final long size = blobClient.getProperties().getBlobSize();
            if (size > maxAttachmentBytes) {
                throw new PermanentBlobException(
                        "Attachment " + fileUri + " is " + size + " bytes, exceeding the maximum "
                                + maxAttachmentBytes + " bytes", HTTP_PAYLOAD_TOO_LARGE, null);
            }
            return blobClient.downloadContent().toBytes();
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
