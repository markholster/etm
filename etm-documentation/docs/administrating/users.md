# Users
To add, create or modify users browse to <http://localhost:8080/gui/settings/users.html> or select the menu option *Settings -> Users*. 

The top drop-down field allows you to select a user to modify, delete or copy. It is highly recommended to remove the default administrator user for security reasons! You should at least change the password of the default administrator user.

Each user can have the following attributes:

**User attributes**
Name | Description
--- | ---
Id | The unique id of the user. This id is also used for logging in to the Enterprise Telemetry Monitor Gui.
Name | The (full) name of the user.
Email address | The email address of the user.
Api key | The api key that may be added as http header when making calls to the [Rest processor](../processing-events/rest-processor.md).
Secondary api key | The secondary api key that may be added as http header when making calls to the [Rest processor](../processing-events/rest-processor.md).
Groups | The groups the user belongs to. This field will only be available if at least one group is created or when a ldap connection is setup. When the user is synchronized with a ldap server only the membership of imported ldap groups are shown. See [Groups](groups.md) for more information.
Filter query | The filter query that needs to be applied to the user. See [Filter query](#filter-query) for more information.
Filter query occurrence | The occurrence of the filter query. See [Filter query](#filter-query) for more information.
Always show correlated events | Always show directly correlated events in the event detail screen even when they normally would be filtered by a [Filter query](#filter-query). Note that this does not mean that those correlated events will be shown in the [Search results widget](../searching/search-result-widget.md). This feature can, for example, be useful if you apply a filter that shows only request, but you want to be able to view the correlated responses in the event detail screen as well.   
Locale | The locale of the user.
Time zone | The time zone the user is in. Time based properties will be converted to the selected time zone.
Search history size | The number of queries that need to be remembered.
Default search time range | The default time range (in seconds) on the search screen to query. For example, if you set this value to 60 the last 60 seconds will be queried by default when entering the search screen.
Dashboards datasources | The datasources available to the user when creating a graph/dashboard. Note that when an authorization to a datasource is revoked and the user has stored a graph that uses the datasource the graph will no longer ouput data and renders into an error state.
Signal datasources | The datasources available to the user when creating a signal. Note that when an authorization to a datasource is revoked and the user has stored a signal that uses the datasource the signal will no longer be checked for threshold exceedances.
Notifiers | The [Notifiers](../administrating/notifiers.md) available to the user. A notifier can be used for sending alerts configured in [Signals](../signals/README.md).
Access control | The roles the user should have. See [User roles](#user-roles) for a detailed description.
Change password on logon | The user is forced to change the password on the next logon, or in the current session if the user is already logged on.
New password | The new password for the user. This field is mandatory when adding a new user.
Retype password | Retype the new password in this field to make sure you didn't enter a typo in the *New password* field.

## User roles
Each user can have one or more roles. Depending on the roles the user has, he or she can or can't access some parts of the Enterprise Telemetry Monitor Gui. For most roles on of the
values 'None', 'Read', 'Write' or 'Read & write' can be assigned. Depending on your selection it will give the user or group no access or a combination of read
and write access to a certain resource.

The following resources are available:
Name | Access to
--- | ---
Audit logs | Menu option *Settings -> Audit logs*
Cluster settings | Menu option *Settings -> Cluster*
Events | Menu option *Search* for read access and the [Rest processor](../processing-events/rest-processor.md) for write access.
Group dashboards | Menu option *Visualizations -> &lt;groupname&gt; -> &#42;*
Group settings | Menu option *Settings -> Groups*
Group signals | Menu option *Signals -> &lt;groupname&gt;*
IIB events | Menu option *Settings -> IIB events*
IIB nodes | Menu option *Settings -> IIB nodes*
Import profiles | Menu option *Settings -> Import profiles*
Index statistics | Menu option *Settings -> Index statistics*
License | Menu option *Settings -> License*
Node settings | Menu option *Settings -> Nodes*
Notifiers | Menu option *Settings -> Notifiers*
Parser settings | Menu option *Settings -> Parsers*
User dashboards | Menu option *Visualizations -> &lt;username&gt; -> &#42;*
User settings | Menu option *Settings -> Users*
User signals | Menu option *Signals -> &lt;username&gt;*

## Filter query
Imagine you have stored a gazillion events in Enterprise Telemetry Monitor. Some of them might contain credentials or other sensitive data and you don't want this data to be visible to everyone with read access to Events. This is were the Filter query is your best friend. When a Filter query is applied to a user (or group) it is attached to every query the user executes. For example, when we configure

```coffeescript
endpoints.endpoint_handlers.application.name: "Enterprise Telemetry Monitor"
```

as a Filter query for user Bob, every time Bob enters a query the query will be extended with this filter query. If Bob searches for

```coffeescript
name: BobsEventName
```

under the hood Enterprise Telemetry Monitor will query for:

```coffeescript
name: BobsEventName AND endpoints.endpoint_handlers.application.name: "Enterprise Telemetry Monitor"
```

This way we can prevent Bob of seeing any events that are not generated by the application "Enterprise Telemetry Monitor".
The above example assumed the value *Must* was selected in the Filter query occurrence options field. If the value is changed to *Must not* Enterprise Telemetry Monitor would have executed the following query:

```coffeescript
name: BobsEventName AND NOT endpoints.endpoint_handlers.application.name: "Enterprise Telemetry Monitor"
```

In this case Bob can see all events, but not the ones generated by the application "Enterprise Telemetry Monitor". When combined with [Groups](groups.md) a very flexible and powerful set of access rules can be applied.

:::warning Note
[Joins](../searching/query-syntax.md#joins) are not supported in filter queries.
:::

## Import user from ldap
In case you have configured a ldap server in the [Ldap settings](cluster.md#ldap-settings) you can import a user by clicking on the *Import* button. Enter the id of the user you want to import and confirm your input by clicking on the *Import* button. You don't need to import all users from your ldap server. When a user successfully logs in into Enterprise Telemetry Monitor by providing his/her ldap credentials the account will automatically be synchronized. Though you have to make sure at least one ldap group the user belongs to is imported into Enterprise Telemetry Monitor.