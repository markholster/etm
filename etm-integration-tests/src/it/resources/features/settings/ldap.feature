Feature: Ldap

  @LdapServer
  Scenario Outline: Ldap configuration
    Given The user logs in to ETM as it-tester with password Welcome123 using <browser>
    And The user browses to /gui/settings/cluster.html
    Then The cluster settings page should be visible
  # Select the Ldap tab
    When The user selects the Ldap tab
    And The current Ldap configuration is empty
    And The embedded Ldap server is entered
    And The user browses to /gui/settings/users.html
    Then The import buttons should be enabled
  # Import the user
    When The user with id "etm-admin" is removed
    And The user with id "etm-admin" is imported from Ldap
    Then The user with id "etm-admin" should be available
  # Import the group
    When The user browses to /gui/settings/groups.html
    And The group with id "cn=etm-admin-group,ou=groups,dc=jecstar,dc=com" is removed
    And The group with id "cn=etm-admin-group,ou=groups,dc=jecstar,dc=com" is imported from Ldap
    Then The group with id "cn=etm-admin-group,ou=groups,dc=jecstar,dc=com" should be available
  # Assign role to group
    When The group with id "cn=etm-admin-group,ou=groups,dc=jecstar,dc=com" is assigned the etm_event_read role
    And The user logs out
    And The user logs in to ETM as etm-admin with password password using <browser>
    And The user browses to /gui/search/index.html
    Then The search page should be visible

    Examples:
      | browser |
      | chrome  |
      | firefox |
    #  | ie      |