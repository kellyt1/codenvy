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
package com.codenvy.analytics.pig.scripts;

import com.codenvy.analytics.BaseTest;
import com.codenvy.analytics.datamodel.LongValueData;
import com.codenvy.analytics.metrics.Context;
import com.codenvy.analytics.metrics.Metric;
import com.codenvy.analytics.metrics.MetricType;
import com.codenvy.analytics.metrics.Parameters;
import com.codenvy.analytics.metrics.sessions.AbstractProductUsage;
import com.codenvy.analytics.metrics.sessions.AbstractProductUsageTime;
import com.codenvy.analytics.metrics.sessions.AbstractProductUsageUsers;
import com.codenvy.analytics.pig.scripts.util.Event;
import com.codenvy.analytics.pig.scripts.util.LogGenerator;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

/** @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a> */
public class TestProductUsageTime extends BaseTest {

    @BeforeClass
    public void prepare() throws Exception {
        List<Event> events = new ArrayList<>();

        events.add(Event.Builder.createUserCreatedEvent("user-id1", "user@gmail.com", "user@gmail.com")
                                .withDate("2013-11-01").withTime("17:00:00").build());
        events.add(Event.Builder.createWorkspaceCreatedEvent("ws1", "wsid1", "user@gmail.com")
                                .withDate("2013-11-01").withTime("17:01:00").build());

        // sessions #1 - 240s
        events.add(Event.Builder.createSessionStartedEvent("ANONYMOUSUSER_user11", "ws1", "ide", "1")
                                .withDate("2013-11-01").withTime("19:00:00").build());
        events.add(Event.Builder.createSessionFinishedEvent("ANONYMOUSUSER_user11", "ws1", "ide", "1")
                                .withDate("2013-11-01").withTime("19:04:00").build());

        // sessions #2 - 300s
        events.add(Event.Builder.createSessionStartedEvent("user@gmail.com", "ws1", "ide", "2").withDate("2013-11-01")
                                .withTime("20:00:00").build());
        events.add(Event.Builder.createSessionFinishedEvent("user@gmail.com", "ws1", "ide", "2").withDate("2013-11-01")
                                .withTime("20:05:00").build());

        // sessions #3 - 120s
        events.add(Event.Builder.createSessionStartedEvent("ANONYMOUSUSER_user11", "ws2", "ide", "3")
                                .withDate("2013-11-01").withTime("18:00:00").build());
        events.add(Event.Builder.createSessionFinishedEvent("ANONYMOUSUSER_user11", "ws2", "ide", "3")
                                .withDate("2013-11-01").withTime("18:02:00").build());

        // by mistake
        events.add(Event.Builder.createSessionFinishedEvent("user@gmail.com", "ws1", "ide", "2").withDate("2013-11-01")
                                .withTime("20:25:00").build());

        // session will be ignored,
        events.add(Event.Builder.createSessionStartedEvent("ANONYMOUSUSER_user11", "tmp-1", "ide", "4")
                                .withDate("2013-11-01").withTime("20:00:00").build());
        events.add(Event.Builder.createSessionFinishedEvent("ANONYMOUSUSER_user11", "tmp-1", "ide", "4")
                                .withDate("2013-11-01").withTime("20:05:00").build());


        File log = LogGenerator.generateLog(events);

        Context.Builder builder = new Context.Builder();
        builder.put(Parameters.FROM_DATE, "20131101");
        builder.put(Parameters.TO_DATE, "20131101");
        builder.put(Parameters.LOG, log.getAbsolutePath());

        builder.putAll(
                scriptsManager.getScript(ScriptType.USERS_PROFILES, MetricType.USERS_PROFILES_LIST).getParamsAsMap());
        pigServer.execute(ScriptType.USERS_PROFILES, builder.build());

        builder.put(Parameters.LOG, log.getAbsolutePath());
        builder.putAll(scriptsManager.getScript(ScriptType.WORKSPACES_PROFILES, MetricType.WORKSPACES_PROFILES_LIST)
                                     .getParamsAsMap());
        pigServer.execute(ScriptType.WORKSPACES_PROFILES, builder.build());

        builder.putAll(scriptsManager.getScript(ScriptType.PRODUCT_USAGE_SESSIONS, MetricType.PRODUCT_USAGE_SESSIONS_LIST).getParamsAsMap());
        pigServer.execute(ScriptType.PRODUCT_USAGE_SESSIONS, builder.build());
    }

    @Test
    public void testDateAndDoubleUserFilterMinIncludeMaxInclude() throws Exception {
        Context.Builder builder = new Context.Builder();
        builder.put(Parameters.FROM_DATE, "20131101");
        builder.put(Parameters.TO_DATE, "20131101");
        builder.put(Parameters.USER, "user-id1 OR anonymoususer_user11");

        Metric metric = new TestAbstractProductUsageTime(240000, 300000, true, true);
        assertEquals(metric.getValue(builder.build()), new LongValueData(840000));

        metric = new TestAbstractProductUsageSessions(240000, 300000, true, true);
        assertEquals(metric.getValue(builder.build()), new LongValueData(3L));

        metric = new TestProductUsageUsers(300000, 360000, true, true);
        assertEquals(metric.getValue(builder.build()), new LongValueData(1L));
    }

    @Test
    public void testDateAndDoubleUserWsFilterMinIncludeMaxInclude() throws Exception {
        Context.Builder builder = new Context.Builder();
        builder.put(Parameters.FROM_DATE, "20131101");
        builder.put(Parameters.TO_DATE, "20131101");
        builder.put(Parameters.USER, "user-id1 OR anonymoususer_user11");
        builder.put(Parameters.WS, "wsid1");

        Metric metric = new TestAbstractProductUsageTime(240000, 300000, true, true);
        assertEquals(metric.getValue(builder.build()), new LongValueData(540000L));

        metric = new TestAbstractProductUsageSessions(240000, 300000, true, true);
        assertEquals(metric.getValue(builder.build()), new LongValueData(2L));

        metric = new TestProductUsageUsers(300000, 360000, true, true);
        assertEquals(metric.getValue(builder.build()), new LongValueData(1L));
    }

    @Test
    public void testDateAndUserFilterMinIncludeMaxInclude() throws Exception {
        Context.Builder builder = new Context.Builder();
        builder.put(Parameters.FROM_DATE, "20131101");
        builder.put(Parameters.TO_DATE, "20131101");
        builder.put(Parameters.USER, "user-id1");

        Metric metric = new TestAbstractProductUsageTime(240000, 300000, true, true);
        assertEquals(metric.getValue(builder.build()), new LongValueData(300000L));

        metric = new TestAbstractProductUsageSessions(240000, 300000, true, true);
        assertEquals(metric.getValue(builder.build()), new LongValueData(1L));

        metric = new TestProductUsageUsers(300000, 360000, true, true);
        assertEquals(metric.getValue(builder.build()), new LongValueData(1L));
    }


    @Test
    public void testDateFilterMinIncludeMaxInclude() throws Exception {
        Context.Builder builder = new Context.Builder();
        builder.put(Parameters.FROM_DATE, "20131101");
        builder.put(Parameters.TO_DATE, "20131101");

        Metric metric = new TestAbstractProductUsageTime(240000, 300000, true, true);
        assertEquals(metric.getValue(builder.build()), new LongValueData(840000));

        metric = new TestAbstractProductUsageSessions(240000, 300000, true, true);
        assertEquals(metric.getValue(builder.build()), new LongValueData(3L));

        metric = new TestProductUsageUsers(300000, 360000, true, true);
        assertEquals(metric.getValue(builder.build()), new LongValueData(1L));
    }

    @Test
    public void testDateFilterMinIncludeMaxExclude() throws Exception {
        Context.Builder builder = new Context.Builder();
        builder.put(Parameters.FROM_DATE, "20131101");
        builder.put(Parameters.TO_DATE, "20131101");

        Metric metric = new TestAbstractProductUsageTime(240000, 300000, true, false);
        assertEquals(metric.getValue(builder.build()), new LongValueData(240000L));

        metric = new TestAbstractProductUsageSessions(240000, 300000, true, false);
        assertEquals(metric.getValue(builder.build()), new LongValueData(1L));

        metric = new TestProductUsageUsers(300000, 360000, true, false);
        assertEquals(metric.getValue(builder.build()), new LongValueData(1L));
    }

    @Test
    public void testDateFilterMinExcludeMaxExclude() throws Exception {
        Context.Builder builder = new Context.Builder();
        builder.put(Parameters.FROM_DATE, "20131101");
        builder.put(Parameters.TO_DATE, "20131101");

        Metric metric = new TestAbstractProductUsageTime(240000, 300000, false, false);
        assertEquals(metric.getValue(builder.build()), new LongValueData(0L));

        metric = new TestAbstractProductUsageSessions(240000, 300000, false, false);
        assertEquals(metric.getValue(builder.build()), new LongValueData(0L));

        metric = new TestProductUsageUsers(300000, 360000, false, false);
        assertEquals(metric.getValue(builder.build()), new LongValueData(0L));
    }

    @Test
    public void testDateFilterMinExcludeMaxInclude() throws Exception {
        Context.Builder builder = new Context.Builder();
        builder.put(Parameters.FROM_DATE, "20131101");
        builder.put(Parameters.TO_DATE, "20131101");

        Metric metric = new TestAbstractProductUsageTime(240000, 300000, false, true);
        assertEquals(metric.getValue(builder.build()), new LongValueData(600000L));

        metric = new TestAbstractProductUsageSessions(240000, 300000, false, true);
        assertEquals(metric.getValue(builder.build()), new LongValueData(2L));

        metric = new TestProductUsageUsers(300000, 360000, false, true);
        assertEquals(metric.getValue(builder.build()), new LongValueData(0L));
    }

    @Test
    public void testDateFilterMinIncludeMaxIncludeNoData() throws Exception {
        Context.Builder builder = new Context.Builder();
        builder.put(Parameters.FROM_DATE, "20131102");
        builder.put(Parameters.TO_DATE, "20131102");

        Metric metric = new TestAbstractProductUsageTime(240000, 300000, true, true);
        assertEquals(metric.getValue(builder.build()), new LongValueData(0L));

        metric = new TestAbstractProductUsageSessions(240000, 300000, true, true);
        assertEquals(metric.getValue(builder.build()), new LongValueData(0L));

        metric = new TestProductUsageUsers(300000, 360000, true, true);
        assertEquals(metric.getValue(builder.build()), new LongValueData(0L));
    }

    public class TestProductUsageUsers extends AbstractProductUsageUsers {

        public TestProductUsageUsers(long min, long max, boolean includeMin, boolean includeMax) {
            super("fake", min, max, includeMin, includeMax);
        }

        @Override
        public String getStorageCollectionName() {
            return MetricType.PRODUCT_USAGE_SESSIONS.toString().toLowerCase();
        }

        @Override
        public String getDescription() {
            return null;
        }
    }


    public class TestAbstractProductUsageTime extends AbstractProductUsageTime {

        public TestAbstractProductUsageTime(long min, long max, boolean includeMin, boolean includeMax) {
            super("fake", min, max, includeMin, includeMax);
        }

        @Override
        public String getStorageCollectionName() {
            return MetricType.PRODUCT_USAGE_SESSIONS.toString().toLowerCase();
        }

        @Override
        public String getDescription() {
            return null;
        }
    }

    private class TestAbstractProductUsageSessions extends AbstractProductUsage {

        public TestAbstractProductUsageSessions(long min, long max, boolean includeMin, boolean includeMax) {
            super("fake", min, max, includeMin, includeMax);
        }

        @Override
        public String getStorageCollectionName() {
            return MetricType.PRODUCT_USAGE_SESSIONS.toString().toLowerCase();
        }

        @Override
        public String getDescription() {
            return null;
        }
    }
}