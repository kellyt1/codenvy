/*
 *  [2012] - [2016] Codenvy, S.A.
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
package com.codenvy.api.workspace.server.stack;

import com.codenvy.api.permission.server.AbstractPermissionsDomain;
import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Domain for storing stacks' permissions
 *
 * @author Sergii Leschenko
 */
public class StackDomain extends AbstractPermissionsDomain<StackPermissionsImpl> {
    public static final String DOMAIN_ID = "stack";

    public static final String READ   = "read";
    public static final String SEARCH = "search";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";

    public StackDomain() {
        super(DOMAIN_ID, ImmutableList.of(READ,
                                          SEARCH,
                                          UPDATE,
                                          DELETE));
    }

    @Override
    public StackPermissionsImpl doCreateInstance(String userId, String instanceId, List<String> allowedActions) {
        return new StackPermissionsImpl(userId, instanceId, allowedActions);
    }
}
