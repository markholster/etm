package com.jecstar.etm.server.core.domain.aggregator.converter;

import com.jecstar.etm.server.core.converter.custom.NestedObjectConverter;
import com.jecstar.etm.server.core.domain.aggregator.Aggregator;
import com.jecstar.etm.server.core.domain.aggregator.bucket.*;
import com.jecstar.etm.server.core.domain.aggregator.metric.*;
import com.jecstar.etm.server.core.domain.aggregator.pipeline.*;

import java.util.Map;

public class AggregatorConverter extends NestedObjectConverter<Aggregator> {

    public AggregatorConverter() {
        super(f -> {
            switch ((String) f.get(Aggregator.AGGREGATOR_TYPE)) {
                case BucketAggregator.TYPE:
                    return createBucketAggregator(f);
                case MetricsAggregator.TYPE:
                    return createMetricsAggregator(f);
                case PipelineAggregator.TYPE:
                    return createPipelineAggregator(f);
                default:
                    throw new IllegalArgumentException((String) f.get(Aggregator.AGGREGATOR_TYPE));
            }
        });
    }

    private static BucketAggregator createBucketAggregator(Map<String, Object> f) {
        switch ((String) f.get(BucketAggregator.BUCKET_AGGREGATOR_TYPE)) {
            case DateHistogramBucketAggregator.TYPE:
                return new DateHistogramBucketAggregator();
            case FilterBucketAggregator.TYPE:
                return new FilterBucketAggregator();
            case HistogramBucketAggregator.TYPE:
                return new HistogramBucketAggregator();
            case MissingBucketAggregator.TYPE:
                return new MissingBucketAggregator();
            case SignificantTermBucketAggregator.TYPE:
                return new SignificantTermBucketAggregator();
            case TermBucketAggregator.TYPE:
                return new TermBucketAggregator();
            default:
                throw new IllegalArgumentException((String) f.get(BucketAggregator.BUCKET_AGGREGATOR_TYPE));
        }
    }


    private static MetricsAggregator createMetricsAggregator(Map<String, Object> f) {
        switch ((String) f.get(MetricsAggregator.METRICS_AGGREGATOR_TYPE)) {
            case AverageMetricsAggregator.TYPE:
                return new AverageMetricsAggregator();
            case CountMetricsAggregator.TYPE:
                return new CountMetricsAggregator();
            case MaxMetricsAggregator.TYPE:
                return new MaxMetricsAggregator();
            case MedianMetricsAggregator.TYPE:
                return new MedianMetricsAggregator();
            case MedianAbsoluteDeviationMetricsAggregator.TYPE:
                return new MedianAbsoluteDeviationMetricsAggregator();
            case MinMetricsAggregator.TYPE:
                return new MinMetricsAggregator();
            case PercentileMetricsAggregator.TYPE:
                return new PercentileMetricsAggregator();
            case PercentileRankMetricsAggregator.TYPE:
                return new PercentileRankMetricsAggregator();
            case ScriptedMetricsAggregator.TYPE:
                return new ScriptedMetricsAggregator();
            case SumMetricsAggregator.TYPE:
                return new SumMetricsAggregator();
            case UniqueCountMetricsAggregator.TYPE:
                return new UniqueCountMetricsAggregator();
            default:
                throw new IllegalArgumentException((String) f.get(MetricsAggregator.METRICS_AGGREGATOR_TYPE));
        }
    }

    private static PipelineAggregator createPipelineAggregator(Map<String, Object> f) {
        switch ((String) f.get(PipelineAggregator.PIPELINE_AGGREGATOR_TYPE)) {
            case AveragePipelineAggregator.TYPE:
                return new AveragePipelineAggregator();
            case CumulativeSumPipelineAggregator.TYPE:
                return new CumulativeSumPipelineAggregator();
            case DerivativePipelineAggregator.TYPE:
                return new DerivativePipelineAggregator();
            case MaxPipelineAggregator.TYPE:
                return new MaxPipelineAggregator();
            case MedianPipelineAggregator.TYPE:
                return new MedianPipelineAggregator();
            case MinPipelineAggregator.TYPE:
                return new MinPipelineAggregator();
            case MovingFunctionAggregator.TYPE:
                return new MovingFunctionAggregator();
            case ScriptedPipelineAggregator.TYPE:
                return new ScriptedPipelineAggregator();
            case SerialDifferencingPipelineAggregator.TYPE:
                return new SerialDifferencingPipelineAggregator();
            case SumPipelineAggregator.TYPE:
                return new SumPipelineAggregator();
            default:
                throw new IllegalArgumentException((String) f.get(PipelineAggregator.PIPELINE_AGGREGATOR_TYPE));
        }
    }

}
