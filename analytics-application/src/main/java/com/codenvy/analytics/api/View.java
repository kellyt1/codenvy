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
package com.codenvy.analytics.api;

import com.codenvy.analytics.datamodel.ListValueData;
import com.codenvy.analytics.datamodel.MapValueData;
import com.codenvy.analytics.datamodel.ValueData;
import com.codenvy.analytics.metrics.*;
import com.codenvy.analytics.services.view.CSVFileHolder;
import com.codenvy.analytics.services.view.SectionData;
import com.codenvy.analytics.services.view.ViewBuilder;
import com.codenvy.analytics.services.view.ViewData;
import com.codenvy.api.analytics.shared.dto.MetricValueDTO;
import com.codenvy.dto.server.DtoFactory;
import com.codenvy.dto.server.JsonArrayImpl;
import com.codenvy.dto.server.JsonStringMapImpl;
import com.google.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.*;
import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.Map.Entry;

import static com.codenvy.analytics.Utils.toArray;
import static com.codenvy.analytics.datamodel.ValueDataUtil.*;
import static com.codenvy.analytics.metrics.Context.valueOf;
import static com.codenvy.analytics.util.Utils.extractParams;

/**
 * @author Alexander Reshetnyak
 * @author Anatoliy Bazko
 */
@Path("view")
@Singleton
public class View {

    private static final Logger LOG = LoggerFactory.getLogger(View.class);

    private final ViewBuilder   viewBuilder;
    private final CSVFileHolder csvFileCleanerHolder;

    @Inject
    public View(ViewBuilder viewBuilder, CSVFileHolder csvFileCleanerHolder) {
        this.viewBuilder = viewBuilder;
        this.csvFileCleanerHolder = csvFileCleanerHolder;
    }

    @GET
    @Path("username/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public Response getUserNameById(@PathParam("id") String userId,
                                    @Context UriInfo uriInfo,
                                    @Context SecurityContext securityContext) throws IOException {

        try {
            Map<String, String> params = extractParams(uriInfo, securityContext);

            com.codenvy.analytics.metrics.Context context = com.codenvy.analytics.metrics.Context.valueOf(params);
            context = context.cloneAndPut(MetricFilter.USER, userId);

            Metric metric = MetricFactory.getMetric(MetricType.USERS_PROFILES_LIST);
            ListValueData valueData = getAsList(metric, context);

            String userName;
            if (valueData.size() != 0) {
                Map<String, ValueData> profile = treatAsMap(treatAsList(valueData).get(0));
                String[] aliases = toArray(profile.get(AbstractMetric.ALIASES));

                if (aliases.length == 0) {
                    userName = userId;
                } else {
                    userName = aliases[0];
                }
            } else {
                userName = userId;
            }

            Map<String, String> m = new HashMap<>(1);
            m.put(MetricFilter.USER.toString(), userName);

            return Response.status(Response.Status.OK).entity(new JsonStringMapImpl<>(m).toJson()).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("wsname/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public Response getWsNameById(@PathParam("id") String wsId,
                                  @Context UriInfo uriInfo,
                                  @Context SecurityContext securityContext) throws IOException {

        try {
            Map<String, String> params = extractParams(uriInfo, securityContext);

            com.codenvy.analytics.metrics.Context context = com.codenvy.analytics.metrics.Context.valueOf(params);
            context = context.cloneAndPut(MetricFilter.WS, wsId);

            Metric metric = MetricFactory.getMetric(MetricType.WORKSPACES_PROFILES_LIST);
            ListValueData valueData = getAsList(metric, context);

            String wsName;
            if (valueData.size() != 0) {
                Map<String, ValueData> profile = treatAsMap(treatAsList(valueData).get(0));
                ValueData name = profile.get(AbstractMetric.WS_NAME);

                wsName = name == null ? wsId : name.getAsString();
            } else {
                wsName = wsId;
            }

            Map<String, String> m = new HashMap<>(1);
            m.put(MetricFilter.WS.toString(), wsName);

            return Response.status(Response.Status.OK).entity(new JsonStringMapImpl<>(m).toJson()).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("metric/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public Response getMetricValue(@PathParam("name") String metricName,
                                   @QueryParam("page") String page,
                                   @QueryParam("per_page") String perPage,
                                   @Context UriInfo uriInfo,
                                   @Context SecurityContext securityContext) {

        try {
            Map<String, String> context = extractParams(uriInfo,
                                                        page,
                                                        perPage,
                                                        securityContext);

            ValueData value = getMetricValue(metricName, valueOf(context));
            MetricValueDTO outputValue = getMetricValueDTO(metricName, value);
            return Response.status(Response.Status.OK).entity(outputValue).build();
        } catch (MetricNotFoundException e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("metric/{name}/summary")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public Response getSummarizedMetricValue(@PathParam("name") String metricName,
                                             @Context UriInfo uriInfo,
                                             @Context SecurityContext securityContext) {

        try {
            Map<String, String> params = extractParams(uriInfo, securityContext);
            ListValueData value = getSummarizedMetricValue(metricName, valueOf(params));

            Map<String, String> m;
            if (value.size() == 0) {
                m = Collections.emptyMap();
            } else {
                Map<String, ValueData> items = ((MapValueData)value.getAll().get(0)).getAll();
                m = new HashMap<>(items.size());

                for (Entry<String, ValueData> entry : items.entrySet()) {
                    m.put(entry.getKey(), entry.getValue().getAsString());
                }
            }

            return Response.status(Response.Status.OK).entity(new JsonStringMapImpl<>(m).toJson()).build();

        } catch (MetricNotFoundException e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("metric/{name}/expand")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public Response getExpandedMetricValue(@PathParam("name") String metricName,
                                           @QueryParam("page") String page,
                                           @QueryParam("per_page") String perPage,
                                           @Context UriInfo uriInfo,
                                           @Context SecurityContext securityContext) {

        try {
            Map<String, String> context = extractParams(uriInfo,
                                                        page,
                                                        perPage,
                                                        securityContext);

            ValueData value = getExpandedMetricValue(metricName, valueOf(context));
            ViewData result = viewBuilder.getViewData(value);
            String json = transformToJson(result);

            return Response.status(Response.Status.OK).entity(json).build();
        } catch (MetricNotFoundException e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public Response getViewDataAsJson(@PathParam("name") String name,
                                      @Context UriInfo uriInfo,
                                      @Context SecurityContext securityContext) {
        try {
            Map<String, String> params = extractParams(uriInfo,
                                                       securityContext);

            com.codenvy.analytics.metrics.Context context = valueOf(params);

            ViewData result = viewBuilder.getViewData(name, context);
            String json = transformToJson(result);

            return Response.status(Response.Status.OK).entity(json).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("{name}.csv")
    @Produces({"text/csv"})
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public Response getViewDataAsCsv(@PathParam("name") String viewName,
                                     @Context UriInfo uriInfo,
                                     @Context SecurityContext securityContext) {
        try {
            Map<String, String> params = extractParams(uriInfo,
                                                       securityContext);

            com.codenvy.analytics.metrics.Context context = valueOf(params);

            if (context.exists(Parameters.TIME_UNIT)) {
                context = com.codenvy.analytics.Utils.initRowsCountForCSVReport(context);
            }

            ViewData result = viewBuilder.getViewData(viewName, context);
            final File csvFile = csvFileCleanerHolder.createNewFile();
            try (FileOutputStream csvOutputStream = new FileOutputStream(csvFile)) {
                transformToCsv(result, csvOutputStream);
            }

            return Response.status(Response.Status.OK).entity(getStreamingOutput(csvFile)).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    private StreamingOutput getStreamingOutput(final File csvFile) {
        return new StreamingOutput() {
            @Override
            public void write(OutputStream os) throws IOException, WebApplicationException {
                try (FileInputStream csvInputStream = new FileInputStream(csvFile)) {
                    IOUtils.copy(csvInputStream, os);
                } finally {
                    csvFile.delete();
                }
            }
        };
    }

    @GET
    @Path("{name}/expandable-metric-list")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public Response getViewExpandableViewMetricList(@PathParam("name") String viewName) {
        try {
            List<Map<Integer, MetricType>> result = viewBuilder.getViewExpandableMetricMap(viewName);
            String json = transformToJson(result);

            return Response.status(Response.Status.OK).entity(json).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    private ValueData getMetricValue(String metricName, com.codenvy.analytics.metrics.Context context) throws IOException, ParseException {
        context = viewBuilder.initializeTimeInterval(context);
        return MetricFactory.getMetric(metricName).getValue(context);
    }

    private ValueData getExpandedMetricValue(String metricName, com.codenvy.analytics.metrics.Context context) throws IOException,
                                                                                                                      ParseException {
        context = viewBuilder.initializeTimeInterval(context);
        return ((Expandable)MetricFactory.getMetric(metricName)).getExpandedValue(context);
    }

    private ListValueData getSummarizedMetricValue(String metricName, com.codenvy.analytics.metrics.Context context) throws IOException,
                                                                                                                            ParseException {
        context = viewBuilder.initializeTimeInterval(context);
        return (ListValueData)((Summaraziable)MetricFactory.getMetric(metricName)).getSummaryValue(context);
    }

    /**
     * Transforms view data into table in to json format.
     *
     * @return the resulted format will be: [ [ ["section0-row0-column0", "section0-row0-column1", ...]
     * ["section0-row1-column0", "section0-row1-column1", ...] ... ], [ ["section1-row0-column0",
     * "section0-row0-column1", ...] ["section1-row1-column0", "section0-row1-column1", ...] ... ], ... ]
     */
    protected String transformToJson(ViewData data) {
        List<LinkedHashSet<Object>> result = new ArrayList<>(data.size());

        for (Entry<String, SectionData> sectionEntry : data.entrySet()) {
            LinkedHashSet<Object> newSectionData = new LinkedHashSet<>(sectionEntry.getValue().size());

            for (int i = 0; i < sectionEntry.getValue().size(); i++) {
                List<ValueData> rowData = sectionEntry.getValue().get(i);
                List<String> newRowData = new ArrayList<>(rowData.size());

                for (int j = 0; j < rowData.size(); j++) {
                    newRowData.add(rowData.get(j).getAsString());
                }

                newSectionData.add(newRowData);
            }

            result.add(newSectionData);
        }

        return new JsonArrayImpl<>(result).toJson();
    }

    /**
     * Transforms view data into table in to csv format.
     * <p/>
     * the resulted format will be: section0-row0-column0, section0-row0-column1, ... section0-row1-column0,
     * section0-row1-column1, ... ... section1-row0-column0, section0-row0-column1, ... section1-row1-column0,
     * section0-row1-column1, ... ...
     */
    protected void transformToCsv(ViewData data, OutputStream os) throws IOException {
        for (Entry<String, SectionData> sectionEntry : data.entrySet()) {
            for (int i = 0; i < sectionEntry.getValue().size(); i++) {
                List<ValueData> rowData = sectionEntry.getValue().get(i);
                os.write((getCsvRow(rowData) + "\n").getBytes("UTF-8"));
            }
        }
    }

    /**
     * Transforms Map<sectionNumber, Map<rowNumber, metricType>> map into table in to json format.
     *
     * @return the resulted format will be: [
     * {"1": "total_factories",   // first section
     * "2": "created_factories",
     * ...},
     * <p/>
     * {},                        // second section
     * <p/>
     * {"3": "active_workspaces", // third section
     * "5": "active_users",
     * ...},
     * <p/>
     * ...
     * ]
     */
    private String transformToJson(List<Map<Integer, MetricType>> list) {
        if (list.size() == 0) {
            return "[]";
        }

        final String METRIC_ROW_PATTERN = "\"%1$s\":\"%2$s\",";

        StringBuilder result = new StringBuilder("[");

        for (Map<Integer, MetricType> sectionMetrics : list) {
            if (sectionMetrics.isEmpty()) {
                result.append("{},");
            } else {
                result.append("{");

                for (Entry<Integer, MetricType> entry : sectionMetrics.entrySet()) {
                    String rowNumber = entry.getKey().toString();
                    String metricType = entry.getValue().toString().toLowerCase();
                    result.append(String.format(METRIC_ROW_PATTERN, rowNumber, metricType));
                }

                // remove ended ","
                result.deleteCharAt(result.length() - 1);
                result.append("},");
            }
        }

        // remove ended ","
        result.deleteCharAt(result.length() - 1);
        result.append("]");

        return result.toString();
    }

    public String getCsvRow(List<ValueData> data) {
        StringBuilder builder = new StringBuilder();

        for (ValueData valueData : data) {
            if (builder.length() != 0) {
                builder.append(',');
            }

            builder.append('\"');
            builder.append(valueData.getAsString().replace("\"", "\"\""));
            builder.append('\"');
        }

        return builder.toString();
    }

    MetricValueDTO getMetricValueDTO(String metricName, ValueData metricValue) {
        MetricValueDTO metricValueDTO = DtoFactory.getInstance().createDto(MetricValueDTO.class);
        metricValueDTO.setName(metricName);
        metricValueDTO.setType(metricValue.getType());
        metricValueDTO.setValue(metricValue.getAsString());

        return metricValueDTO;
    }
}
