# Signals
What's a monitoring tool without the option to send some signals? Right, useful but time consuming. Signals give you
the possibility to let Enterprise Telemetry Monitor notify you when something relevant is happening the minute the event took place.

To add, modify or delete signals browse to <http://localhost:8080/gui/signal/signal.html >or select the menu option *Signals*. Note that the menu option might be contain a submenu with your username and the groups you belong to. This extra submenu will give you the opportunity to create a signal for a group or just for yourself.

Each signal is uniquely identified by it's name and selects data from a certain datasource. Enterprise Telemetry Monitor provides 3 datasources:

* Audits
* Events
* Metrics

The Events datasource contains all event you stored in Enterprise Telemetry Monitor. The Metrics datasource contains metric data of all Enterprise Telemetry Monitor instances. This information can be very helpful to monitor
your Enterprise Telemetry Monitor cluster. Finally the Audits datasource tracks all activities run in Enterprise Telemetry Monitor. Depending on your authorizations you might not have access to all datasources.
To narrow down the data used in your signal you can provide a query and a time range on any of the time fields available. This query will be used as filter query for your statistics. Also, when you have the datasource *Events* selected one or more [Filter query](../administrating/users.md#filter-query)'s might be applied.

Signals can be seen as a Line graph with a certain time span, and a horizontal threshold that will trigger a notification when exceeded. Just like the [Graphs](../visualizations/graphs.md) the configuration of a Signal is divided into several sections.

## Data section
In the Data section you can configure the data that should be in the scope of your signal. This basically comes down to selecting the right datasource and define a time scope for
your signal. Besides exact date and timestamps you can of course use an [Elasticsearch Date Math](https://www.elastic.co/guide/en/elasticsearch/reference/7.x/common-options.html#date-math) expression like u used to be in the [Search widget](../searching/search-widget.md).
To further narrow down the data set you can enter any kind of query in the query field.

## Threshold section
In the threshold section you can configure the threshold on which you want to be notified. By adding [Bucket aggregators](../visualizations/graphs.md#bucket-aggregator), [Metric aggregators](../visualizations/graphs.md#metric-aggregator) or [Pipeline aggregators](../visualizations/graphs.md#pipeline-aggregator) you can define on which attribute or attributes
the threshold should be applied. Remember that when you define more than one [Metric aggregator](../visualizations/graphs.md#metric-aggregator) or [Pipeline aggregator](../visualizations/graphs.md#pipeline-aggregator) the threshold will be compared with all those aggregators! When you need a
[Metric aggregator](../visualizations/graphs.md#metric-aggregator) as input for a [Pipeline aggregator](../visualizations/graphs.md#pipeline-aggregator) but don't want that [Metric aggregator](../visualizations/graphs.md#metric-aggregator) to be considered during threshold calculation you need to make sure the 'Show on graph' attribute of the aggregator
is set to 'No'.

## Notifications section
Enterprise Telemetry Monitor will run the query's at a specified interval This interval can be adjusted by changing the *Check every* input value. To receive notification you must add 1 or more [Notifiers](../administrating/notifiers.md). When a notifier with type *Email* is added Enterprise Telemetry Monitor might not send an email on every threshold exceedance.
When, for example, the check interval is set to 1 minute and the threshold is exceeded for 2 hours your mailbox will be spammed with 120 emails all saying the same. To prevent this Enterprise Telemetry Monitor uses an exponential backoff policy. The time between two emails will be doubled on every consecutive failure,
but will never be longer that 45 minutes. Finally, you will receive an email when the threshold is no longer exceeded.

If the signal is added to a group and an email notifier is added the *Email recipients* box on the right gives you the option to notify all members of a group. Be aware that if your group is an LDAP group only members with an Enterprise Telemetry Monitor account and a valid email address will receive an email. In short
this means that a user must have logged in at least once into Enterprise Telemetry Monitor or must be imported from LDAP by an administrator.

The 'Max frequency of exceedance' option let you define how strict the notifiers should kick in. Should Enterprise Telemetry Monitor send an email when a single threshold is exceeded or are you monitoring a self healing application that will be given some time to recover itself?

When all required fields are entered, the visualization button will be enabled to give you a visual representation of the signal you've created.

