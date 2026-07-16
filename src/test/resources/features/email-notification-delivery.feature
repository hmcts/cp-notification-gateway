Feature: Email notification delivery

  Report recipients reliably receive their email notification: once a
  send-email-notification command is raised, the gateway delivers the email
  through the notification provider and records the notification as sent. When
  the attachment cannot be retrieved, the gateway fails the notification rather
  than sending a broken email.

  @smoke
  Scenario: Email notification is delivered to the recipient
    Given a send-email-notification command for a recipient with an attachment
    When the gateway processes the command
    Then the email is sent via the Gov.UK Notify provider
    And the delivery status is polled from the provider
    And the notification is recorded as SENT

  Scenario: A notification with an unretrievable attachment is failed and no email is sent
    Given a send-email-notification command whose attachment is missing
    When the gateway processes the command
    Then the notification is recorded as FAILED
    And no email is sent via the Gov.UK Notify provider
