# Query syntax
The query syntax is quite extensive and at the base you can provide a series of terms and operators. By default all event attributes are matched, but specific attributes can be specified to narrow down the query. The query syntax is based on the [Elasticsearch Query DSL](https://www.elastic.co/guide/en/elasticsearch/reference/7.x/query-dsl-query-string-query.html).
In large datasets you can improve search performance by always specifying the event attribute you want to query. This allows Enterprise Telemetry Monitor to narrow the search down to the given attribute instead of querying them all.  

## Event attribute names
Searching for specific event attribute can be done with the following syntax:
```coffeescript
<attribute-name>: value
```

for example this query will search for events where the *name* attribute contains *MyEventName*
```coffeescript
name: MyEventName
```

Also values can be combined. If you omit the OR operator it will be applied as default.
```coffeescript
name: (MyEventName OR MyOtherName)
name: (MyEventName MyOtherName)
```

When searching for an exact match the term must be quoted.
```coffeescript
name: "My Name Should Be An Exact Match!"
```

There is 1 reserved keyword: `_exists_` It takes an event attribute as parameter and queries for events that contain the provided attribute.

## Wildcards
Wildcards can be applied to terms, using `?` to replace a single character and `*` to replace zero or more characters.

The following query will search for events of which the name contains a term that starts with *My&#42;*.
```coffeescript
name: My*
```

CAUTION: Starting a term with a wildcard will be very inefficient and may consume a lot of memory. Try to prevent such queries at any cost because all terms in the index need to be examined.

## Fuzziness
Sometimes the data in events contains human misspellings. Those misspellings won't match any of the previous mentioned search methods, but with a so called fuzzy search you are still be able to match them. Use the fuzzy operator `~` in your query to match terms that are like the given term in the query.
```coffeescript
name: MyEventNmae~
```

Fuzzy queries use the [Damerau-Levenshtein distance](https://en.wikipedia.org/wiki/Damerau-Levenshtein_distance) to find all terms with a maximum of 2 differences. The edit distance can be added to the query:
```coffeescript
name: MyEvntNmae~4
```

## Proximity searches
A proximity search allows the specified terms to be close to each other but not necessary next to each other. For example
```coffeescript
name: "My Name"~5
```

searches for the *My* and *Name* terms with a term distance of 5. 

## Ranges
Ranges can be specified to numeric and date attributes. Inclusive ranges are specified with square brackets `[min TO max]` and exclusive ranges with curly brackets `{min TO max}`.

All events of 2018 can be queried as follow
```coffeescript
endpoints.endpoint_handlers.handling_time: [2018-01-01 TO 2018-12-31]
```

Or all events with a payload length between 1000 and 2000 chars
```coffeescript
payload_length: [1000 TO 2000]
```

Wildcards can also be applied
```coffeescript
payload_length: [1000 TO *]
```

And even curly brackets and square brackets can be combined in a single range
```coffeescript
payload_length: [10 TO 50}
```

For queries without an upper or lower bound the mathematical syntax can be used
```coffeescript
payload_length:>1000
payload_length:>=1000
payload_length:<1000
payload_length:<=1000
```

## Boolean operators
By default all provided terms are optional unless they are quoted. This behavior can be changed by adding the boolean operators `+` and `-`. For example
```coffeescript
name: My +name -must not be +empty
```

states that we are searching for an event with a name that must contain the terms *name* and *empty*, must not contain the term *must* and may contain the terms *My*, *not* and *be*.

## Grouping

Terms can be grouped by using parentheses.
This is in particular useful if you want to combine multiple `AND` and `OR` operators

```coffeescript
name: (My AND name) OR (must AND be) OR empty
```

## Joins

Enterprise Telemetry Monitor supports a limited sets of joins.
Requests and responses can be joined into a single query using the `WITH <REQUEST|RESPONSE>` statement.
If you, for example, need all the request messages with a response payload length > 100 you could use the following query

```coffeescript
* WITH RESPONSE payload_length:> 100
```

Vice versa you could do the same to list all responses with a certain name of which the request payload length was greater that a certain amount

```coffeescript
name: MyResponseName WITH REQUEST payload_length:> 100
```

WARNING: Under the hood Enterprise Telemetry Monitor will execute multiple queries to make these joins possible.
Each sub query after the `WITH <REQUEST|RESPONSE>` statement may never result in more than 65536 matches otherwise an error will be thrown.
To scope this query to a minimum of results the *Start date* and *End date* are also taken into account during execution.
This means that both the requests and responses should be within the given date/time range.

## Reserved characters

The reserved characters are: `+ - = && || > < ! ( ) { } [ ] ^ " ~ * ? : \ /` 
If you want to use any of these characters in your term(s) you have to escape them by a leading backslash.
For example

```coffeescript
name: \(DemoName\) 
```

searches for an event with the name *(DemoName)*.

## Unexpected results
Most of the time the above instructions will give you the results you're looking for. In some specific situations like email addresses
or URL's you may see more or fewer results than you expected. In almost all cases this is caused by the way Enterprise Telemetry Monitor is
storing the events in Elasticsearch. Event values aren't stored as is in the database, but are split into one or more 'tokens'. 

By default, Enterprise Telemetry monitor is using the default tokenizer of Elasticsearch which is perfectly fine for most text/languages.  
When you search for `name: MyEventName`, Enterprise Telemetry Monitor is actually searching for a *token* that matches `MyEventName`, and 
not an entire attribute *value*.
 
To fully understand the search process you need to know the difference between a token, and an attribute value. Fortunately that's quite simple. 
An attribute value may be split up into several tokens. That's it, no more, no less.

To explain why this process may lead to unexpected search results let me show you some examples. When you have indexed an event on the endpoint 
`http://instserv0001.my.corp/shopping-card.html` and you want to search for it you could use the following query:
```coffeescript
endpoints.name: http://instserv0001.my.corp/shopping-card.html
```
This query will fail because we need to [escape the special characters](#reserved-characters). So our second try would be 
```coffeescript
endpoints.name: http\:\/\/instserv0001.my.corp\/shopping-card.html
```
This time we receive results. A lot of results! Most of those results don't match the specified endpoint name (i'll explain later why). 
Let's change the query a little again and make it an exact match by using double quotes around the value.
```coffeescript
endpoints.name: "http://instserv0001.my.corp/shopping-card.html"
```
There we go. Finally we've got the events we were looking for. 

Now, let's say we want to search for all calls on `http://instserv0001.my.corp`, or all shopping related calls like 
`http://instserv0001.my.corp/shopping*`. That's not going to work with double quotes because that will only find exact
matched. But if we don't use double quotes we're getting way too many results. In order to understand why we get so many
results we need to remember a query is executed against *tokens* and not attribute *values*. The attribute value 
`http://instserv0001.my.corp/shopping-card.html` is actually split up in the tokens `http`, `instser0001`, `my.corp`, 
`shopping` and `card.html`. The actual token algorithm is not covert in this document but is described in the 
[Unicode Standard Annex #29](http://unicode.org/reports/tr29/).

With this knowledge we can easily find all requests to `instserv0001.my.corp` by quering for 
```coffeescript
endpoints.name: instserv0001
```
or
```coffeescript
endpoints.name: (instserv0001 AND my.corp)
```
when you have multiple instserv0001 instances running in different domains. 

Now we get rid of the double quotes we can unleash all flexibility on our queries again. For example
```coffeescript
endpoints.name: (instserv* AND my.copr~)
```