package com.newrelic.plugins.oracle;

import com.newrelic.metrics.publish.processors.EpochCounter;

public class MetricData implements IMetricData {

    public final String name;
    public final String metricClass;
    public Number metricCount;
    public final EpochCounter counter;

    public MetricData(String name, String metricClass, Number metricCount) {
        this.name = name;
        this.metricClass = metricClass;
        this.metricCount = metricCount;
        this.counter = new EpochCounter();
    }

}
