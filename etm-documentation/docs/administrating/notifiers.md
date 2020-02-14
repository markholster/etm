# Notifiers
To add, modify or delete notifiers browse to <http://localhost:8080/gui/settings/notifiers.html> or select the menu option *Settings -> Notifiers*. 

A notifier is capable of notifying a user or system of an event from Enterprise Telemetry Monitor. 

You can create three types of notifiers. 
1. The *Business event* type will create a [Business event](../event-layout/README.md#business-event) that will be logged to Enterprise Telemetry Monitor. 
1. The *Email* type will use a SMTP server to send an email. 
1. Finally the *SNMP* type will send an SNMTP trap/notification to your monitoring server.

When you need to store passwords for notifiers note that this password will be base64 encoded into the database. Make sure only Enterprise Telemetry Monitor has access to your database!

When using an SNMPv3 Notifier your SNMP administrator might need an engine id of an Enterprise Telemetry Monitor instance to allow it on the SNMP infrastructure. Enterprise Telemetry Monitor assigns an engine id based on the Private Enterprise Number of Jecstar and the ip address the Enterprise Telemetry Monitor instance is running on. During startup of the Enterprise Telemetry Monitor node a [Business event] will be logged with the assigned engine id. As long as your ip address will be the same the engine id will also be the same. Keep in mind that running multiple Enterprise Telemetry Monitor instances on the same ip will cause all those instances to have the same engine id.
Make sure to add the required root certificates to the [Certificate settings](cluster.md#certificate-settings) when connecting to a notifier endpoint over TLS.