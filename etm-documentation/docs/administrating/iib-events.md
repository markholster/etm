# IIB Events
To add, modify or delete IIB Nodes browse to <http://localhost:8080/gui/iib/events.html> or select the menu option *Settings -> IIB Events*. 

Once your [IIB Nodes](iib-nodes.md) are configured you can enable or disable [IIB Monitoring Events](https://www.ibm.com/support/knowledgecenter/SSMKHH_9.0.0/com.ibm.etools.mft.doc/ac60386_.htm) of your deployed IIB applications and flows. Enabling or disabling monitoring events is as simple as selecting the application or flow and select the preferred monitoring option per node in that flow. Monitoring of the entire application must also be enabled or disabled. Enterprise Telemetry Monitor doesn't support monitoring events on all nodes in a flow, but shows the node types that it is capable of processing. Also, make sure your output terminal of the node you want to monitor has a connection to another node otherwise the monitoring event won't be emitted. 

::: tip Note
Enabling or disabling IIB monitoring events can take some time on your IIB Node. Please be patient while applying your settings. This is not something Enterprise Telemetry Monitor has any influence on.
:::

Enterprise Telemetry Monitor is not changing any monitoring configuration when for example an IIB Node is removed from the configuration. Monitoring events will still be emitted if not disabled before removing the configuration. The same goes for undeploying an IIB application or flow. You also need to make sure the emitted events are picked up by one of the configured processors.