# Error codes
During execution Enterprise Telemetry Monitor can run into an error. These may be shown to the end user, or are visible in the logs. Most of the time an error code is provided with the error message. This section explains these error codes and provides a reason and solution.

Code | Reason | Solution
--- | --- | ---
100000 | General error. This error code is used when an error occurred in code that Enterprise Telemetry Monitor depends on. | 
100001 | Unauthorized. You are not authorized for this action. |
100002 | Data communication error. Enterprise Telemetry Monitor is unable to communicate with Elasticsearch. |
200000 | Invalid license key. | You have provided an invalid license key. Please make sure your license key is exactly copied as provided by Jecstar Innovation.
200001 | License expired. | Your license has expired and needs to be renewed.
200002 | License not yet valid. | Your license is not yet valid.
200003 | License storage size exceeded. | The maximum database size of your license is exceeded. Upgrade your license or remove some indices.
201000 | Configuration load error. | Please check the connection to your Elasticsearch cluster.
201010 | Error creating XML unmarshaller. |
201011 | Invalid xpath expression. The provided input is an invalid XPath expression. | Change your input to a valid XPath expression.
201012 | Invalid xslt template. The provided input is an invalid XSLT template. | Change your input to a valid XSLT template.
201013 | Invalid json (path) expression. The provided input is an invalid JSON (path) expression or is not definite. | Change your input to a valid definite JSON (path) expression.
201014 | Invalid regular expression. The provided input is an invalid regular expression or is not definite. | Change your input to a valid regular expression.
201015 | Invalid javascript expression. The provided input is invalid javascript. | Correct the script to be a valid ECMAScript.
201029 | Invalid expression parser type. |
201030 | No user admins left. You are trying to change a user that will result in a system without any users with [Read & write access to the User settings](../administrating/users.md#user-roles). | Make sure you will have an user with Read & write access to the User settings left when the change is applied.
201031 | Invalid or unknown LDAP user. | Check your LDAP connection or the state of your LDAP server.
201032 | Invalid or unknown LDAP group. | Check your LDAP connection or the state of your LDAP server.
201033 | The api key or secondary api key is not unique. | Rotate your api keys.
202001 | Invalid password. | Retype your password and try again.
202002 | The new password may not be the same as the old password. | Choose a new password that is not the same as the current password.
300000 | Unable to connect to IIB node. | Check your connection parameters and make sure the IIB Node is running.
300001 | Unknown IIB object. |
400000 | Maximum number of graphs reached. | You are not allowed to store more graphs. Delete a graph, or ask your Enterprise Telemetry Monitor administrator to raise the maximum number of graphs that can be stored.
400001 | Maximum number of dashboards reached. | You are not allowed to store more dashboards. Delete a dashboard, or ask your Enterprise Telemetry Monitor administrator to raise the maximum number of dashboards that can be stored.
400002 | Not authorized for dashboard data source. | 
500000 | Maximum number of signals reached. | You are not allowed to store more signals. Delete a signal, or ask your Enterprise Telemetry Monitor administrator to raise the maximum number of signals that can be stored.
500001 | Not authorized for signal data source. | 
500002 | Not authorized for notifier. | 
600000 | Maximum number of search templates reached. | You are not allowed to store more search templates. Delete a search template, or ask your Enterprise Telemetry Monitor administrator to raise the maximum number of search templates that can be stored.
600001 | Maximum number of events in Directed Graph reached. | The transactions you want to view are build up by more that 2048 events. To prevent Enterprise Telemetry Monitor to run out of memory this isn't allowed. This error indicates a problem in your logging regarding the event_id, correlation_id or transaction_id. 