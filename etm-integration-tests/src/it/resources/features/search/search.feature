Feature: Search

  Scenario Outline: Search for etm events
    Given The user logs in to ETM as it-tester with password Welcome123 using <browser>
    And The user browses to /gui/search/index.html
    Then The search page should be visible
  # Test the presence of the search result table and the query in the query history
    When The user searches for endpoints.endpoint_handlers.application.name: "Enterprise Telemetry Monitor" of types Log
    Then The search result table should be visible and contain 2 columns
    And The search history should contain endpoints.endpoint_handlers.application.name: "Enterprise Telemetry Monitor"
  # Test sorting the result table
    When The user sorts the search results by Name
    Then The result table should be sorted by Name
  # Test adding a column
    When The result table edit link is clicked
    Then The "Table settings" modal should be shown
    When The user adds a row with the name "Log level" on the field "log_level"
    Then The search result table should be visible and contain 3 columns

    Examples:
      | browser |
      | chrome  |
      | firefox |
      | ie      |