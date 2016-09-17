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
import com.codenvy.api.permission.shared.dto.PermissionsDto;
import com.codenvy.api.permission.shared.model.Permissions;
import com.codenvy.api.permission.shared.model.PermissionsDomain;
import com.google.common.collect.ImmutableSet;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.dto.server.DtoFactory;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static com.codenvy.api.permission.server.AbstractPermissionsDomain.SET_PERMISSIONS;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Tests for {@link PermissionsManager}
 *
 * @author Sergii Leschenko
 */
@Listeners(MockitoTestNGListener.class)
public class PermissionsManagerTest {
    @Mock
    private PermissionsDao<TestPermissionsImpl> permissionsDao;

    private PermissionsManager permissionsManager;

    @BeforeMethod
    public void setUp() throws Exception {
        when(permissionsDao.getDomain()).thenReturn(new TestDomain());

        permissionsManager = new PermissionsManager(ImmutableSet.of(permissionsDao));
    }

    @Test(expectedExceptions = ServerException.class,
          expectedExceptionsMessageRegExp = "Permissions Domain 'test' should be stored in only one storage. " +
                                            "Duplicated in class com.codenvy.api.permission.server.spi.PermissionsDao.* and class com.codenvy.api.permission.server.spi.PermissionsDao.*")
    public void shouldThrowExceptionIfThereAreTwoStoragesWhichServeOneDomain() throws Exception {
        @SuppressWarnings("unchecked")
        final PermissionsDao anotherStorage = mock(PermissionsDao.class);
        when(anotherStorage.getDomain()).thenReturn(new TestDomain());

        permissionsManager = new PermissionsManager(ImmutableSet.of(permissionsDao, anotherStorage));
    }

    @Test
    public void shouldBeAbleToStorePermissions() throws Exception {
        final Permissions permissions = DtoFactory.newDto(PermissionsDto.class)
                                                  .withUserId("user")
                                                  .withDomainId("test")
                                                  .withInstanceId("test123")
                                                  .withActions(singletonList(SET_PERMISSIONS));
        permissionsManager.storePermission(permissions);

        verify(permissionsDao).store(new TestDomain().doCreateInstance(permissions.getUserId(),
                                                                       permissions.getDomainId(),
                                                                       permissions.getActions()));
    }

    @Test(expectedExceptions = ConflictException.class,
          expectedExceptionsMessageRegExp = "Can't edit permissions because there is not any another user with permission 'setPermissions'")
    public void shouldNotStorePermissionsWhenItRemoveLastSetPermissions() throws Exception {
        when(permissionsDao.exists("user", "test123", SET_PERMISSIONS)).thenReturn(true);
        when(permissionsDao.getByInstance("test123"))
                .thenReturn(singletonList(new TestPermissionsImpl("user", "test", "test123", singletonList("delete"))));

        permissionsManager.storePermission(new TestPermissionsImpl("user", "test", "test123", singletonList("delete")));
    }

    @Test
    public void shouldNotCheckExistingSetPermissionsIfUserDoesNotHaveItAtAll() throws Exception {
        when(permissionsDao.exists("user", "test123", SET_PERMISSIONS)).thenReturn(false);
        when(permissionsDao.getByInstance("test123"))
                .thenReturn(singletonList(new TestPermissionsImpl("user", "test", "test123", singletonList("delete"))));

        permissionsManager.storePermission(new TestPermissionsImpl("user", "test", "test123", singletonList("delete")));

        verify(permissionsDao, never()).getByInstance(anyString());
    }

    @Test
    public void shouldBeAbleToDeletePermissions() throws Exception {
        permissionsManager.remove("user", "test", "test123");

        verify(permissionsDao).remove(eq("user"), eq("test123"));
    }

    @Test(expectedExceptions = ConflictException.class,
          expectedExceptionsMessageRegExp = "Can't remove permissions because there is not any another user with permission 'setPermissions'")
    public void shouldNotRemovePermissionsWhenItContainsLastSetPermissionsAction() throws Exception {
        when(permissionsDao.exists("user", "test123", SET_PERMISSIONS)).thenReturn(true);
        when(permissionsDao.getByInstance("test123"))
                .thenReturn(singletonList(new TestPermissionsImpl("user", "test", "test123", singletonList("delete"))));

        permissionsManager.remove("user", "test", "test123");
    }

    @Test
    public void shouldNotCheckExistingSetPermissionsIfUserDoesNotHaveItAtAllOnRemove() throws Exception {
        when(permissionsDao.exists("user", "test123", SET_PERMISSIONS)).thenReturn(false);
        when(permissionsDao.getByInstance("test123"))
                .thenReturn(singletonList(new TestPermissionsImpl("user", "test", "test123", singletonList("delete"))));

        permissionsManager.remove("user", "test", "test123");

        verify(permissionsDao, never()).getByInstance(anyString());
    }

    @Test
    public void shouldBeAbleToGetPermissionsByUserAndDomainAndInstance() throws Exception {
        final TestPermissionsImpl permissions = new TestPermissionsImpl("user", "test", "test123", singletonList("read"));
        when(permissionsDao.get("user", "test123")).thenReturn(permissions);

        final Permissions fetchedPermissions = permissionsManager.get("user", "test", "test123");

        assertEquals(permissions, fetchedPermissions);
    }

    @Test
    public void shouldBeAbleToGetPermissionsByInstance() throws Exception {
        final TestPermissionsImpl firstPermissions = new TestPermissionsImpl("user", "test", "test123", singletonList("read"));
        final TestPermissionsImpl secondPermissions = new TestPermissionsImpl("user1", "test", "test123", singletonList("read"));

        when(permissionsDao.getByInstance("test123")).thenReturn(Arrays.asList(firstPermissions, secondPermissions));

        final List<AbstractPermissions> fetchedPermissions = permissionsManager.getByInstance("test", "test123");

        assertEquals(fetchedPermissions.size(), 2);
        assertTrue(fetchedPermissions.contains(firstPermissions));
        assertTrue(fetchedPermissions.contains(secondPermissions));
    }

    @Test
    public void shouldBeAbleToCheckPermissionExistence() throws Exception {
        when(permissionsDao.exists("user", "test123", "use")).thenReturn(true);
        when(permissionsDao.exists("user", "test123", "update")).thenReturn(false);

        assertTrue(permissionsManager.exists("user", "test", "test123", "use"));
        assertFalse(permissionsManager.exists("user", "test", "test123", "update"));
    }

    @Test
    public void shouldBeAbleToDomains() throws Exception {
        final List<AbstractPermissionsDomain> domains = permissionsManager.getDomains();

        assertEquals(domains.size(), 1);
        assertTrue(domains.contains(new TestDomain()));
    }

    @Test
    public void shouldBeAbleToDomainActions() throws Exception {
        final PermissionsDomain testDomain = permissionsManager.getDomain("test");
        final List<String> allowedActions = testDomain.getAllowedActions();

        assertEquals(allowedActions.size(), 5);
        assertTrue(allowedActions.containsAll(ImmutableSet.of(SET_PERMISSIONS, "read", "write", "use", "delete")));
    }

    @Test(expectedExceptions = NotFoundException.class,
          expectedExceptionsMessageRegExp = "Requested unsupported domain 'unsupported'")
    public void shouldThrowExceptionWhenRequestedUnsupportedDomain() throws Exception {
        permissionsManager.getDomain("unsupported");
    }

    public class TestDomain extends AbstractPermissionsDomain<TestPermissionsImpl> {
        public TestDomain() {
            super("test", Arrays.asList("read", "write", "use", "delete"));
        }

        @Override
        protected TestPermissionsImpl doCreateInstance(String userId, String instanceId, List allowedActions) {
            return new TestPermissionsImpl("user", "test", "test123", singletonList(SET_PERMISSIONS));
        }
    }

    public class TestPermissionsImpl extends AbstractPermissions {

        private String domainId;

        private String instanceId;

        @Override
        public String getInstanceId() {
            return instanceId;
        }

        @Override
        public String getDomainId() {
            return domainId;
        }

        public TestPermissionsImpl(String userId, String domainId, String instanceId, List<String> actions) {
            super(userId, actions);
            this.domainId = domainId;
            this.instanceId = instanceId;
        }

    }
}
