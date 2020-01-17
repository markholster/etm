# Event layout
The heart of Enterprise Telemetry Monitor is based on event data. The event structure is flexible but has a common base for some predefined event types. Event types are fixed and cannot be created. The content of these types however can be extended with custom information. This information can be used to query or monitor your events.

## Common event data
All event types have a common set of attributes. This section describes these attributes.

**Common event attributes**
Name | Default value | Description
--- | --- | ---
id | | The unique id of the event. When two or more events with the same id are processed they are mostly merged, but some attributes will be overwritten. Make sure your events are only reusing event id's when they functionally are the same. Be aware that processing event with an id that is processed about 1000 times before will cause major performance degradation. To prevent an unrecoverable state Enterprise Telemetry Monitor will automatically blacklist id's that are used too much for an hour.
correlation_id | | The id of the event this event is correlated to. Use this attribute for request/reply correlation. Also make sure you don't correlate more that 1000 events to a single event otherwise you will experience a major performance degradation. To prevent an unrecoverable state Enterprise Telemetry Monitor will automatically blacklist correlation id's that are used too much for an hour. If a request is correlated by more than 10 other events only the first 10 events will be shown in the event detail screen.
correlation_data | Empty map | A map that can be filled by any of the configured [Parsers](../administrating/parsers.md). The field has no processing functionality whatsoever.
endpoints | | The endpoints this event has passed. See [Endpoint attributes](#event-endpoint-attributes) to view the nested attributes.
extracted_data |Empty map | A map that can be filled by any of the configured [Parsers](../administrating/parsers.md). The field has no processing functionality whatsoever.
metadata | Empty map | A map that can be filled by any of the configured [Parsers](../administrating/parsers.md). The field has no processing functionality whatsoever.
name | | The name of the event.
payload | | The payload of the event.
payload_encoding | | A write only attribute that needs to be filled when the payload isn't plain text. Supported values are *base64* and *base64CaApiGateway*.
payload_format | | The payload format. Can be one of HTML, JSON, SOAP, SQL, TEXT or XML.
payload_length | | A calculated read only attributes that hold the length of the payload content. 
timestamp | | A calculated read only attributes that hold the date/time the event got first processed. 

**<a name="event-endpoint-attributes"></a>Endpoint attributes**
Name | Default value | Description
--- | --- | ---
name | | The name of the endpoint this event is logged on.
endpoint_handlers | Empty list | A list with endpoint handlers that have read or written the event. See [Endpoint handler attributes](#event-endpoint-handler-attributes) to view the nested attributes.

**<a name="event-endpoint-handler-attributes"></a>Endpoint handler attributes**
Name | Default value | Description
--- | --- | ---
application | | The application that handles the event. See [Application attributes](#event-application-attributes) to view the nested attributes.
handling_time | Current date/time if this is the only handler for the event. | The date/time the event was handled by the endpoint handler.
location | | The physical location the event was handled. See [Location attributes](#event-location-attributes) to view the nested attributes.
metadata | Empty map | A map that can be filled by any of the configured [Parsers](../administrating/parsers.md). The field has no processing functionality whatsoever.
sequence_number | | The sequence number of the event within the given transaction. When events occur at exactly the same time this attribute is used to determine the event order within a transaction.
transaction_id | | The transaction id under which this event is handled. This is the main attribute to correlate events besides the correlation id. For example, if an application receives a requests then transforms it and passes the request through to another system, then the received and send request can be correlated by providing the same transaction id. Make sure you use an unique transaction id for every transaction you want to be correlated. When using [Integration with IBM MQ and/or IBM Integration Bus] this will be automatically done. Another option is to provide multiple [Log event]s and a [Http event] with the same transaction id. This way you can view every log line that belongs to a single http request.
type | | *WRITER* when the endpoint handler has written to the endpoint or *READER* when the endpoint handler had read from the endpoint.
latency | | A calculated read only attribute that contains the time in milliseconds between the handlingTime of the writing endpoint handler and a reading endpoint handler.
response_time | | A calculated read only attribute that contains the time in milliseconds between the handling time of writing a request event and reading the correlated response event.

**<a name="event-application-attributes"></a>Application attributes**
Name | Default value | Description
--- | --- | ---
name | | The name of the application.
host_address | | The host address the application is running on. This can be a hostname or ip address.
instance | | The instance name of the application. Useful if your application is clustered and has multiple instances.
principal | | The user or system account that has generated or caused the event to be emitted.
version | | The version of the application.

**<a name="event-location-attributes"></a>Location attributes**
Name | Default value | Description
--- | --- | ---
latitude | | The latitude.
longitude | | The longitude.

## Business event
The business event can be used when you want to log a certain event that has happened during one of your business processes. For example, if your business is selling books you can create a business event every time you sell a book. If you provide the book name, price, location etc in xml as payload you can add parsers to extract this data to the extractedData map and generate statistics over these extracted fields. By providing the information that is important to you, you can generate statistics on anything you want! And more important, these statistics are near real-time. 

The business event doesn't contain any specific attributes. Only the [Common event data](#common-event-data) can be provided.

## Http event
The http event can be used when an application sends or receives a http request or response. Besides the [Common event data](#common-event-data) attributes the http event has the following attributes: 

**Http event attributes**
Name | Default value | Description
--- | --- | ---
http_type||The http event type. Can be one of CONNECT, DELETE, GET, HEAD, OPTIONS, POST, PUT, TRACE or RESPONSE.
expiry||The moment the event expires.
status_code||The http status code used in the response of a request.

## Log event
The log event can be used when an application wants to log something. Enterprise Telemetry Monitor can be positioned as a central logging system to provide fine grained access to all of your logs. Besides the [Common event data](#common-event-data) attributes the log event has the following attributes:  

**Log event attributes**
Name | Default value | Description
--- | --- | ---
log_level | | The log level, for example DEBUG or ERROR.
stack_trace | | A stack trace that belongs to the payload that is logged.

## Messaging event
The messaging event can be used when an application sends or receives a message over a messaging system such as IBM MQ or ActiveMQ. Besides the [Common event data](#common-event-data) attributes the messaging event has the following attributes: 

**Messaging event attributes**
Name | Default value | Description
--- | --- | ---
expiry | | The moment the event expires.
messaging_type | | The messaging type, can be one of REQUEST, RESPONSE or FIRE_FORGET.

## SQL event
The SQL event can be used when an application sends or receives a SQL query to a database. Besides the [Common event data](#common-event-data) attributes the SQL event has the following attributes: 

**SQL event attributes**
Name | Default value | Description
--- | --- | ---
sql_type | | The SQL type, can be one of DELETE, INSERT, SELECT, UPDATE or RESULTSET.