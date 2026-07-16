package uk.gov.hmcts.cp.notification.integration.base;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import uk.gov.hmcts.cp.notification.integration.stubs.support.AzuriteContainerSupport;

public abstract class AbstractBlobIntegrationTest extends AbstractIntegrationTest {
    @DynamicPropertySource
    static void blobProperties(final DynamicPropertyRegistry registry) {
        AzuriteContainerSupport.registerProperties(registry);
    }
}
