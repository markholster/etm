create table telemetry_event ( 
    id timeuuid primary key, 
    application text,
    content text,
    correlationCreationTime timestamp,
    correlationData map<text, text>, 
    correlationId timeuuid,
    correlationName text,
    correlations list<timeuuid>,
    creationTime timestamp,
    direction text, 
    endpoint text, 
    expiryTime timestamp,
    metadata map<text, text>, 
    name text,
    sourceCorrelationId text, 
    sourceId text, 
    transactionId timeuuid, 
    transactionName text,
    type text,
    slaRule text
);

create table transaction_performance (
    transactionName_timeunit text,
    startTime timestamp,
    transactionId timeuuid,
    transactionName text,
    expiryTime timestamp,
    finishTime timestamp,
    application text,
    primary key (transactionName_timeunit, startTime, transactionId)
);

create table message_performance (
    name_timeunit text,
    startTime timestamp,
    id timeuuid,
    name text,
    expiryTime timestamp,
    finishTime timestamp,
    application text,
    primary key (name_timeunit, startTime, id)
);

create table message_expiration (
    name_timeunit text,
    expiryTime timestamp,
    id timeuuid,
    name text,
    startTime timestamp,
    finishTime timestamp,
    application text,
    primary key (name_timeunit, expiryTime, id)
);

create table transaction_sla (
    transactionName_timeunit text,
    slaExpiryTime timestamp,
    transactionId timeuuid,
    transactionName text,
    startTime timestamp,
    finishTime timestamp,
    application text,
    slaRule text,
    primary key (transactionName_timeunit, slaExpiryTime, transactionId)
);

create table event_occurrences (
    timeunit timestamp,
    type text,
    name_timeframe text,
    name text,
    primary key (timeunit, type, name_timeframe)
);

create table sourceid_id_correlation (
    sourceId text,
    application text,
    creationTime timestamp,
    expiryTime timestamp,
    id timeuuid,
    transactionId timeuuid,
    transactionName text,
    name text,
    slaRule text,
    primary key (sourceId, application)
);

create table correlation_data (
    name_timeunit text,
    name text,
    value text,
    timeunit timestamp,
    id timeuuid, 
    primary key (name_timeunit, name, value, timeunit, id)
);

create table application_counter (
    application_timeunit text,
    timeunit timestamp,
    application text,
    count counter,
    messageRequestCount counter,
    incomingMessageRequestCount counter,
    outgoingMessageRequestCount counter,
    messageResponseCount counter,
    incomingMessageResponseCount counter,
    outgoingMessageResponseCount counter,
    messageDatagramCount counter,
    incomingMessageDatagramCount counter,
    outgoingMessageDatagramCount counter,
    messageResponseTime counter,
    incomingMessageResponseTime counter,
    outgoingMessageResponseTime counter,
    primary key (application_timeunit, timeunit, application)
);

create table eventname_counter (
    eventName_timeunit text,
    timeunit timestamp,
    eventName text,
    count counter,
    messageRequestCount counter,
    messageResponseCount counter,
    messageDatagramCount counter,
    messageResponseTime counter,
    primary key (eventName_timeunit, timeunit, eventName)
);

create table application_event_counter (
    application_timeunit text,
    timeunit timestamp,
    application text,
    eventName text,
    count counter,
    messageRequestCount counter,
    incomingMessageRequestCount counter,
    outgoingMessageRequestCount counter,
    messageResponseCount counter,
    incomingMessageResponseCount counter,
    outgoingMessageResponseCount counter,
    messageDatagramCount counter,
    incomingMessageDatagramCount counter,
    outgoingMessageDatagramCount counter,
    messageResponseTime counter,
    incomingMessageResponseTime counter,
    outgoingMessageResponseTime counter,
    primary key (application_timeunit, timeunit, application, eventName)
);

create table transactionname_counter (
    transactionName_timeunit text,
    timeunit timestamp,
    transactionName text,
    count counter,
    transactionStart counter,
    transactionFinish counter,
    transactionResponseTime counter,
    primary key (transactionName_timeunit, timeunit, transactionName)
);

create table data_retention (
    timeunit timestamp,
    id timeuuid,
    eventOccurrenceTimeunit timestamp,
    sourceId text,
    partionKeySuffix text,
    applicationName text,
    eventName text,
    transactionName text,
    correlationData map<text, text>,
    primary key (timeunit, id)
);

create table endpoint_config (
    endpoint text primary key,
    direction text,
    applicationParsers list<text>,
    eventNameParsers list<text>,
    correlationParsers map<text, text>,
    transactionNameParsers list<text>,
    slaRules map<text, text>
);