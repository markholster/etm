create keyspace etm with replication = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };
use etm;

create table endpoint_config (
    endpoint text primary key,
    reading_application_parsers list<text>,
    writing_application_parsers list<text>,
    eventname_parsers list<text>,
    correlation_parsers map<text, text>,
    transaction_name_parsers list<text>
);

create table telemetry_event ( 
    id text primary key, 
    content text,
    correlation_id text,
    correlations list<text>,
    endpoint text, 
    name text,
    writing_application text,
    writing_time timestamp
);

create table correlation_data (
    name_timeunit text,
    name text,
    value text,
    event_time timestamp,
    event_id text, 
    primary key (name_timeunit, name, value, event_time, event_id)
);

create table event_name_counter (
    event_name text,
    event_name_day text,
    aggregation text,
    event_time timestamp,
    count counter,
    message_request_count counter,
    message_response_count counter,
    message_datagram_count counter,
    primary key ((event_name, event_name_day), aggregation, event_time)
);