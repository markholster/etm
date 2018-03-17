Feature: Login

  Scenario Outline: Login to ETM
    Given The user logs in to ETM as it-tester with password Welcome123 using <browser>
    And The user browses to /gui/
    Then The login page should be visible

    Examples:
      | browser |
      | chrome  |
      | firefox |
      | ie      |