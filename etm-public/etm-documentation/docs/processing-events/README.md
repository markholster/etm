# Processing events
Enterprise Telemetry Monitor needs to be feeded with events to do its magic. This can be done in several ways:

* Sending events to the REST processor
* Sending events to an IBM message queue or topic that is read by the IBM MQ processor.
* Sending events to a JMS compatible messaging infrastructure.
* Sending events to a Kafka messaging infrastructure.

Depending on your current infrastructure and expected load you can pick any of these options. When guaranteed delivery and processing of your events is a requirement than sending the event to a message queue is the best option for you. On the other hand the rest processor doesn't need any changes to your infrastructure but if the processor is down for maintenance no events will be processed. The applications sending events will likely drop the events that can't be delivered.

Performance wise there is no advantage in picking one specific processor. Enterprise Telemetry Monitor is capable of processing ~50.000 log events per second on a single instance. Of course the more information you provide in your event, the more time it will take to process the event. Also memory and disk speed can influence the throughput of events in a relatively high way.