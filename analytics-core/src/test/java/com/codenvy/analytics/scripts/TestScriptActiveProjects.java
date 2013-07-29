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

package com.codenvy.analytics.scripts;


import static org.testng.Assert.assertTrue;

import com.codenvy.analytics.BaseTest;
import com.codenvy.analytics.metrics.MetricParameter;
import com.codenvy.analytics.metrics.value.ListListStringValueData;
import com.codenvy.analytics.metrics.value.ListStringValueData;
import com.codenvy.analytics.scripts.util.Event;
import com.codenvy.analytics.scripts.util.LogGenerator;

import org.junit.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a> */
public class TestScriptActiveProjects extends BaseTest {

    @Test
    public void testEventFound() throws Exception {
        List<Event> events = new ArrayList<Event>();

        events.add(Event.Builder.createProjectCreatedEvent("user2", "ws2", "session", "project1", "type1").withDate("2010-10-02").build());
        events.add(Event.Builder.createProjectCreatedEvent("user2", "ws2", "session", "project2", "type1").withDate("2010-10-02").build());
        events.add(Event.Builder.createProjectCreatedEvent("user2", "ws3", "session", "project2", "type1").withDate("2010-10-02").build());
        events.add(Event.Builder.createProjectCreatedEvent("user3", "ws2", "session", "project1", "type1").withDate("2010-10-02").build());
        events.add(Event.Builder.createProjectDestroyedEvent("user2", "ws2", "session", "project3", "type1").withDate("2010-10-02").build());

        File log = LogGenerator.generateLog(events);

        Map<String, String> params = new HashMap<String, String>();
        params.put(MetricParameter.FROM_DATE.name(), "20101002");
        params.put(MetricParameter.TO_DATE.name(), "20101002");

        ListListStringValueData valueData = (ListListStringValueData)executeAndReturnResult(ScriptType.ACTIVE_PROJECTS, log, params);
        List<ListStringValueData> all = valueData.getAll();

        Assert.assertEquals(all.size(), 5);
        assertTrue(all.contains(new ListStringValueData(Arrays.asList("ws2", "user2", "project1", "type1"))));
        assertTrue(all.contains(new ListStringValueData(Arrays.asList("ws2", "user2", "project2", "type1"))));
        assertTrue(all.contains(new ListStringValueData(Arrays.asList("ws3", "user2", "project2", "type1"))));
        assertTrue(all.contains(new ListStringValueData(Arrays.asList("ws2", "user3", "project1", "type1"))));
        assertTrue(all.contains(new ListStringValueData(Arrays.asList("ws2", "user2", "project3", "type1"))));
    }
}
