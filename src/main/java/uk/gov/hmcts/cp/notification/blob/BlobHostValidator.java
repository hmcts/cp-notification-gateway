package uk.gov.hmcts.cp.notification.blob;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class BlobHostValidator {
    private static final String HTTPS = "https";

    private final Set<String> allowedHosts;

    public BlobHostValidator(
            @Value("${cp.notification.blob.allowed-hosts:}") final List<String> allowedHosts) {
        this.allowedHosts = allowedHosts.stream()
                .filter(StringUtils::hasText)
                .map(host -> host.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public void validate(final String scheme, final String host) {
        if (!HTTPS.equalsIgnoreCase(scheme)) {
            throw new DisallowedBlobHostException(
                    "Refusing to attach a Managed-Identity token to a non-https blob fileUri (scheme '"
                            + scheme + "')");
        }
        if (host == null || !allowedHosts.contains(host.toLowerCase(Locale.ROOT))) {
            throw new DisallowedBlobHostException(
                    "blob fileUri host '" + host + "' is not in the configured allow-list "
                            + allowedHosts);
        }
    }
}
