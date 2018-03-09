Feature: Transaction overview

  Scenario Outline: Show transaction overview
    Given The system contains events that form chain
    And User logs in to ETM as it-tester with password Welcome123 using <browser>
    And The user browses to /gui/search/index.html
    Then The search page should be visible
  # Search for the given event
    When The user searches for the first event in the chain
    And The event is found
    And The user selects the event
    Then The "Endpoints" tab should be selectable
  # Select the Endpoints tab
    When The user selects the "Endpoints" tab
    Then The endpoint overview should be visible
    And The endpoint overview should contain 3 canvas items
  # Select the application
    When The users selects the application in the overview
    Then The transaction details should be visible
  # Select the Event chain tab
    When The user selects the "Event chain" tab
    Then The event chain should be visible
    And The event chain should contain 3 canvas items

    Examples:
      | browser |
      | chrome  |
      | firefox |
#      | ie      |