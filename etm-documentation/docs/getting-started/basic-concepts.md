# Basic concepts

Before we can go any further it is important to explain the basis concepts of Enterprise Telemetry Monitor.

**Cluster**
A Cluster is a group of Enterprise Telemetry Monitor Nodes forming a distributed application without a single point of failure. Technically speaking a Cluster can be formed with a single Node but this is not a recommended setup.

**Node**
A Node is an instance of an Enterprise Telemetry Monitor installation. Each node can provide all of the functionalities that Enterprise Telemetry Monitor provides.

**Event**
An Event is the most basic entity in Enterprise Telemetry Monitor. An Event can be an http request, but also a log line from an application. 

**Processor**
The Processor is the component that handles the processing of all events. It is capable of enriching and grouping event based on your configuration.

**Transaction**
An transaction is a group of Events that belong together. The scope of a Transaction is configurable, and highly dependent on how the Event is handed over to the Processor.

**Gui**
The Graphical User Interface of Enterprise Telemetry Monitor. You can access Enterprise Telemetry Monitor with (almost) any modern browser on any Node. With the Gui your are able to monitor, search and correlate all of the processed events.

**Elasticsearch**
The database that is used by Enterprise Telemetry Monitor. This database is not provided with the product, but needs to be setup separately if not already available in your organization. More information can be found on the [Elasticsearch website](https://www.elastic.co/products/elasticsearch).

In short a simple setup could be something like the image below; Application 1 and IBM Integration Bus provide Event to the Processor. The processor is enhancing and saving the Events to the Elasticsearch database. Users are able to query and monitoring events with a browsers on any device.