/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.analytics.metrics;

import com.codenvy.analytics.BaseTest;
import com.codenvy.analytics.datamodel.DoubleValueData;
import com.codenvy.analytics.datamodel.LongValueData;
import com.codenvy.analytics.pig.scripts.ScriptType;
import com.codenvy.analytics.pig.scripts.util.Event;
import com.codenvy.analytics.pig.scripts.util.LogGenerator;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.codenvy.analytics.datamodel.ValueDataUtil.getAsDouble;
import static com.codenvy.analytics.datamodel.ValueDataUtil.getAsLong;
import static org.testng.AssertJUnit.assertEquals;

/** @author Anatoliy Bazko */
public class TestRunners extends BaseTest {

    @BeforeClass
    public void setUp() throws Exception {
        prepareData();
    }

    @Test
    public void testRunsFinishedByUser() throws Exception {
        Metric metric = MetricFactory.getMetric(MetricType.RUNS_FINISHED_BY_USER);

        LongValueData l = getAsLong(metric, Context.EMPTY);

        assertEquals(l.getAsLong(), 2);
    }

    @Test
    public void testRunsFinishedByTimeout() throws Exception {
        Metric metric = MetricFactory.getMetric(MetricType.RUNS_FINISHED_BY_TIMEOUT);

        LongValueData l = getAsLong(metric, Context.EMPTY);

        assertEquals(l.getAsLong(), 1);
    }

    @Test
    public void testRunsWithAlwaysOn() throws Exception {
        Metric metric = MetricFactory.getMetric(MetricType.RUNS_WITH_ALWAYS_ON);

        LongValueData l = getAsLong(metric, Context.EMPTY);

        assertEquals(l.getAsLong(), 1);
    }

    @Test
    public void testRunsWithTimeout() throws Exception {
        Metric metric = MetricFactory.getMetric(MetricType.RUNS_WITH_TIMEOUT);

        LongValueData l = getAsLong(metric, Context.EMPTY);

        assertEquals(l.getAsLong(), 2);
    }

    @Test
    public void testMemoryUsagePerHour() throws Exception {
        Metric metric = MetricFactory.getMetric(MetricType.RUNS_MEMORY_USAGE_PER_HOUR);

        DoubleValueData d = getAsDouble(metric, Context.EMPTY);

        assertEquals(d.getAsDouble(), 4.5);
    }

    @Test
    public void testMemoryUsage() throws Exception {
        Metric metric = MetricFactory.getMetric(MetricType.RUNS_MEMORY_USAGE);

        LongValueData l = getAsLong(metric, Context.EMPTY);

        assertEquals(l.getAsLong(), 384);
    }

    @Test
    public void testRuns() throws Exception {
        Metric metric = MetricFactory.getMetric(MetricType.RUNS);

        LongValueData l = getAsLong(metric, Context.EMPTY);

        assertEquals(l.getAsLong(), 3);
    }

    @Test
    public void testRunsFinished() throws Exception {
        Metric metric = MetricFactory.getMetric(MetricType.RUNS_FINISHED);

        LongValueData l = getAsLong(metric, Context.EMPTY);

        assertEquals(l.getAsLong(), 3);
    }

    @Test
    public void testRunsTime() throws Exception {
        Metric metric = MetricFactory.getMetric(MetricType.RUNS_TIME);

        LongValueData l = getAsLong(metric, Context.EMPTY);

        assertEquals(l.getAsLong(), 300000);
    }

    private void prepareData() throws Exception {
        Context.Builder builder = new Context.Builder();
        builder.put(Parameters.FROM_DATE, "20131020");
        builder.put(Parameters.TO_DATE, "20131020");
        builder.put(Parameters.LOG, initLogs().getAbsolutePath());

        builder.putAll(scriptsManager.getScript(ScriptType.EVENTS, MetricType.RUNS).getParamsAsMap());
        pigServer.execute(ScriptType.EVENTS, builder.build());

        builder.putAll(scriptsManager.getScript(ScriptType.EVENTS, MetricType.RUNS_FINISHED).getParamsAsMap());
        pigServer.execute(ScriptType.EVENTS, builder.build());

        builder.putAll(scriptsManager.getScript(ScriptType.USED_TIME, MetricType.RUNS_TIME).getParamsAsMap());
        pigServer.execute(ScriptType.USED_TIME, builder.build());
    }

    private File initLogs() throws Exception {
        List<Event> events = new ArrayList<>();

        // #1 2min, stopped by user
        events.add(new Event.Builder().withDate("2013-10-20")
                                      .withTime("10:00:00")
                                      .withParam("EVENT", "run-started")
                                      .withParam("WS", "ws")
                                      .withParam("USER", "user")
                                      .withParam("PROJECT", "project")
                                      .withParam("TYPE", "projectType")
                                      .withParam("ID", "id1")
                                      .withParam("MEMORY", "128")
                                      .withParam("LIFETIME", "1")
                                      .build());
        events.add(new Event.Builder().withDate("2013-10-20")
                                      .withTime("10:10:00")
                                      .withParam("EVENT", "run-finished")
                                      .withParam("WS", "ws")
                                      .withParam("USER", "user")
                                      .withParam("PROJECT", "project")
                                      .withParam("TYPE", "projectType")
                                      .withParam("ID", "id1")
                                      .withParam("MEMORY", "128")
                                      .withParam("LIFETIME", "600")
                                      .withParam("USAGE-TIME", "120000")
                                      .build());

        // #2 1m, stopped by user
        events.add(new Event.Builder().withDate("2013-10-20")
                                      .withTime("11:00:00")
                                      .withParam("EVENT", "run-started")
                                      .withParam("WS", "ws")
                                      .withParam("USER", "user")
                                      .withParam("PROJECT", "project")
                                      .withParam("TYPE", "projectType")
                                      .withParam("ID", "id2")
                                      .withParam("MEMORY", "128")
                                      .withParam("LIFETIME", "-1")
                                      .build());
        events.add(new Event.Builder().withDate("2013-10-20")
                                      .withTime("11:01:00")
                                      .withParam("EVENT", "run-finished")
                                      .withParam("WS", "ws")
                                      .withParam("USER", "user")
                                      .withParam("PROJECT", "project")
                                      .withParam("TYPE", "projectType")
                                      .withParam("ID", "id2")
                                      .withParam("MEMORY", "128")
                                      .withParam("LIFETIME", "-1")
                                      .withParam("USAGE-TIME", "60000")
                                      .build());


        // #3 1m, stopped by timeout
        events.add(new Event.Builder().withDate("2013-10-20")
                                      .withTime("11:00:00")
                                      .withParam("EVENT", "run-started")
                                      .withParam("WS", "ws")
                                      .withParam("USER", "user")
                                      .withParam("PROJECT", "project")
                                      .withParam("TYPE", "projectType")
                                      .withParam("ID", "id3")
                                      .withParam("MEMORY", "128")
                                      .withParam("LIFETIME", "60")
                                      .build());
        events.add(new Event.Builder().withDate("2013-10-20")
                                      .withTime("11:01:00")
                                      .withParam("EVENT", "run-finished")
                                      .withParam("WS", "ws")
                                      .withParam("USER", "user")
                                      .withParam("PROJECT", "project")
                                      .withParam("TYPE", "projectType")
                                      .withParam("ID", "id3")
                                      .withParam("MEMORY", "128")
                                      .withParam("LIFETIME", "60")
                                      .withParam("USAGE-TIME", "120000")
                                      .build());

        return LogGenerator.generateLog(events);
    }
}