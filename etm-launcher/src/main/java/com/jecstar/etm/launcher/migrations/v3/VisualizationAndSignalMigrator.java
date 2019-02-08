package com.jecstar.etm.launcher.migrations.v3;

import com.jecstar.etm.gui.rest.services.dashboard.domain.*;
import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.DashboardConverter;
import com.jecstar.etm.gui.rest.services.dashboard.domain.converter.GraphContainerConverter;
import com.jecstar.etm.gui.rest.services.dashboard.domain.graph.*;
import com.jecstar.etm.launcher.migrations.AbstractEtmMigrator;
import com.jecstar.etm.server.core.domain.aggregator.Aggregator;
import com.jecstar.etm.server.core.domain.aggregator.bucket.*;
import com.jecstar.etm.server.core.domain.aggregator.metric.*;
import com.jecstar.etm.server.core.domain.configuration.ElasticsearchLayout;
import com.jecstar.etm.server.core.domain.converter.json.JsonConverter;
import com.jecstar.etm.signaler.domain.Notifications;
import com.jecstar.etm.signaler.domain.Signal;
import com.jecstar.etm.signaler.domain.Threshold;
import com.jecstar.etm.signaler.domain.TimeUnit;
import com.jecstar.etm.signaler.domain.converter.SignalConverter;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.index.IndexAction;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import java.util.*;
import java.util.function.Function;

/**
 * Migrator that migrates the graphs, dashboards and signals from users and groups to the new layout using hierarchical objects.
 *
 * @since 3.4.0
 */
public class VisualizationAndSignalMigrator extends AbstractEtmMigrator {

    private final String migrationIndexPrefix = "migetm_";
    private final Client client;
    private final JsonConverter jsonConverter = new JsonConverter();
    private final GraphContainerConverter graphContainerConverter = new GraphContainerConverter();
    private final DashboardConverter dashboardConverter = new DashboardConverter();
    private final SignalConverter signalConverter = new SignalConverter();

    public VisualizationAndSignalMigrator(Client client) {
        this.client = client;
    }

    @Override
    public boolean shouldBeExecuted() {
        SearchHits searchHits = client.prepareSearch(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery())
                        .should(QueryBuilders.existsQuery("user.graphs.query"))
                        .should(QueryBuilders.existsQuery("user.signals.query"))
                        .should(QueryBuilders.existsQuery("group.graphs.query"))
                        .should(QueryBuilders.existsQuery("group.signals.query"))
                        .minimumShouldMatch(1)
                )
                .get().getHits();

        return searchHits.totalHits != 0;
    }

    @Override
    public void migrate() {
        if (!shouldBeExecuted()) {
            return;
        }
        checkAndCleanupPreviousRun(this.client, this.migrationIndexPrefix);

        Function<SearchHit, DocWriteRequest> processor = searchHit -> {
            IndexRequestBuilder builder = new IndexRequestBuilder(this.client, IndexAction.INSTANCE)
                    .setIndex(VisualizationAndSignalMigrator.this.migrationIndexPrefix + searchHit.getIndex())
                    .setType(searchHit.getType())
                    .setId(searchHit.getId())
                    .setSource(mapToNewLayout(searchHit.getSourceAsMap()));
            return builder.request();
        };

        FailureDetectingBulkProcessorListener listener = new FailureDetectingBulkProcessorListener();
        boolean succeeded = migrateUsers(this.client, createBulkProcessor(client, listener), listener, processor);
        if (succeeded) {
            succeeded = migrateGroups(this.client, createBulkProcessor(client, listener), listener, processor);
        }
        if (succeeded) {
            succeeded = moveAllOtherConfigurationItems(this.client, createBulkProcessor(client, listener), listener);
        }
        if (!succeeded) {
            System.out.println("Errors detected. Quitting migration. Migrated indices are prefixed with '" + this.migrationIndexPrefix + "' and are still existent in your Elasticsearch cluster!");
            return;
        }
        deleteIndices(this.client, "old indices", ElasticsearchLayout.CONFIGURATION_INDEX_NAME);
        flushIndices(this.client, this.migrationIndexPrefix + ElasticsearchLayout.CONFIGURATION_INDEX_NAME);
        reindexTemporaryIndicesToNew(this.client, listener, this.migrationIndexPrefix);
        deleteIndices(this.client, "temporary indices", this.migrationIndexPrefix + "*");
        deleteTemporaryIndexTemplate(this.client, this.migrationIndexPrefix);
        checkAndCreateIndexExistence(client, ElasticsearchLayout.CONFIGURATION_INDEX_NAME);
    }

    private boolean migrateUsers(Client client, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener, Function<SearchHit, DocWriteRequest> processor) {
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery())
                        .must(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER)))
                .setTimeout(TimeValue.timeValueSeconds(30))
                .setFetchSource(true);
        return migrateEntity(searchRequestBuilder, "users", client, bulkProcessor, listener, processor);
    }

    private boolean migrateGroups(Client client, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener, Function<SearchHit, DocWriteRequest> processor) {
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery())
                        .must(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP)))
                .setTimeout(TimeValue.timeValueSeconds(30))
                .setFetchSource(true);
        return migrateEntity(searchRequestBuilder, "groups", client, bulkProcessor, listener, processor);
    }

    private boolean moveAllOtherConfigurationItems(Client client, BulkProcessor bulkProcessor, FailureDetectingBulkProcessorListener listener) {
        Function<SearchHit, DocWriteRequest> processor = searchHit -> {
            IndexRequestBuilder builder = new IndexRequestBuilder(this.client, IndexAction.INSTANCE)
                    .setIndex(VisualizationAndSignalMigrator.this.migrationIndexPrefix + searchHit.getIndex())
                    .setType(searchHit.getType())
                    .setId(searchHit.getId())
                    .setSource(searchHit.getSourceAsMap());
            return builder.request();
        };
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(ElasticsearchLayout.CONFIGURATION_INDEX_NAME)
                .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery())
                        .mustNot(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_USER))
                        .mustNot(QueryBuilders.termQuery(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, ElasticsearchLayout.CONFIGURATION_OBJECT_TYPE_GROUP)))
                .setTimeout(TimeValue.timeValueSeconds(30))
                .setFetchSource(true);
        return migrateEntity(searchRequestBuilder, "everything else", client, bulkProcessor, listener, processor);
    }

    private Map<String, Object> mapToNewLayout(Map<String, Object> sourceAsMap) {
        String objectType = this.jsonConverter.getString(ElasticsearchLayout.ETM_TYPE_ATTRIBUTE_NAME, sourceAsMap);
        Map<String, Object> entityMap = this.jsonConverter.getObject(objectType, sourceAsMap);
        List<Map<String, Object>> graphsMap = this.jsonConverter.getArray("graphs", entityMap);
        if (graphsMap != null) {
            mapGraphs(graphsMap);
        }
        List<Map<String, Object>> dashboardsMap = this.jsonConverter.getArray("dashboards", entityMap);
        if (dashboardsMap != null) {
            mapDashboards(dashboardsMap);
        }
        List<Map<String, Object>> signalsMap = this.jsonConverter.getArray("signals", entityMap);
        if (signalsMap != null) {
            mapSignals(signalsMap);
        }
        return sourceAsMap;
    }

    @SuppressWarnings("unchecked")
    private void mapGraphs(List<Map<String, Object>> graphsMap) {
        ListIterator<Map<String, Object>> iterator = graphsMap.listIterator();
        while (iterator.hasNext()) {
            Map<String, Object> graphMap = iterator.next();
            GraphContainer graphContainer = new GraphContainer();
            graphContainer.setName(this.jsonConverter.getString("name", graphMap));

            Data data = new Data();
            data.setDataSource(this.jsonConverter.getString("data_source", graphMap));
            data.setFrom(this.jsonConverter.getString("from", graphMap));
            data.setTill(this.jsonConverter.getString("till", graphMap));
            data.setQuery(this.jsonConverter.getString("query", graphMap));
            data.setTimeFilterField(this.jsonConverter.getString("time_filter_field", graphMap));
            graphContainer.setData(data);

            Graph graph = null;
            Map<String, Object> graphTypeMap = null;
            if (graphMap.containsKey("line")) {
                graphTypeMap = this.jsonConverter.getObject("line", graphMap);
                graph = new LineGraph().setLineType(LineType.SMOOTH).setShowDataLabels(false).setShowMarkers(false);
            } else if (graphMap.containsKey("stacked_area")) {
                graphTypeMap = this.jsonConverter.getObject("stacked_area", graphMap);
                graph = new AreaGraph().setLineType(LineType.SMOOTH).setShowDataLabels(false).setShowMarkers(false).setSubType("stacked");
            } else if (graphMap.containsKey("bar")) {
                graphTypeMap = this.jsonConverter.getObject("bar", graphMap);
                graph = new BarGraph().setSubType("basic");
            } else if (graphMap.containsKey("number")) {
                graphTypeMap = this.jsonConverter.getObject("number", graphMap);
                YAxis yAxis = new YAxis();
                List<Map<String, Object>> aggregators = new ArrayList<>();
                // The graphType is a metric aggregator
                aggregators.add(graphTypeMap);
                yAxis.setAggregators(createMetricAggregators(aggregators));
                graph = new NumberGraph().setyAxis(yAxis);
            }
            if (graph instanceof AxesGraph) {
                AxesGraph axesGraph = (AxesGraph) graph;
                XAxis xAxis = new XAxis();
                YAxis yAxis = new YAxis();
                axesGraph.setXAxis(xAxis).setYAxis(yAxis).setShowLegend(true).setOrientation(AxesGraph.Orientation.VERTICAL);

                Map<String, Object> xAxisMap = this.jsonConverter.getObject("x_axis", graphTypeMap);
                Map<String, Object> yAxisMap = this.jsonConverter.getObject("y_axis", graphTypeMap);
                yAxis.setFormat(this.jsonConverter.getString("format", yAxisMap, "d"));

                Map<String, Object> aggregatorMap = this.jsonConverter.getObject("aggregator", xAxisMap);
                xAxis.setBucketAggregator(createBucketAggregator(aggregatorMap));

                List<Aggregator> aggregators = createMetricAggregators(this.jsonConverter.getArray("aggregators", yAxisMap));

                Map<String, Object> subAggregatorMap = this.jsonConverter.getObject("sub_aggregator", xAxisMap);
                if (subAggregatorMap != null) {
                    BucketAggregator subAggregator = createBucketAggregator(subAggregatorMap);
                    subAggregator.setAggregators(aggregators);
                    yAxis.setAggregators(Arrays.asList(subAggregator));
                } else {
                    yAxis.setAggregators(aggregators);
                }
            }
            if (graph != null) {
                graphContainer.setGraph(graph);
            }

            iterator.set(this.jsonConverter.toMap(this.graphContainerConverter.write(graphContainer)));
        }
    }

    private void mapDashboards(List<Map<String, Object>> dashboardsMap) {
        ListIterator<Map<String, Object>> iterator = dashboardsMap.listIterator();
        while (iterator.hasNext()) {
            Map<String, Object> dashboardMap = iterator.next();
            Dashboard dashboard = new Dashboard();
            dashboard.setName(this.jsonConverter.getString("name", dashboardMap));

            List<Map<String, Object>> rowsList = this.jsonConverter.getArray("rows", dashboardMap);
            if (rowsList != null) {
                List<Row> rows = new ArrayList<>();
                for (Map<String, Object> rowMap : rowsList) {
                    Row row = new Row();
                    row
                            .setId(this.jsonConverter.getString("id", rowMap))
                            .setHeight(this.jsonConverter.getInteger("height", rowMap, 16));
                    List<Map<String, Object>> columnsList = this.jsonConverter.getArray("cols", rowMap);
                    if (columnsList != null) {
                        List<Column> columns = new ArrayList<>();
                        for (Map<String, Object> columnMap : columnsList) {
                            Column column = new Column()
                                    .setWidth(this.jsonConverter.getInteger("parts", columnMap))
                                    .setId(this.jsonConverter.getString("id", columnMap))
                                    .setTitle(this.jsonConverter.getString("title", columnMap))
                                    .setGraphName(this.jsonConverter.getString("name", columnMap))
                                    .setBordered(this.jsonConverter.getBoolean("bordered", columnMap, true))
                                    .setRefreshRate(this.jsonConverter.getInteger("refresh_rate", columnMap));
                            Data data = new Data();
                            data.setDataSource(this.jsonConverter.getString("data_source", columnMap));
                            data.setFrom(this.jsonConverter.getString("from", columnMap));
                            data.setTill(this.jsonConverter.getString("till", columnMap));
                            data.setQuery(this.jsonConverter.getString("query", columnMap));
                            data.setTimeFilterField(this.jsonConverter.getString("time_filter_field", columnMap));
                            column.setData(data);

                            columns.add(column);
                        }
                        row.setColumns(columns);

                    }
                    rows.add(row);
                }
                dashboard.setRows(rows);
            }

            iterator.set(this.jsonConverter.toMap(this.dashboardConverter.write(dashboard)));
        }
    }

    private void mapSignals(List<Map<String, Object>> signalsMap) {
        ListIterator<Map<String, Object>> iterator = signalsMap.listIterator();
        while (iterator.hasNext()) {
            Map<String, Object> signalMap = iterator.next();
            Signal signal = new Signal().setName(this.jsonConverter.getString("name", signalMap));

            com.jecstar.etm.signaler.domain.Data data = new com.jecstar.etm.signaler.domain.Data();
            data.setDataSource(this.jsonConverter.getString("data_source", signalMap));
            data.setFrom("now-" + TimeUnit.safeValueOf(this.jsonConverter.getString("timespan_timeunit", signalMap)).toTimestampExpression(this.jsonConverter.getInteger("timespan", signalMap)));
            data.setTill("now");
            data.setQuery(this.jsonConverter.getString("query", signalMap));
            data.setTimeFilterField("timestamp");
            signal.setData(data);

            Threshold threshold = new Threshold();
            threshold.setValue(this.jsonConverter.getDouble("threshold", signalMap));
            threshold.setCardinality(this.jsonConverter.getInteger("cardinality", signalMap));
            threshold.setCardinalityUnit(TimeUnit.safeValueOf(this.jsonConverter.getString("cardinality_timeunit", signalMap)));
            threshold.setComparison(Threshold.Comparison.safeValueOf(this.jsonConverter.getString("comparison", signalMap)));
            String op = this.jsonConverter.getString("operation", signalMap);
            MetricsAggregator metricsAggregator = null;
            if ("AVERAGE".equalsIgnoreCase(op)) {
                metricsAggregator = new AverageMetricsAggregator()
                        .setField(this.jsonConverter.getString("field", signalMap));
            } else if ("COUNT".equalsIgnoreCase(op)) {
                metricsAggregator = new CountMetricsAggregator();
            } else if ("MAX".equalsIgnoreCase(op)) {
                metricsAggregator = new MaxMetricsAggregator()
                        .setField(this.jsonConverter.getString("field", signalMap));
            } else if ("MEDIAN".equalsIgnoreCase(op)) {
                metricsAggregator = new MedianMetricsAggregator()
                        .setField(this.jsonConverter.getString("field", signalMap));
            } else if ("MIN".equalsIgnoreCase(op)) {
                metricsAggregator = new MinMetricsAggregator()
                        .setField(this.jsonConverter.getString("field", signalMap));
            } else if ("SUM".equalsIgnoreCase(op)) {
                metricsAggregator = new SumMetricsAggregator()
                        .setField(this.jsonConverter.getString("field", signalMap));
            } else if ("CARDINALITY".equalsIgnoreCase(op)) {
                metricsAggregator = new UniqueCountMetricsAggregator()
                        .setField(this.jsonConverter.getString("field", signalMap));
            }
            if (metricsAggregator != null) {
                metricsAggregator.setId(UUID.randomUUID().toString()).setShowOnGraph(true);
                threshold.setAggregators(Arrays.asList(metricsAggregator));
            }
            signal.setThreshold(threshold);

            Notifications notifications = new Notifications();
            notifications.setInterval(this.jsonConverter.getInteger("interval", signalMap));
            notifications.setIntervalUnit(TimeUnit.safeValueOf(this.jsonConverter.getString("interval_timeunit", signalMap)));
            notifications.setMaxFrequencyOfExceedance(this.jsonConverter.getInteger("limit", signalMap));
            notifications.setEmailAllEtmGroupMembers(this.jsonConverter.getBoolean("email_all_etm_group_members", signalMap, null));
            notifications.setEmailRecipients(this.jsonConverter.getArray("email_recipients", signalMap));
            notifications.setNotifiers(this.jsonConverter.getArray("notifiers", signalMap));
            signal.setNotifications(notifications);

            Map<String, Object> resultMap = this.jsonConverter.toMap(this.signalConverter.write(signal));
            for (String attr : Signal.METADATA_KEYS) {
                if (signalMap.containsKey(attr)) {
                    resultMap.put(attr, signalMap.get(attr));
                }
            }
            iterator.set(resultMap);
        }
    }

    private List<Aggregator> createMetricAggregators(List<Map<String, Object>> aggregators) {
        List<Aggregator> result = new ArrayList<>();
        for (Map<String, Object> aggregatorMap : aggregators) {
            MetricsAggregator metricsAggregator = null;
            String aggregatorType = this.jsonConverter.getString("aggregator", aggregatorMap);
            if ("count".equalsIgnoreCase(aggregatorType)) {
                metricsAggregator = new CountMetricsAggregator();

            } else if ("average".equalsIgnoreCase(aggregatorType)) {
                metricsAggregator = new AverageMetricsAggregator()
                        .setField(this.jsonConverter.getString("field", aggregatorMap));
            } else if ("max".equalsIgnoreCase(aggregatorType)) {
                metricsAggregator = new MaxMetricsAggregator()
                        .setField(this.jsonConverter.getString("field", aggregatorMap));
            } else if ("median".equalsIgnoreCase(aggregatorType)) {
                metricsAggregator = new MedianMetricsAggregator()
                        .setField(this.jsonConverter.getString("field", aggregatorMap));
            } else if ("min".equalsIgnoreCase(aggregatorType)) {
                metricsAggregator = new MinMetricsAggregator()
                        .setField(this.jsonConverter.getString("field", aggregatorMap));
            } else if ("percentile".equalsIgnoreCase(aggregatorType)) {
                metricsAggregator = new PercentileMetricsAggregator()
                        .setPercentile(this.jsonConverter.getDouble("percentile_data", aggregatorMap))
                        .setField(this.jsonConverter.getString("field", aggregatorMap));
            } else if ("percentile_rank".equalsIgnoreCase(aggregatorType)) {
                metricsAggregator = new PercentileRankMetricsAggregator()
                        .setRank(this.jsonConverter.getDouble("percentile_data", aggregatorMap))
                        .setField(this.jsonConverter.getString("field", aggregatorMap));
            } else if ("sum".equalsIgnoreCase(aggregatorType)) {
                metricsAggregator = new SumMetricsAggregator()
                        .setField(this.jsonConverter.getString("field", aggregatorMap));
            } else if ("cardinality".equalsIgnoreCase(aggregatorType)) {
                metricsAggregator = new UniqueCountMetricsAggregator()
                        .setField(this.jsonConverter.getString("field", aggregatorMap));
            }
            if (metricsAggregator != null) {
                metricsAggregator
                        .setId(this.jsonConverter.getString("id", aggregatorMap))
                        .setName(this.jsonConverter.getString("label", aggregatorMap))
                        .setShowOnGraph(true);
                result.add(metricsAggregator);
            }
        }
        return result;
    }

    private BucketAggregator createBucketAggregator(Map<String, Object> aggregatorMap) {
        BucketAggregator bucketAggregator = null;
        String aggregatorType = this.jsonConverter.getString("aggregator", aggregatorMap);
        if ("date_histogram".equalsIgnoreCase(aggregatorType)) {
            bucketAggregator = new DateHistogramBucketAggregator()
                    .setInterval(this.jsonConverter.getString("interval", aggregatorMap))
                    .setMinDocCount(null);
        } else if ("term".equalsIgnoreCase(aggregatorType)) {
            String orderBy = this.jsonConverter.getString("order_by", aggregatorMap);
            if ("term".equals(orderBy)) {
                orderBy = "_key";
            }
            bucketAggregator = new TermBucketAggregator()
                    .setOrder(this.jsonConverter.getString("order", aggregatorMap))
                    .setOrderBy(orderBy)
                    .setTop(this.jsonConverter.getInteger("top", aggregatorMap))
                    .setMinDocCount(null);
        } else if ("histogram".equalsIgnoreCase(aggregatorType)) {
            bucketAggregator = new HistogramBucketAggregator()
                    .setInterval(this.jsonConverter.getDouble("interval", aggregatorMap))
                    .setMinDocCount(null);
        } else if ("significant_term".equalsIgnoreCase(aggregatorType)) {
            bucketAggregator = new SignificantTermBucketAggregator()
                    .setTop(this.jsonConverter.getInteger("top", aggregatorMap, 5))
                    .setMinDocCount(null);

        }
        if (bucketAggregator != null) {
            bucketAggregator
                    .setField(this.jsonConverter.getString("field", aggregatorMap))
                    .setFieldType(this.jsonConverter.getString("field_type", aggregatorMap))
                    .setId(UUID.randomUUID().toString())
                    .setShowOnGraph(true);

        }
        return bucketAggregator;
    }

}
