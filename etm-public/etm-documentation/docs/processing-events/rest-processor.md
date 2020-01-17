# Rest processor
The rest processor is enabled by default. To check if the processor is configured make sure the option *restProcessorEnabled* is set to *true* in the [Http section in etm.yml](../setup/node-configuration.md#http-section-in-etm-yml).
All requests made to the rest processor should contain an http header with the name `apikey`. The value of the `apikey` header must be the *Api key* or *Secondary api key* of a user with write access to Events as configured in the [User roles](../administrating/users.md#user-roles).
To prevent downtime when rotating an api key, it is also possible to pass both api keys separated by a comma in the `apikey` header. This way you can safely rotate a single api key when it's compromised without having to change the client code immediately. The rest processor will check if any of the 2 given api keys belongs to a user.

## Add a single event
To process a single event post some data to http://localhost:8080/rest/processor/event/. The data needs to be in JSON format and should contain a single json object with 2 keys: *type* and *data*. Type should be one of the predefined event types for example *log*, and data should contain the event data as described in the [Event layout](../event-layout) section.

The following curl example will add a log event to Enterprise Telemetry Monitor:

```bash
curl -H 'Content-Type: application/json' -H 'apikey: <user api key>,<user secondary api key>' -XPOST 'http://localhost:8080/rest/processor/event/' -d'
{
    "type" : "log",
    "data" : {
    	"payload" : "My first Enterprise Telemetry Monitor event!"
    }
}'
```

As a response you will receive the following message

```json
{ "status": "acknowledged" }
```
telling you the event is successfully received and will be available in Enterprise Telemetry Monitor from now on.

Optionally you can set the [Import profiles](../administrating/import-profiles.md) that should be used to enhance the event:
```bash
curl -H 'Content-Type: application/json' -H 'apikey: <user api key>,<user secondary api key>' -XPOST 'http://localhost:8080/rest/processor/event/' -d'
{
    "type" : "log",
    "import_profile": "my-import-profile",
    "data" : {
    	"payload" : "An Enterprise Telemetry Monitor event enhanced by my-import-profile"
    }
}'
```

## Adding events in bulk
Although adding a single event takes only a few milliseconds there will be some overhead of setting up a connection. When you expect a high load it might be useful to add events in bulk. This can be done by posting the to http://127.0.0.1:8080/rest/processor/event/_bulk. The data send to this endpoint needs to be a json array of objects with the same layout as the object explained in the section [Add a single event](#add-a-single-event). 

For example:

```bash
curl -H 'Content-Type: application/json' -H 'apikey: <user api key>,<user secondary api key>' -XPOST 'http://localhost:8080/rest/processor/event/_bulk' -d'
[ 
  { "type" : "log", "data" : { "payload" : "My first bulk event!" }},
  { "type" : "log", "import_profile": "my-profile", "data" : { "payload" : "My second bulk event!" }}
]'
```

will add 2 events in a single POST action. Again you will receive 

```json
{ "status": "acknowledged" }
```

to let you know the events are successfully received.