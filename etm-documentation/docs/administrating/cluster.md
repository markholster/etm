# Cluster
To manage your cluster settings browse to <http://localhost:8080/gui/settings/cluster.html> or select the menu option *Settings -> Cluster*. 

On the cluster settings page you can adjust several settings that help you keep your cluster performing the way you want. By default most of the settings should be fine, but if you want to make optimal use of each CPU cycle your cluster has to offer you have the option to tune it on this page.

When changing any of these properties no restart is required. All settings will be automatically applied within 60 seconds. For some settings a hot-restart of the processor is required. This may cause a little latency peek in the Processor.

The cluster page has grouped related configuration items into several tabs:

## General settings
Name | Description
--- | ---
Http session timeout | The maximum idle time in milliseconds for an http session before it will be cleaned up. All http sessions are stored in Elasticsearch to provide maximum flexibility in starting and stopping different nodes. An http session will always be available on all nodes to provide High Availablilty out of the box. 
Import profile cache size | The cache size for [Import profiles](import-profiles.md). This is the configuration for parsers and enhancers. Set to zero to disable the cache. Be aware that setting the cache size to a large value may consume a lot of memory!
Search export max rows | The maximum number of rows that can be exported from the search page. If you set this value to high it might lead to a very high memory consumption of your Enterprise Telemetry Monitor Node.
Max search templates | The maximum number of search templates a user may store.
Max search history size | The maximum number of queries that are kept in the users query history. An individual user may configure a lower number for him/herself.
Max graphs | The maximum number of graphs that can be stored by a single user or group.
Max dashboards | The maximum number of dashboards that can be stored by a single user or group.
Max signals | The maximum number of signals that can be stored by a single user or group.

## Elasticsearch settings 
Name | Description
--- | ---
Shards per index | Each day at 00:00 UTC a new Elasticsearch index is created. This option sets the number of [shards](https://www.elastic.co/guide/en/elasticsearch/reference/7.x/scalability.html) in each new index.
Replicas per index | The number of [replica's](https://www.elastic.co/guide/en/elasticsearch/reference/7.x/scalability.html) each Elasticsearch index should have. Leave this value to zero if you have only one Elasticsearch instance in your cluster.
Max event indices | The number of event indices to keep. Each day at 00:00 UTC a new Elasticsearch index is created. This means that setting this value to 10 will keep your events at least 9 days, depending on your local time zone.
Max metrics indices | The number of metrics indices to keep. Each Enterprise Telemetry Monitor node generates metrics to the metrics index of the current day. This index cannot be queried, but is useful to monitor your Enterprise Telemetry Monitor cluster health.
Max audit log indices | The number of audit logs indices to keep. This index cannot be queried, but keeps your audit logs to see who is doing what in Enterprise Telemetry Monitor.
Wait for active shards | The number of Elasticsearch shards that need to be active before performing any query. Leave this value to 1 if you have only one Elasticsearch instance in your cluster, or have not configured any replicas.
Retries on conflict | The number of retries before an insert or update query will fail.
Query timeout | The timeout in milliseconds for queries to Elasticsearch.

### Remote clusters
Enterprise Telemetry Monitor is capable of searching through different Elasticsearch clusters. This might be useful when you have multiple datacenters with their own Enterprise Telemetry Monitor instances. To create a single overview of all
events in all datacenters you can connect an Enterprise Telemetry Monitor instance to remote Elasticsearch instances. Remote clusters will be used in the context of [Searching](../searching/README.md), [Visualizations](../visualizations/README.md) and [Signals](../signals/README.md).

To add a remote cluster to the running Enterprise Telemetry Monitor instance click on the *Add cluster* link. This adds a row in the Remote cluster table. You must give the remote
cluster a unique name. When you enable the *Cluster wide* option, Enterprise Telemetry Monitor will manage the connections to the remote cluster for you. To be able to connect to the
remote cluster it needs at least one seed of the remote host. All Elasticsearch instances of the local cluster should be able to connect to all nodes in the remote cluster! When a cluster
cannot be reached Enterprise Telemetry Monitor will not log any warning. The cluster will be ignored without a warning.

When you disable the *Cluster wide* option, Enterprise Telemetry Monitor will not configure anything for you. You need to configure the remote cluster alias in the elasticsearch.yml files
yourself. The only thing you need to tell Enterprise Telemetry Monitor is the cluster name you configured in Elasticsearch. This option gives you more flexibility over which 
nodes are capable of connecting to the remote cluster.  

For mor information on Elasticsearch configuration of remote clusters browse to <https://www.elastic.co/guide/en/elasticsearch/reference/7.x/modules-remote-clusters.html>. 

## Persisting settings
Name | Description
--- | ---
Enhancing handler count | The number of threads that will be used by the event enhancer in the Processor.
Persisting handler count | The number of threads that will be used by the event persister in the Processor. 
Event buffer size | The maximum number of event that can be buffered by the Processor before they are offered to the event enhancer and event persister.
Wait strategy | The strategy to use when the event processor is waiting for events. The *Blocking* strategy can be used when low-latency are not as important as CPU resources. The *Busy spin* strategy will use CPU cycles to avoid syscalls. Syscalls will cause a peek in latency. The *Sleeping* strategy will consume less CPU resources over time, but has also a greater latency peek over time. The *Yielding* strategy is a good compromise between performance and CPU resource without incurring significant latency spikes. 
Persisting bulk count | The maximum number of events that can be buffered before flushed to an Elasticsearch node.
Persisting bulk size | The maximum combined size in bytes of events that can be buffered before flushed to an Elasticsearch node.
Persisting bulk time | The maximum number of millisecond that events can be buffered before flushed to an Elasticsearch node.

## Ldap settings
Name | Description
--- | ---
Ldap host | The hostname or ip-address the ldap server is running on.
Ldap port | The port number the ldap server is listening on.
Connection security | Select the connection security that applies to the ldap server. Make sure to add the required root certificates to the [Certificate settings](#certificate-settings) when connecting to an LDAP server over TLS.
Bind DN | The Distinguished Name (DN) of the user that is connecting to the ldap server.
Bind password | The password used to connect to the ldap server. Note that this password will be base64 encoded into the database. The password will be visible in it's encoded form in the audit logs and in your browsers network log. Make sure only Enterprise Telemetry Monitor has access to your database!
Min connections | The minimum number of connections to the ldap server in the connection pool. 
Max connections | The maximum number of connections to the ldap server in the connection pool.
Connection test base DN | The base DN used to perform a connection test query on.  
Connection test search filter | The search filter used to perform a connection test query. No connection test will be executed when you leave this field empty. 
Group base DN | The base DN for all groups that need to be synchronized with Enterprise Telemetry Monitor. 
Group search filter | The search filter for groups that need to be synchronized with Enterprise Telemetry Monitor. Make sure you use the variable *{group}* on the place where the name of the group would normally be in your search filter. E.g. *(cn={group})*
User base DN | The base DN for all user that need to be synchronized with Enterprise Telemetry Monitor.
User search filter | The search filter for users that need to be synchronized with Enterprise Telemetry Monitor. Make sure you use the variable *{user}* on the place where the id of the user would normally be in your search filter. E.g. *(uid={user})*
User search in subtree | Set to *Yes* when the search for users should take place in the entire *User base DN* instead of only the root of the *User base DN*.
User identifier attribute | The name of the attribute that holds the id of the user.
User full name attribute | The name of the attribute that holds the full name of the user.
User email attribute | The name of the attribute that holds the email address of the user.
User member of groups attribute | Some ldap instances hold the group memberships of an user inside an user attribute. Place the name of that attribute in this field if this situation applies to your ldap configuration. 
User groups query base DN | The base DN for the query to find the group memberships of an user. Most of the time this would be the same af the *Groupe base DN*.
User groups query filter | The filter used to determine the group membership(s) of an user. User attributes may be provided in the for of *{&lt;attribute-name&gt;}*. E.g. *(&#124;(member={dn})(uniqueMember={dn})(memberUid={uid}))*

There are some things to take into account when configuring a connection to a LDAP server in Enterprise Telemetry Monitor. First of all the *Group search filter* should be configured in such a way that only groups that should belong to your Enterprise Telemetry Monitor instance will be returned. So if your LDAP server contains 100 LDAP groups for several applications and 5 of those groups apply to Enterprise Telemetry Monitor, the search filter should be configured to return only those 5 groups when passing '*' as the *{group}* variable.
Secondly, all user that can successfully authenticate against your LDAP server can login to Enterprise Telemetry Monitor unless your *User search filter* limits this set of users. Before authenticating the Distinguished Name (DN) of the provided username is searched. This is where the *User search filter* is applied. If this search action doesn't result in one and only one Distinguished Name the authentication process will fail. So if you want to limit the successful logins to users that belong to a certain Enterprise Telemetry Monitor LDAP group this limitation should be added to the *User search filter*.
Thirdly, if a user is already present in the Enterprise Telemetry Monitor user administration this password takes priority over the LDAP password! The only way to change this is by importing the user as described in the [Import user from ldap](users.md#import-user-from-ldap) chapter.
Finally, ldap users can be added to non-ldap groups, but non-ldap users cannot be added to ldap groups. Password management and groups membership of ldap users cannot be managed in Enterprise Telemetry Monitor.

::: tip 
Make sure you have at least one account with admin privileges that is a non-ldap account. This way it is always possible to login to Enterprise Telemetry Monitor even when the LDAP server isn't reachable for some reason.
:::

## Certificate settings
To make a secure connection to other systems, like an LDAP server, you need to trust te servers certificate. This can be done by adding the root certificate of the trust chain, or importing an intermediate and explicitly set is as trust anchor.

Click on the *Import* and a popup window will be shown. The import method let you choose between download a certificate chain from a remote server, or upload a PEM encoded certificate file to Enterprise Telemetry Monitor. After downloading or uploading a chain you have to select which certificate(s) need to be imported. Usually this would be a root or intermediate certificate.
After importing the certificates you need to tell Enterprise Telemetry Monitor if the certificate is a trust anchor or not. When Enterprise Telemetry Monitor comes across a trust anchor while validating the certificate chain of a server it will stop validating certificates further down the chain. Because of this nature self signed certificates are always acting as trust anchors.
Finally you should provide the *Usage* of the certificate. If you want, for example, add a root certificate that should only be trusted for an LDAP server and not for a SMTP server you should only select the LDAP option at the *Usage* input field. During chain validation all certificates loaded into Enterprise Telemetry Monitor should have the usage selected in the context they are used. So if you also have loaded both the root and intermediate certificates of an LDAP server both certificates should have the LDAP usage enabled.