# Installation
Enterprise Telemetry Monitor is written in Java and needs a Java 11 or higher runtime installed on your machine. The installation of the Java runtime is not covered in this manual and may differ from platform to platform.
Before you proceed, please check if a correct version of the Java runtime is installed by running:

```bash
java -version
```

If your organization not already has an Elasticsearch cluster installed it is time to install it now. Enterprise Telemetry Monitor needs an Elasticsearch cluster formed of nodes with version 7.0 or compatible. The installation of Elasticsearch is covert at the [Elastic.co\'s website](https://www.elastic.co/guide/en/elasticsearch/reference/7.x/install-elasticsearch.html).
Don\'t forget to read the [important configurations](https://www.elastic.co/guide/en/elasticsearch/reference/7.x/important-settings.html) and [important system configurations](https://www.elastic.co/guide/en/elasticsearch/reference/7.x/system-config.html) pages!
Enterprise Telemetry Monitor does not require a specific Elasticsearch cluster name and can join any Elasticsearch cluster, but out of the box it is trying to connect to a cluster with the name 'elasticsearch'. Enterprise Telemetry Monitor indici and aliases will be prefixed with abbreviation *etm_*.

::: tip Note
Since Elasticsearch 6.3 the default download on the Elasticsearch page is the full featured Elasticsearch image which requires an Elasticsearch license after some time. This is fine if your company owns an Elasticsearch
license. If your not comfortable getting an Elasticsearch subscription the *oss* version is the one to pick. The *oss* version of Elasticsearch can be found [here](https://www.elastic.co/downloads/elasticsearch-oss/).
:::

Once Elasticsearch is up and running you can download and run Enterprise Telemetry Monitor. The binaries can be downloaded from [www.jecstar.com/downloads/](https://www.jecstar.com/downloads/). Depending on your operating system (or personal
preference) you can download a .zip or .tgz file. In this example we choose the .tgz file, so let's download the binary to our machine:

```bash
curl -LO https://www.jecstar.com/downloads/etm-4.3.0.tgz
```

Then extract the archive with the following command:

```bash
tar -xvf etm-4.3.0.tgz
```

This will extract the archive to the directory etm-4.3.0. We then change to the bin directory of Enterprise Telemetry Monitor to fire it up for the first time:

```bash
cd etm-4.3.0/bin
```

You are now ready to start Enterprise Telemetry Monitor with the following command:

```bash
./etm console
```

::: warning Important
By default Enterprise Telemetry Monitor tries to connect to an Elasticsearch cluster with the name *elasticsearch* and a node at 127.0.0.1:9200. If you have changed your Elasticsearch cluster name to something else you should change the Enterprise Telemetry Monitor configuration to match the Elasticsearch configuration. How to change the cluster name can be found in the [Node configuration](../setup/node-configuration.md#elasticsearch-section-in-etm-yml) chapter.
:::

If everything goes well the console will output the following:

```bash
Running Enterprise Telemetry Monitor...
Enterprise Telemetry Monitor started. 
```

As mentioned above, one of the most common mistakes is a mismatch between the Enterprise Telemetry Monitor configuration and the Elasticsearch cluster. If your console prints something like

```
Running Enterprise Telemetry Monitor...
Feb 19, 2019 7:57:11 PM org.elasticsearch.client.sniff.Sniffer run
SEVERE: error while sniffing nodes
java.net.ConnectException: Connection refused
	at org.elasticsearch.client.RestClient$SyncResponseListener.get(RestClient.java:952)
	at org.elasticsearch.client.RestClient.performRequest(RestClient.java:229)
	at org.elasticsearch.client.sniff.ElasticsearchNodesSniffer.sniff(ElasticsearchNodesSniffer.java:104)
	at org.elasticsearch.client.sniff.Sniffer.sniff(Sniffer.java:209)
	at org.elasticsearch.client.sniff.Sniffer$Task.run(Sniffer.java:139)
	at org.elasticsearch.client.sniff.Sniffer$1.run(Sniffer.java:82)
	at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)
	at java.util.concurrent.FutureTask.run(FutureTask.java:266)
	at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.access$201(ScheduledThreadPoolExecutor.java:180)
	at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:293)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	at java.lang.Thread.run(Thread.java:748)
```

your Enterprise Telemetry Monitor configuration cannot find the Elasticsearch cluster. Please contact your Elasticsearch administrator for the correct Elasticsearch connection parameters, and change the setting in the etm.yml file which can be found in the config directory.

When everything is configured well, you can access Enterprise Telemetry Monitor with a browser by browsing to <http://127.0.0.1:8080/gui/>. During installation an administrative account is created with the username *admin* and the password *password*.

::: danger Important
Don't forget to change these default credentials! It is recommended to create a new administrative account and remove the default one as soon as possible.
:::
