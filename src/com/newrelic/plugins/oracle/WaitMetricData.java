package com.newrelic.plugins.oracle;

import com.newrelic.metrics.publish.processors.EpochCounter;

public class WaitMetricData extends MetricData {
    public Number waitTime;
    public final EpochCounter waitedTime;

    public WaitMetricData(String name, String waitClass, Number count, Number waitTime) {
        super(name, waitClass, count);
        this.waitTime = waitTime;
        this.waitedTime = new EpochCounter();
    }

}
