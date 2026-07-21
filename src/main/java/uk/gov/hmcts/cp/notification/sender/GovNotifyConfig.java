package uk.gov.hmcts.cp.notification.sender;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import uk.gov.service.notify.NotificationClient;

@Configuration
class GovNotifyConfig {
    @Bean
    /* default */ NotificationClient notificationClient(
            @Value("${cp.notification.govnotify.base-url:}") final String baseUrl,
            @Value("${cp.notification.govnotify.api-key:}") final String apiKey) {
        return new NotificationClient(apiKey, baseUrl);
    }
}
