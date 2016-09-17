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
package com.codenvy.api.permission.server.jpa;

import com.codenvy.api.permission.server.AbstractPermissionsDomain;
import com.codenvy.api.permission.server.model.impl.AbstractPermissions;
import com.codenvy.api.permission.server.spi.PermissionsDao;
import com.codenvy.api.permission.shared.model.Permissions;
import com.google.inject.persist.Transactional;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.EntityManager;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Basic JPA DAO implementation for {@link Permissions} objects.
 *
 * @author Max Shaposhnik
 */
public abstract class AbstractJpaPermissionsDao<T extends AbstractPermissions> implements PermissionsDao<T> {

    private final AbstractPermissionsDomain<T> supportedDomain;

    @Inject
    protected Provider<EntityManager> managerProvider;

    public AbstractJpaPermissionsDao(AbstractPermissionsDomain<T> supportedDomain) {
        this.supportedDomain = supportedDomain;
    }

    @Override
    public AbstractPermissionsDomain<T> getDomain() {
        return supportedDomain;
    }

    @Override
    public void store(T permissions) throws ServerException {
        requireNonNull(permissions, "Permissions instance required");
        try {
            doCreate(permissions);
        } catch (RuntimeException e) {
            throw new ServerException(e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean exists(String userId, String instanceId, String action) throws ServerException {
        requireNonNull(userId, "User identifier required");
        requireNonNull(action, "Action name required");
        T permissions;
        try {
            permissions = get(userId, instanceId);
        } catch (NotFoundException e) {
            return false;
        }
        return permissions.getActions().contains(action);
    }

    @Override
    public void remove(String userId, String instanceId) throws ServerException, NotFoundException {
        requireNonNull(instanceId, "Instance identifier required");
        requireNonNull(userId, "User identifier required");
        doRemove(userId, instanceId);
    }

    @Override
    public abstract T get(String userId, String instanceId) throws ServerException, NotFoundException;

    @Override
    public abstract List<T> getByUser(String userId) throws ServerException;

    @Override
    public abstract List<T> getByInstance(String instanceId) throws ServerException;

    @Transactional
    protected void doCreate(T permissions) throws ServerException {
        EntityManager manager = managerProvider.get();
        try {
            final T result = get(permissions.getUserId(), permissions.getInstanceId());
            result.getActions().clear();
            result.getActions().addAll(permissions.getActions());
        } catch (NotFoundException n) {
            manager.persist(permissions);
        }
    }

    @Transactional
    protected void doRemove(String userId, String instanceId) throws ServerException, NotFoundException {
        try {
            final T entity = get(userId, instanceId);
            managerProvider.get().remove(entity);
        } catch (RuntimeException e) {
            throw new ServerException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Converts '*' user wildcard to {@code null}
     *
     * @return {@code null} when user identifier equal to '*',
     * either user identifier will be returned
     */
    public static String wildcardToNull(String userId) {
        return !"*".equals(userId) ? userId : null;
    }
}
