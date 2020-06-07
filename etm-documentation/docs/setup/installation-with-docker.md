# Installation with Docker
Enterprise Telemetry Monitor is also available as a Docker image. This image is based on the openjdk11:alpine-slim image. To retrieve the image run the following command:

```bash
docker pull docker.jecstar.com/etm:4.3.0
```

To run the Docker image Enterprise Telemetry Monitor needs to know where to find Elasticsearch. The most simple way of doing this is to use an environment variable:

```bash
docker run -p 8080:8080 -e "secret=<secret_to_store_sensitive_data>" -e "elasticsearch_connectAddresses=<my-elasticsearch-host>:<my-elasticsearch-port>" -e "elasticsearch_clusterName=elasticsearch" docker.jecstar.com/etm:4.3.0
```

Most of the properties mentioned in the [Node configuration](node-configuration.md) chapter can be passed as environment variables to the Docker container. Properties not mentioned in the [General configuration in etm.yml](node-configuration/general-configuration.md) but is a separate section of the configuration should be prefixed by their section names. For example, the properties *elasticsearch.clusterName* in the configuration file should be passed as environment variable *elasticsearch_clusterName*. In short, the 'dot' in the configuration file should be replaced with an 'underscore' in the environment variable name.

Another option of configuring the Docker image is by mounting a custom configuration file from the host system to the location the image is expecting the configuration file. This can be done by adding

```bash
-v full_path_to/custom_etm.yml:/usr/share/etm/config/etm.yml
```

to the Docker run command. Also, if you need an [Integration with IBM MQ and/or IBM Integration Bus](integration-with-ibm.md) you need to mount the directory that contains the proprietary IIB and/or MQ jar files to the image:

```bash
-v full_path_to/proprietary_jar_files:/usr/share/etm/lib/ext
```

::: warning Important 
The container runs Enterprise Telemetry Monitor as user etm using uid:gid 1000:1000. Bind mounted host directories and files, such as custom_etm.yml above, need to be accessible by this user.
:::