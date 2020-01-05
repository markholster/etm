# Groups
To add, create or modify groups browse to <http://localhost:8080/gui/settings/groups.html> or select the menu option *Settings -> Groups*. 

Groups are a convenient way of managing some user attributes that should be applied to several users. You can, for example, create an administration group that has read and write access to User settings. When a user is added to that group the read and write access to User settings is also applied to that user.

When a user is added to multiple groups he or she has the access roles and Filter queries of all groups combined. Also if any of the groups of an user has the *Always show correlated events* option set to *Yes* the correlated events will be shown in the event detail screen.

Special attention should be paid to the *Dashboards datasources*, *Signal datasources* and *Notifiers* attributes. The value specified for these attributes are valid for the corresponding Visualization and Signal menu options that belong to this group. A value set on this specific group will only be available in the context of that group and in de user context for users that belong to this specific group.

## Import group from ldap
In case you have configured a ldap server in the [Ldap settings](cluster.md#ldap-settings) you can import a group by clicking on the *Import* button. Select the group you want to import and confirm your selection by clicking on the *Import* button. Make sure you assign at least one read and/or write permission to the group otherwise users that are member of that group have no access rights!