package uk.gov.hmcts.cp.notification.sender;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import uk.gov.service.notify.NotificationClient;

@Configuration
class GovNotifyConfig {
    @Bean
    /* default */ NotificationClient notificationClient(
            @Value("${cp.notification.govnotify.base-url:}") final String baseUrl,
            @Value("${cp.notification.govnotify.api-key:}") final String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException(
                    "cp.notification.govnotify.api-key (env CP_NG_GOVNOTIFY_API_KEY) must be set — "
                            + "the Gov.Notify client cannot start without it");
        }
        return new NotificationClient(apiKey, baseUrl);
    }
}
