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
package com.codenvy.analytics.metrics.workspaces;

import com.codenvy.analytics.metrics.AbstractListValueResulted;
import com.codenvy.analytics.metrics.Context;
import com.codenvy.analytics.metrics.MetricType;
import com.codenvy.analytics.metrics.ReadBasedSummariziable;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import javax.annotation.security.RolesAllowed;

/** @author <a href="mailto:abazko@codenvy.com">Anatoliy Bazko</a> */
@RolesAllowed({"system/admin", "system/manager"})
public class WorkspacesStatisticsList extends AbstractListValueResulted implements ReadBasedSummariziable {

    public static final String JOINED_USERS = "joined_users";

    public WorkspacesStatisticsList() {
        super(MetricType.WORKSPACES_STATISTICS_LIST);
    }

    @Override
    public String getDescription() {
        return "Workspaces' statistics data";
    }

    @Override
    public String getStorageCollectionName() {
        return getStorageCollectionName(MetricType.USERS_STATISTICS);
    }

    @Override
    public String[] getTrackedFields() {
        return new String[]{WS,
                            PROJECTS,
                            RUNS,
                            DEBUGS,
                            BUILDS,
                            DEPLOYS,
                            FACTORIES,
                            TIME,
                            SESSIONS,
                            INVITES,
                            RUN_TIME,
                            BUILD_TIME,
                            PAAS_DEPLOYS,
                            JOINED_USERS};
    }

    @Override
    public DBObject[] getSpecificDBOperations(Context clauses) {
        DBObject group = new BasicDBObject();
        group.put(ID, "$" + WS);
        group.put(PROJECTS, new BasicDBObject("$sum", "$" + PROJECTS));
        group.put(RUNS, new BasicDBObject("$sum", "$" + RUNS));
        group.put(DEBUGS, new BasicDBObject("$sum", "$" + DEBUGS));
        group.put(DEPLOYS, new BasicDBObject("$sum", "$" + DEPLOYS));
        group.put(BUILDS, new BasicDBObject("$sum", "$" + BUILDS));
        group.put(FACTORIES, new BasicDBObject("$sum", "$" + FACTORIES));
        group.put(TIME, new BasicDBObject("$sum", "$" + TIME));
        group.put(SESSIONS, new BasicDBObject("$sum", "$" + SESSIONS));
        group.put(INVITES, new BasicDBObject("$sum", "$" + INVITES));
        group.put(RUN_TIME, new BasicDBObject("$sum", "$" + RUN_TIME));
        group.put(BUILD_TIME, new BasicDBObject("$sum", "$" + BUILD_TIME));
        group.put(PAAS_DEPLOYS, new BasicDBObject("$sum", "$" + PAAS_DEPLOYS));
        group.put(JOINED_USERS, new BasicDBObject("$sum", "$" + JOINED_USERS));

        DBObject project = new BasicDBObject();
        project.put(WS, "$" + ID);
        project.put(PROJECTS, "$" + PROJECTS);
        project.put(RUNS, "$" + RUNS);
        project.put(DEBUGS, "$" + DEBUGS);
        project.put(DEPLOYS, "$" + DEPLOYS);
        project.put(BUILDS, "$" + BUILDS);
        project.put(FACTORIES, "$" + FACTORIES);
        project.put(TIME, "$" + TIME);
        project.put(SESSIONS, "$" + SESSIONS);
        project.put(INVITES, "$" + INVITES);
        project.put(RUN_TIME, "$" + RUN_TIME);
        project.put(BUILD_TIME, "$" + BUILD_TIME);
        project.put(PAAS_DEPLOYS, "$" + PAAS_DEPLOYS);
        project.put(JOINED_USERS, "$" + JOINED_USERS);

        return new DBObject[]{new BasicDBObject("$group", group),
                              new BasicDBObject("$project", project)};
    }

    @Override
    public DBObject[] getSpecificSummarizedDBOperations(Context clauses) {
        DBObject[] dbOperations = getSpecificDBOperations(clauses);
        ((DBObject)(dbOperations[0].get("$group"))).put(ID, null);
        ((DBObject)(dbOperations[1].get("$project"))).removeField(WS);

        return dbOperations;
    }
}

