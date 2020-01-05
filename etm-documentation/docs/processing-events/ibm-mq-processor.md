# IBM MQ processor
The rest IBM MQ processor is disabled by default. Follow the instructions in the section [IBM MQ section in etm.yml](../setup/node-configuration.md#ibm-mq-section-in-etm-yml) to enable a processor that receives messages from one or more queues or topics. 

Contrary to the [Rest processor](rest-processor.md) the IBM MQ processor can handle different content types. Depending on the value of the option *messagesType* defined in the [Destination options](../setup/node-configuration.md#ibmmq-destination-options) the processing is as follow:

**auto** 
: The IBM MQ processor will try to determine with type of message is received. This is the most flexible option, but also the slowest. If you expect a high load please define a specific format per IBM MQ endpoint.

**iibevent**
: The event must be a xml document formatted as a [IIB Monitoring Event](https://www.ibm.com/support/knowledgecenter/SSMKHH_9.0.0/com.ibm.etools.mft.doc/ac60386_.htm) message.

**etmevent**
: The event must be a json object with the layout described in the [Add a single event](rest-processor.md#add-a-single-event) section. The [buld event layout](rest-processor.md#adding-events-in-bulk) is also supported.

**clone**
: The received MQ message is a clone of the original message. When using this setting the event type will always be *messaging*, and the IBM MQ MQMD header is used to determine several event attributes.