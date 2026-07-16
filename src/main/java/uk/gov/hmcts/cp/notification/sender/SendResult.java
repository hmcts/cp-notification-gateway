package uk.gov.hmcts.cp.notification.sender;

import lombok.Builder;

@Builder
public record SendResult(String reference) {
}
