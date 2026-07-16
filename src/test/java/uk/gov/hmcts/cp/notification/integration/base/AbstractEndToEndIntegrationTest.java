package uk.gov.hmcts.cp.notification.integration.base;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import uk.gov.hmcts.cp.notification.integration.stubs.support.AzuriteContainerSupport;
import uk.gov.hmcts.cp.notification.integration.stubs.GovUkNotifyStubService;
import uk.gov.hmcts.cp.notification.integration.stubs.support.ServiceBusContainerSupport;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractEndToEndIntegrationTest extends AbstractIntegrationTest {
    @DynamicPropertySource
    static void endToEndProperties(final DynamicPropertyRegistry registry) {
        ServiceBusContainerSupport.registerProperties(registry);
        AzuriteContainerSupport.registerProperties(registry);
        GovUkNotifyStubService.registerProperties(registry);

        registry.add("job.executor.poll-interval", () -> "500");
    }
}
