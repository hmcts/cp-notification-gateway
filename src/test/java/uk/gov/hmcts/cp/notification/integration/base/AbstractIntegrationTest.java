package uk.gov.hmcts.cp.notification.integration.base;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import uk.gov.hmcts.cp.notification.integration.stubs.support.PostgresContainerSupport;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
    @DynamicPropertySource
    static void datasourceProperties(final DynamicPropertyRegistry registry) {
        PostgresContainerSupport.registerProperties(registry);
    }
}
