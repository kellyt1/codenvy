/*
 *    Copyright (C) 2013 Codenvy.
 *
 */
package com.codenvy.analytics.metrics;

import java.io.IOException;

import com.codenvy.analytics.metrics.ValueFromMapMetric.ValueType;

/**
 * @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a>
 */
public class ProjectCreatedTypeJavaJarPercentMetric extends AbstractProjectsCreatedMetric {

    ProjectCreatedTypeJavaJarPercentMetric() throws IOException {
        super(MetricType.PROJECT_TYPE_JAVA_JAR_PERCENT, MetricFactory.createMetric(MetricType.PROJECTS_CREATED_LIST), "Jar",
              ValueType.PERCENT);
    }
}
