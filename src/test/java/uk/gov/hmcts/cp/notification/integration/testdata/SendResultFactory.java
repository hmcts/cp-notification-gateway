package uk.gov.hmcts.cp.notification.integration.testdata;

import uk.gov.hmcts.cp.notification.sender.SendResult;

public final class SendResultFactory {

    private SendResultFactory() {
    }

    public static SendResult.SendResultBuilder aSendResult() {
        return SendResult.builder()
                .reference("notify-ref");
    }
}
