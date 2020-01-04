### Integration with IBM MQ and/or IBM Integration Bus
Enterprise Telemetry Monitor is capable of providing deep integration with IBM MQ and or IBM Integration Bus. To make use of any of these integrations the classpath of Enterprise Telemetry Monitor needs to be extended. Due to the MQ license Jecstar is not allowed to provide this specific library with Enterprise Telemetry Monitor. 

To make use of the IBM MQ integration you need to copy a file named *com.ibm.mq.allclient.jar* from your MQ installation to the <INSTALL_DIR>/lib/ext directory. This integration makes it possible to process events from any IBM MQ Destination. For further configuration see the [IBM MQ section in etm.yml](node-configuration.md#ibm-mq-section-in-etm-yml).

The IBM Integration Bus integration makes it possible to manage the emission of [IIB Monitoring Events](https://www.ibm.com/support/knowledgecenter/SSMKHH_9.0.0/com.ibm.etools.mft.doc/ac60386_.htm) from within Enterprise Telemetry Monitor.
Depending on your IIB version you need to copy some files to the <INSTALL_DIR>/lib/ext directory of every Enterprise Telemetry Monitor node running. Consult the table below to determine which files are necessary for your setup.

**Dependencies based on IIB version**
Filename | IIB 9 | IIB 10
--- | --- | ---
configmanagerproxy.jar | :white_check_mark: | :x:
ibmjsseprovider2.jar | :white_check_mark: | :white_check_mark:
integrationapi.jar | :x: | :white_check_mark:
jetty-io.jar | :x: | :white_check_mark:
jetty-util.jar | :x: | :white_check_mark:
websocket-api.jar | :x: | :white_check_mark:
websocket-client.jar | :x: | :white_check_mark:
websocket-common.jar | :x: | :white_check_mark:
