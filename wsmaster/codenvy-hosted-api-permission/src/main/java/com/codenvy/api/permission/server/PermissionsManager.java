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
package com.codenvy.api.permission.server;

import com.codenvy.api.permission.server.model.impl.AbstractPermissions;
import com.codenvy.api.permission.server.spi.PermissionsDao;
import com.codenvy.api.permission.shared.model.Permissions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.codenvy.api.permission.server.AbstractPermissionsDomain.SET_PERMISSIONS;

/**
 * Facade for Permissions related operations.
 *
 * @author gazarenkov
 * @author Sergii Leschenko
 * @author Anton Korneta
 */
@Singleton
public class PermissionsManager {

    private final List<AbstractPermissionsDomain<? extends AbstractPermissions>> domains;
    private final Map<String, PermissionsDao<? extends AbstractPermissions>>     domainToDao;

    @Inject
    public PermissionsManager(Set<PermissionsDao<? extends AbstractPermissions>> daos) throws ServerException {
        final Map<String, PermissionsDao<? extends AbstractPermissions>> domainToDao = new HashMap<>();
        final List<AbstractPermissionsDomain<? extends AbstractPermissions>> domains = new ArrayList<>();
        for (PermissionsDao<? extends AbstractPermissions> dao : daos) {
            final AbstractPermissionsDomain<? extends AbstractPermissions> domain = dao.getDomain();
            final PermissionsDao<? extends AbstractPermissions> oldStorage = domainToDao.put(domain.getId(), dao);
            domains.add(domain);
            if (oldStorage != null) {
                throw new ServerException("Permissions Domain '" + domain.getId() + "' should be stored in only one storage. " +
                                          "Duplicated in " + dao.getClass() + " and " + oldStorage.getClass());
            }
        }
        this.domains = ImmutableList.copyOf(domains);
        this.domainToDao = ImmutableMap.copyOf(domainToDao);
    }

    /**
     * Stores (adds or updates) permissions.
     *
     * @param permissions
     *         permission to store
     * @throws NotFoundException
     *         when permissions have unsupported domain
     * @throws ConflictException
     *         when new permissions remove last 'setPermissions' of given instance
     * @throws ServerException
     *         when any other error occurs during permissions storing
     */
    public void storePermission(Permissions permissions) throws ServerException, ConflictException, NotFoundException {
        final String domainId = permissions.getDomainId();
        final String instanceId = permissions.getInstanceId();
        final String userId = permissions.getUserId();

        final PermissionsDao<? extends AbstractPermissions> permissionsDao = getPermissionsDao(domainId);
        if (!permissions.getActions().contains(SET_PERMISSIONS)
            && userHasLastSetPermissions(permissionsDao, userId, instanceId)) {
            throw new ConflictException("Can't edit permissions because there is not any another user with permission 'setPermissions'");
        }
        store(permissionsDao, userId, instanceId, permissions);
    }

    private <T extends AbstractPermissions> void store(PermissionsDao<T> dao,
                                                       String userId,
                                                       String instanceId,
                                                       Permissions permissions) throws ConflictException,
                                                                                       ServerException {
        final AbstractPermissionsDomain<T> permissionsDomain = dao.getDomain();
        final T permission = permissionsDomain.newInstance(userId, instanceId, permissions.getActions());

        final Set<String> allowedActions = new HashSet<>(permissionsDomain.getAllowedActions());
        final Set<String> unsupportedActions = permission.getActions()
                                                         .stream()
                                                         .filter(action -> !allowedActions.contains(action))
                                                         .collect(Collectors.toSet());
        if (!unsupportedActions.isEmpty()) {
            throw new ConflictException("Domain with id '" + permissions.getDomainId() + "' doesn't support next action(s): " +
                                        unsupportedActions.stream()
                                                          .collect(Collectors.joining(", ")));
        }

        dao.store(permission);
    }

    /**
     * @param userId
     *         user id
     * @param domainId
     *         domain id
     * @param instanceId
     *         instance id
     * @return userId's permissions for specified instanceId
     * @throws NotFoundException
     *         when given domainId is unsupported
     * @throws NotFoundException
     *         when permissions with given userId and domainId and instanceId was not found
     * @throws ServerException
     *         when any other error occurs during permissions fetching
     */
    public AbstractPermissions get(String userId, String domainId, String instanceId) throws ServerException,
                                                                                             NotFoundException,
                                                                                             ConflictException {
        return getPermissionsDao(domainId).get(userId, instanceId);
    }

    /**
     * @param domainId
     *         domain id
     * @param instanceId
     *         instance id
     * @return set of permissions
     * @throws NotFoundException
     *         when given domainId is unsupported
     * @throws ServerException
     *         when any other error occurs during permissions fetching
     */
    @SuppressWarnings("unchecked")
    public List<AbstractPermissions> getByInstance(String domainId, String instanceId) throws ServerException,
                                                                                              NotFoundException,
                                                                                              ConflictException {
        return (List<AbstractPermissions>)getPermissionsDao(domainId).getByInstance(instanceId);
    }

    /**
     * Removes permissions of userId related to the particular instanceId of specified domainId
     *
     * @param userId
     *         user id
     * @param domainId
     *         domain id
     * @param instanceId
     *         instance id
     * @throws NotFoundException
     *         when given domainId is unsupported
     * @throws ConflictException
     *         when removes last 'setPermissions' of given instanceId
     * @throws ServerException
     *         when any other error occurs during permissions removing
     */
    public void remove(String userId, String domainId, String instanceId) throws ConflictException, ServerException, NotFoundException {
        final PermissionsDao<? extends AbstractPermissions> permissionsDao = getPermissionsDao(domainId);
        if (userHasLastSetPermissions(permissionsDao, userId, instanceId)) {
            throw new ConflictException("Can't remove permissions because there is not any another user with permission 'setPermissions'");
        }
        permissionsDao.remove(userId, instanceId);
    }

    /**
     * @param userId
     *         user id
     * @param domainId
     *         domain id
     * @param instanceId
     *         instance id
     * @param action
     *         action name
     * @return true if the permission exists
     * @throws NotFoundException
     *         when given domainId is unsupported
     * @throws ServerException
     *         when any other error occurs during permission existence checking
     */
    public boolean exists(String userId, String domainId, String instanceId, String action) throws ServerException,
                                                                                                   NotFoundException,
                                                                                                   ConflictException {
        return getDomain(domainId).getAllowedActions().contains(action)
               && getPermissionsDao(domainId).exists(userId, instanceId, action);
    }

    /**
     * Returns supported domains
     */
    public List<AbstractPermissionsDomain> getDomains() {
        return new ArrayList<>(domains);
    }

    /**
     * Returns supported domain
     *
     * @throws NotFoundException
     *         when given domain is unsupported
     */
    public AbstractPermissionsDomain<? extends AbstractPermissions> getDomain(String domain) throws NotFoundException {
        return getPermissionsDao(domain).getDomain();
    }


    private PermissionsDao<? extends AbstractPermissions> getPermissionsDao(String domain) throws NotFoundException {
        final PermissionsDao<? extends AbstractPermissions> permissionsStorage = domainToDao.get(domain);
        if (permissionsStorage == null) {
            throw new NotFoundException("Requested unsupported domain '" + domain + "'");
        }
        return permissionsStorage;
    }

    private boolean userHasLastSetPermissions(PermissionsDao<? extends AbstractPermissions> permissionsStorage,
                                              String userId,
                                              String instanceId) throws ServerException, ConflictException {
        try {
            return permissionsStorage.exists(userId, instanceId, SET_PERMISSIONS)
                   && !permissionsStorage.getByInstance(instanceId)
                                         .stream()
                                         .anyMatch(permission -> !permission.getUserId().equals(userId)
                                                                 && permission.getActions().contains(SET_PERMISSIONS));
        } catch (NotFoundException e) {
            return true;
        }
    }
}
