/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */

package com.codenvy.analytics.metrics;

import com.codenvy.analytics.metrics.value.ListListStringValueData;
import com.codenvy.analytics.metrics.value.MapStringLongValueData;
import com.codenvy.analytics.metrics.value.ValueData;
import com.codenvy.analytics.metrics.value.filters.ProjectsDeployedFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a>
 */
public class PaasDeploymentTypesMetric extends CalculateBasedMetric {

    private final Metric basedMetric;
    
    PaasDeploymentTypesMetric() {
        super(MetricType.PAAS_DEPLOYMENT_TYPES);
        this.basedMetric = MetricFactory.createMetric(MetricType.PROJECTS_DEPLOYED_LIST);
    }

    /** {@inheritDoc} */
    @Override
    public Set<MetricParameter> getParams() {
        return basedMetric.getParams();
    }

    /** {@inheritDoc} */
    @Override
    protected Class< ? extends ValueData> getValueDataClass() {
        return MapStringLongValueData.class;
    }

    /** {@inheritDoc} */
    @Override
    protected ValueData evaluate(Map<String, String> context) throws IOException {
        ListListStringValueData listVD = (ListListStringValueData)basedMetric.getValue(context);

        ProjectsDeployedFilter filter = new ProjectsDeployedFilter(listVD);
        filter = new ProjectsDeployedFilter(filter.getUniqueProjects());
        
        return new MapStringLongValueData(filter.sizeOfGroups(MetricFilter.FILTER_PROJECT_PAAS));
    }
}
