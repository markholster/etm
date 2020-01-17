# Search widget
The left part of the search screen shows an input field in which you can enter your search terms. Optionally you can filter on any of the known event types by
(un)checking the event types you want to be in- or excluded from the search. To query events in a certain date and time range you may a start- and/or end date.
To select an specific date you can enter that date and time, or select the appropriate value from the popup calendar. If the time range should be more flexible
you can also provide an [Elasticsearch Date Math](https://www.elastic.co/guide/en/elasticsearch/reference/7.x/common-options.html#date-math) expression. In the time field you can enter
the name of a field in which the time range should fall.

In the query input field you can enter the terms you are searching for. See the section [Query syntax](query-syntax.md) for the query syntax that can be used. The input field provides a basic form of auto completion by hitting the arrow down button on your keyboard. All available attributes for the selected event types will be show in a drop down box.