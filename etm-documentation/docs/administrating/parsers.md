# Parsers

To add, modify or delete parsers browse to <http://localhost:8080/gui/settings/parsers.html> or select the menu option *Settings -> Parsers*.

Parsers are used to extract information from an event so it can be categorized and/or enhanced with custom values based on the content of an event attribute.

A parser describes a way of extracting data from an event, nothing more nothing less.

## Copy value parser

The copy value parser simply copies the content of the given attribute.
This parser can be particularly useful in cases where the id of an event is stored in a metadata attribute and simply needs to be copied to the id field.

## Fixed position parser

The fixed position parser extract information from the given attribute of an event at a fixed position.
This parser can be useful if you are dealing with payload that has a static layout like good old COBOL records.

## Fixed value parser

The Fixed value parser provides a fixed value no matter what content it is provided with.
Strictly speaking this is not a parser, but always provides the same value.

## Javascript parser

The Javascript parser can execute a ECMAScript 2019 compatible javascript expression.
The script should contain a function that accepts 2 variables.
The first variable will be the content, and the second parameter is the replacement value.
If the parser is used as field extraction only the first parameter is required.
The method should return a String value, or null when no value can be returned.
In the method field the name of the main method from the script should be entered.

## JsonPath parser

The JsonPath parser is capable of extracting data from json content.
There's no formal standard describing the Json path standard, but Enterprise Telemetry Monitor is following [Stefan Goessner's JsonPath implementation](http://goessner.net/articles/JsonPath/).

## Regular expression parser

The Regular expression parser is capable of extracting data based on a Java [regular expression](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/regex/Pattern.html).

## XPath parser

The XPath parser can extract data from XML based content.
XPath 2.0, 3,0 & 3.1 queries are supported to extract data from any XML and/or Soap events.

## XSLT parser

The XSLT parser can extract data from XML based content.
All XSLT 2.0 compatible templates are supported to extract data from any XML and/or Soap events.