Feature: Email notification delivery

  Report recipients reliably receive their email notification: once a
  send-email-notification command is raised, the gateway delivers the email
  through the notification provider and records the notification as sent. When
  the attachment cannot be retrieved, the gateway fails the notification rather
  than sending a broken email. When the originating context provides a reply
  queue the gateway returns the delivery outcome to it as a result event; when
  no reply queue is provided the send completes silently.

  @smoke
  Scenario: A delivered notification is recorded as sent and its outcome returned to the originator
    Given a send-email-notification command for a recipient with an attachment
    And the originator provides a reply queue
    When the gateway processes the command
    Then the email is sent via the Gov.UK Notify provider
    And the delivery status is polled from the provider
    And the notification is recorded as SENT
    And a notification-sent result event is published to the originator's reply queue

  Scenario: A notification with an unretrievable attachment is failed, no email is sent, and the failure returned to the originator
    Given a send-email-notification command whose attachment is missing
    And the originator provides a reply queue
    When the gateway processes the command
    Then the notification is recorded as FAILED
    And no email is sent via the Gov.UK Notify provider
    And a notification-failed result event is published to the originator's reply queue

  Scenario: A notification with no reply queue completes silently with no result event
    Given a send-email-notification command for a recipient with an attachment
    When the gateway processes the command
    Then the notification is recorded as SENT
    And no result event is published
