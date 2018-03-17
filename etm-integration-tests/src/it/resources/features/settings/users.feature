Feature: Users

  @RestoreAdminUser
  Scenario Outline: Remove latest admin account
    Given The user logs in to ETM as it-tester with password Welcome123 using <browser>
    And The user browses to /gui/settings/users.html
    Then The users page should be visible
  # Give all users read only access to the user settings.
    When All users are given user admin read settings
    And The user it-tester is selected
    And The User Settings option is set to Read
    Then Saving the user should fail
    And Deleting the user should fail
    When The User Settings option is set to None
    Then Saving the user should fail
    And Deleting the user should fail
    And Restore admin user

    Examples:
      | browser |
      | chrome  |
      | firefox |
    #  | ie      |