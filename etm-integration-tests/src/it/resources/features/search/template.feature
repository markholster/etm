Feature: Search templates

  Scenario Outline: Create a template
    Given The user logs in to ETM as it-tester with password Welcome123 using <browser>
    And The user browses to /gui/search/index.html
    And All templates are removed
    Then The search page should be visible
  # Check if certain fields are disabled.
    And The element with id "btn-search" is disabled
    And The element with id "template-name" is disabled
    And The element with id "btn-save-template" is disabled
    When The user enters the text "This is a test" in the query field
    Then The element with id "btn-search" is enabled
    And The element with id "template-name" is enabled
    And The element with id "btn-save-template" is disabled
  # We've entered a query now try to save it
    When The user enters the text "Integration-test" in the template name field
    Then The element with id "btn-save-template" is enabled
    When The user saves the template
    Then A template with the name "Integration-test" should be present
  # Now test if the template works
    When The user enters the text "This value should be changed with the value of the template" in the query field
    And The user applies the template "Integration-test"
    Then The query field should contain the value "This is a test"
  # Test updating the template
    When The user enters the text "This is an updated test" in the query field
    And The user enters the text "Integration-test" in the template name field
    And The user saves the template
    Then The "Template already exists" modal should be shown
    When The user confirms the modal with "Yes"
    Then A template with the name "Integration-test" should be present
    # Now test if the updated template works
    When The user enters the text "This value should be changed with the value of the template" in the query field
    And The user applies the template "Integration-test"
    Then The query field should contain the value "This is an updated test"

    Examples:
      | browser |
      | chrome  |
      | firefox |
      | ie      |