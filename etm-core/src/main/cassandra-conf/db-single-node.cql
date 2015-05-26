create keyspace etm with replication = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };
use etm;

create table endpoint_config (
    endpoint text primary key,
    readingApplicationParsers list<text>,
    writingApplicationParsers list<text>,
    eventNameParsers list<text>,
    correlationParsers map<text, text>,
    transactionNameParsers list<text>
);