# Node configuration
Each Enterprise Telemetry Monitor Node has its own configuration file. The file can be found at <INSTALL_DIR>/config/etm.yml. When playing around with Enterprise Telemetry Monitor the defaults will be sufficient, but when you configure a production instance you probably need to tune some configuration options. The configuration file is split into 8 main sections: general, elasticsearch, http, ibm mq, jms, kafka, signaler and logging.

Indentation in the etm.yml configuration file is necessary to create nested properties. See the following example for an explanation on how to create lists an key-value mappings.

```yaml
property1: value1 #This is just a general property with the name *property1* and a value of *value1*.
object1: #A new object with the name *object1* is created. An object itself has no direct value, but has (sub)properties with an indentation of 2 spaces.
  sub-property1: value2 #The property *sub-poroperty1* is added to the object *object1*
list1:
- listproperty1: value3 #A new list is created. A list is actually an object as well because it has no direct value bus has (sub)properties with an indentation of 2 spaces. In this case the list doesn't contain single values but objects. Each object starts with a *-*.
  listproperty2: value4
- listproperty1: value5    
  listproperty2: value4
map1:
  key1: value1 #A new map is created. Just like the list, a map is actually an object. In this case the map contains of simple key/value string pairs.
  key2: value2  
```

A detailed specification of the yaml syntax can be found on the [yaml website](http://yaml.org/).

::: danger Important
When storing passwords in the etm.yml file, make sure the file is only readable by the Enterprise Telemetry Monitor administrators and the user/group that runs Enterprise Telemetry Monitor. 
:::

## General configuration in etm.yml
General configuration options have no indentation in the etm.yml file. The following options are available:

**General configuration options**
Name | Default value | Description
--- | --- | ---
bindingAddress | 0.0.0.0 | The interface address to bind Enterprise Telemetry Monitor to.
clusterName | Enterprise Telemetry Monitor|The name of the Enterprise Telemetry Monitor cluster. When running multiple Enterprise Telemetry Monitor clusters it is recommended to give them a separate name.
instanceName | Node_1 | The name of the Node. When running multiple nodes in a cluster, it is recommended to give them a separate name.
secret | <random> | A secret used to encrypt sensitive data in Elasticsearch. When left empty, a secret will be generated the first time Enterprise Monitor is started. The value should always be the same on all nodes running in the same cluster!
elasticsearch | | The elasticsearch configuration. See [Elasticsearch section in etm.yml](#elasticsearch-section-in-etm-yml) to view the nested options.
http | | The http configuration. See [Http section in etm.yml](#http-section-in-etm-yml) to view the nested options.
ibmMq | | The IBM MQ configuration. See [IBM MQ section in etm.yml](#ibm-mq-section-in-etm-yml) to view the nested options.
jms | | The JMS configuration. See [JMS section in etm.yml](#jms-section-in-etm-yml) to view the nested options.
kafka | | The Kafka configuration. See [Kafka section in etm.yml](#kafka-section-in-etm-yml) to view the nested options.
signaler | | The signaler configuration. See [Signaler section in etm.yml](#signaler-section-in-etm-yml) to view the nested options.
logging | | The logging configuration. See [Logging section in etm.yml](#logging-section-in-etm-yml) to view the nested options.

All other configuration sections are identified with the name of the section without indentation. Configuration options in that section have an indentation of 2 spaces.

::: warning Important 
Special care should be given to the secret configuration. You should always keep a copy of the (generated) value in your password manager! Without the value you won't be 
able to decrypt configured passwords that are stored in Elasticsearch. This would render the configuration of the node useless, and eventually might cause data loss.
:::

## Elasticsearch section in etm.yml
The *elasticsearch* section contains all options that are necessary to connect to an Elasticsearch cluster:

**Elasticsearch configuration options**
Name | Default value | Description
--- | --- | ---
clusterName | elasticsearch | The name of the Elasticsearch cluster to connect to.
connectAddresses | 127.0.0.1:9200 | A list of Elasticsearch nodes to connect to. When high availability is a demand of your production environment you should provide at least 2 addresses. The servers must be added in the format ```<servername_or_ip>:<port>```.
waitForConnectionOnStartup | false | Wait for any of the connections supplied in the *connectAddresses* to be established before fully starting Enterprise Telemetry Monitor. This option is useful when Enterprise Telemetry Monitor is started before any of the Elasticsearch nodes is started.
username | | The username used to connect to a secured Elasticsearch cluster.
password | | The password used to connect to a secured Elasticsearch cluster.
sslTrustStoreLocation | | A full path to the jks truststore. Enable this option when you want to make use of a ssl connection to Elasticsearch.
sslTrustStorePassword | | The password of the jks truststore.

## Http section in etm.yml
The *http* section contains all options that are necessary to start the gui and REST processor:

**Http configuration options**
Name | Default value | Description
--- | --- | ---
httpPort | 8080| The port to bind the http listener to. To disable the http listener set the value to zero or lower.
httpsPort | 8443| The port to bind the secure https listener to. The listener will not start unless the sslKeystore is properly configured.
contextRoot | / | The context root under which the gui and REST processor will be available.
ioThreads | 2 | The number of IO threads. IO threads handle all non-blocking calls to the web server. One thread per cpu core should be more than sufficient.
workerThreads | 16 | The number of worker threads to handle all blocking calls to the web server. Around 10 threads per cpu cure should be a good starting point for servers under a high load.
guiEnabled | true | Should the GUI be enabled? Set this value to false if you don't want users to use the gui on this node. The gui is bound to the */gui* context on your server and can be accessed by browsing to ```http://<bindingAddress>:<httpPort>/gui/```
guiMaxConcurrentRequests | 50 | The maximum number of request that can be processed in parallel at any given moment by the GUI. If the number exceeds the maximum, the requests will be queued.
guiMaxQueuedRequests | 50 | The maximum number of requests that can be queued by the GUI. If a request needs to be queued and the maximum number of queued requests exceeds this maximum the request will be rejected.
restProcessorEnabled | true | Should the REST processor be enabled? Set this value to false if you don't want this node to act as a processor that can process events with a REST api. The REST api is bound to the */rest/processor/* context and can be access from ```http://<bindingAddress>:<httpPort>/rest/processor/```
restProcessorMaxConcurrentRequests | 50 | The maximum number of request that can be processed in parallel at any given moment by the REST processor. If the number exceeds the maximum, the requests will be queued.
restProcessorMaxQueuedRequests | 50 | The maximum number of requests that can be queued by the REST processor. If a request needs to be queued and the maximum number of queued requests exceeds this maximum the request will be rejected.
sslProtocol | TLSv1.2 | The ssl protocol that needs to be used on the secure https listener. The allowed values are depending on your Java installation, but unless you have specific demands the default will be sufficient secure.
sslKeystoreLocation | | The location of you ssl keystore. The keystore contains your public/private key pair to identify your server.
sslKeystorePassword | | The password of the ssl keystore.
sslKeystoreType | PKCS12 | The ssl keystore type.
sslTruststoreLocation | | The location of you ssl truststore. The trust store contains certificates of machines that are allowed to connect to this Node. When not provided, everybody is allowed to access this Node although a a username and password are still necessary to login.
sslTruststorePassword | | The password of the ssl truststore.
sslTruststoreType | JSK | The ssl truststore type.
secureCookies | false | Should the secure flag be set on the session cookies? Set this value to true when your Enterprise Telemetry Monitor instance is accessed via https.

## IBM MQ section in etm.yml
The *ibmMq* section contains all options that are necessary to process Enterprise Telemetry Monitor events from a IBM MQ queue or topic. Make sure to add the MQ libraries to the classpath of the Node. See the [Integration with IBM MQ and/or IBM Integration Bus](integration-with-ibm.md) section.

**IBM MQ configuration options**
Name | Default value | Description
--- | --- | ---
Name | Default value | Description
enabled | false | Should the IBM MQ processor be enabled? Set this value to true to process events from defined IBM MQ queue's and/or topics.
queueManagers | | A list of QueueManagers to connect to. See [QueueManager options](#ibmmq-queuemanager-options) to view the nested options.

**<a name="ibmmq-queuemanager-options"></a>QueueManager options**
Name | Default value | Description
--- | --- | ---
name | QMGR | The name of the QueueManager.
host | 127.0.0.1 | The hostname or ip-address the QueueManager is running on.
port | 1414 | The port the QueueManager is listening on.
channel | | The channel to use to setup the connection to the QueueManager.
userId | | The user id used to setup the connection to the QueueManager.
password | | The password used to setup the connection to the QueueManager.
sslCipherSuite | | The ssl cipher suite to use.
sslProtocol | TLSv1.2 | The ssl protocol that needs to be used to connect to the QueueManager. The allowed values are depending on your Java installation, but unless you have specific demands the default will be sufficient secure.
sslKeystoreLocation | | The location of you ssl keystore. The keystore contains your public/private key pair to identify your Node.
sslKeystorePassword | | The password of the ssl keystore.
sslKeystoreType | PKCS12 | The ssl keystore type.
sslTruststoreLocation | | The location of you ssl truststore. The trust store contains certificates of Queuemanager machines that this Node is allowed to connect to. When not provided, all Queuemanager machines are trusted.
sslTruststorePassword | | The password of the ssl truststore.
sslTruststoreType | JSK | The ssl truststore type.
destinations | | A list of destinations to listen on. See [Destination options](#ibmmq-destination-options) to view the nested options.

**<a name="ibmmq-destination-options"></a>Destination options**
Name | Default value | Description
--- | --- | ---
name | | The name of the Queue or Topic to connect to.
type | queue | The destination type. Can be one of *queue* or *topic*.
defaultImportProfile | | The default [import profile](../administrating/import-profiles.md) to use on this destination if no import profile is provided within the processing events.
minNrOfListeners | 1 | The minimum number of listeners to connect to the destination. Not that auto scaling to maxNrOfListeners only works on local queues.
maxNrOfListeners | 5 | The maximum number of listeners to connect to the destination. Not that auto scaling to maxNrOfListeners only works on local queues.
channel | | The channel to use to setup the connection to the QueueManager.
messagesType | auto | Can be one of *auto* which auto detect the message type but is the slowest, *iibevent* which is capable of handling [IIB Monitoring Events](http://www.ibm.com/support/knowledgecenter/SSMKHH_9.0.0/com.ibm.etools.mft.doc/ac60386_.htm), *etmevent* which is capable of handling events in the Enterprise Telemetry Monitor json format or *clone* which assumes the message read is a clone of the original message. See the section [Event layout](../event-layout) for a description of the Enterprise Telemetry Monitor json format.
maxMessageSize | 4194304 | The maximum message size in bytes that can be read. Depending on the get options the message will be ignored or truncated.
commitSize | 500 | The maximum number of messages processed before a MQCMIT is executed.
commitInterval | 10000 | The maximum number of milliseconds the processor can read messages without executing a MQCMIT.
destinationGetOptions | MQGMO_WAIT + MQGMO_SYNCPOINT + MQGMO_ACCEPT_TRUNCATED_MSG + MQGMO_FAIL_IF_QUIESCING + MQGMO_LOGICAL_ORDER + MQGMO_COMPLETE_MSG + MQGMO_ALL_SEGMENTS_AVAILABLE | The MQ Get options.
destinationOpenOptions | MQOO_INQUIRE + MQOO_FAIL_IF_QUIESCING + MQOO_INPUT_SHARED | The MQ Open options.

## JMS section in etm.yml
The *jms* section contains all options that are necessary to process Enterprise Telemetry Monitor events from a JMS queue or topic. Make sure to add the required JMS libraries to the classpath of the Node. See the [Integration with JMS](../integration-with-jms.md) section for more information.

**JMS configuration options**
Name | Default value | Description
--- | --- | ---
enabled | false | Should the JMS processor be enabled? Set this value to true to process events from defined JMS queue's.
connectionFactories | | A list of connection factories to connect to. See [JMS Connection Factory instantiation](#connection-factory-options) for the nested options.

<a name="connection-factory-options"></a>
Enterprise Telemetry Monitor supports 2 types of JMS Connection Factory instantiation. Each type has its own tag in the yaml configuration file.

When the Connection Factory must be looked up into a JNDI registry the tag *!jndiConnectionFactory* should be used. The JNDI connection factory lookup supports the following options:

**JNDI Connection factory options**
Name | Default value | Description
--- | --- | ---
initialContextFactory | | The full classname of the Initial Context Factory.
providerURL | | The url used to connect to the Intial Context Factory.
jndiName | | The name of the Connection Factory in the JNDI.
parameters | | A map with parameters that will be used to connect to the Iitial Context Factory.
destinations | | A list of destinations to listen on. See [Destination options](#jms-destination-options) to view the nested options.

When the Connection Factory does not resides in a JNDI registry but should be instantiated directly the tag *!nativeConnectionFactory* should be used. The native connection factory instantiation supports the following options:

**Native Connection factory options**
Name | Default value | Description
--- | --- | ---
className | | The full classname of the Connection Factory.
constructorParameters | | A list with parameters that will be passes to the constructor while instantiating the class.
parameters | | A map with parameters that will be set on the Connection Factory instance.
destinations | | A list of destinations to listen on. See [Destination options](#jms-destination-options) to view the nested options.

**<a name="jms-destination-options"></a>Destination options**
Name | Default value | Description
--- | --- | ---
name | | The name of the Queue or Topic to connect to.
type | queue | The destination type. *queue* is the only supported option.
minNrOfListeners | 1 | The minimum number of listeners to connect to the destination.
maxNrOfListeners | 5 | The maximum number of listeners to connect to the destination.
messagesType | auto | Can be one of *auto* which auto detect the message type but is the slowest, *etmevent* which is capable of handling events in the Enterprise Telemetry Monitor json format or *clone* which assumes the message read is a clone of the original message. See the section [Event layout](../event-layout) for a description of the Enterprise Telemetry Monitor json format.
defaultImportProfile | | The default [import profile](../administrating/import-profiles.md) to use on this destination if no import profile is provided within the processing events.

## Kafka section in etm.yml
The *kafka* section contains all options that are necessary to process Enterprise Telemetry Monitor events from a [Kafka](https://kafka.apache.org/) topic.

**Kafka configuration options**
Name | Default value | Description
--- | --- | ---
enabled | false | Should the Kafka processor be enabled? Set this value to true to process events from defined Kafka topics.
topics | | A list of topics to read from. See [Topic options](#kafka-topic-options) to view the nested options.

**<a href="kafka-topic-options"></a>Topic options**
Name | Default value | Description
--- | --- | ---
name | | The name of the topic to connect to.
bootstrapServers | | A list of bootstrap servers to connect to. The servers must be added in the format ```<servername_or_ip>:<port>```.
nrOfListeners | 1 | The number of listeners to connect to read from the topic. This number should never be higher that the number of partitions in your topic.
groupId | Enterprise Telemetry Monitor | The name of the group id to connect to the topic. All Enterprise Telemetry Monitor Nodes should have the same group id.
startFrom | | Set to *beginning* to start processing from the begin of the topic instead of the saved offset.
defaultImportProfile | | The default [import profile](../administrating/import-profiles.md) to use on this destination if no import profile is provided within the processing events.
maxPollRecords | | The maximum number of records to retrieve in a single call to the topic.
maxPollInterval | | The maximum number of milliseconds allowed between two retrieval calls to the topic. If the processing took longer the Node will be considered failed and the topic will be rebalanced.
sessionTimeout | | The timeout used to detect consumer failures. If this Node will not send a heartbeat within this interval the Node will be considered failed and the topic will be rebalanced.
heartbeatInterval | | The expected time between heartbeats. This value should always be lower as the sessionTimeout value.
sslCipherSuite | | The ssl cipher suite to use.
sslProtocols | TLSv1.2 | The ssl protocols that needs to be to used to connect to the kafka servers. The allowed values are depending on your Java installation, but unless you have specific demands the default will be sufficient secure.
sslKeystoreLocation | | The location of you ssl keystore. The keystore contains your public/private key pair to identify your Node.
sslKeystorePassword | | The password of the ssl keystore.
sslKeystoreType | PKCS12 | The ssl keystore type.
sslTruststoreLocation | | The location of you ssl truststore. The trust store contains certificates of Kafka machines that this Node is allowed to connect to.
sslTruststorePassword | | The password of the ssl truststore.
sslTruststoreType | JSK | The ssl truststore type.

## Signaler section in etm.yml
The *signaler* section contains all options to configure the signaler.

**Signaler configuration options**
Name | Default value | Description
--- | --- | ---
enabled | true | Should the signaler be enabled? Set this value to true to let this node send signals to end users/systems.

## Logging section in etm.yml
The *logging* section contains all options to configure the loggers and log levels. Log levels can be one of TRACE, DEBUG, INFO, WARNING or ERROR.

**Logging configuration options**
Name | Default value | Description
--- | --- | ---
rootLogger | INFO | The root logging level. If no specific logger is configured, this value will be used.
loggers | | A map with string key/value pairs. The key is the name of the logger and the value is the log level to be used for that specific logger.
