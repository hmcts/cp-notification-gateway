package uk.gov.hmcts.cp.notification.blob;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.hmcts.cp.notification.integration.base.AbstractBlobIntegrationTest;
import uk.gov.hmcts.cp.notification.integration.Fixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.cp.notification.integration.stubs.AzureBlobFileStoreStub.anAzureBlobFileStore;

class AttachmentDownloaderIntegrationTest extends AbstractBlobIntegrationTest {
    @Autowired
    private AttachmentDownloader attachmentDownloader;

    @Test
    void downloads_attachment_bytes_for_a_valid_file_uri() {
        final byte[] content = Fixtures.loadBytes("attachments/report.csv");
        final String blobName = "report-" + UUID.randomUUID() + ".csv";
        final String fileUri = anAzureBlobFileStore().containing(blobName, content).uriOf(blobName);

        assertThat(attachmentDownloader.download(fileUri)).isEqualTo(content);
    }

    @Test
    void missing_blob_404_raises_a_permanent_failure_not_a_retryable_error() {
        final String missing = anAzureBlobFileStore().uriOf("does-not-exist-" + UUID.randomUUID() + ".csv");

        assertThatThrownBy(() -> attachmentDownloader.download(missing))
                .isInstanceOf(PermanentBlobException.class);
    }
}
