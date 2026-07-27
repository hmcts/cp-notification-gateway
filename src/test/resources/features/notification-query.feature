Feature: Notification record query

  A support or operations engineer triages incidents by querying a
  notification's record directly, without database access: they can look up a
  single notification by its id and search for notifications by delivery status
  and the date they were created.

  @smoke
  Scenario: An operator retrieves a notification's full record and finds it by status and creation date
    Given a notification has been recorded for a recipient
    When the operator looks up that notification by its id
    Then the full notification record is returned
    And searching by that notification's status and creation date returns that notification
