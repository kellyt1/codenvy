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
package com.codenvy.api.workspace.server.jpa;

import com.codenvy.api.machine.server.jpa.OnPremisesJpaMachineModule;
import com.codenvy.api.machine.server.recipe.RecipePermissionsImpl;
import com.codenvy.api.permission.server.PermissionsModule;
import com.codenvy.api.permission.server.jpa.SystemPermissionsJpaModule;
import com.codenvy.api.workspace.server.model.impl.WorkerImpl;
import com.codenvy.api.workspace.server.spi.jpa.OnPremisesJpaStackDao;
import com.codenvy.api.workspace.server.spi.jpa.OnPremisesJpaWorkspaceDao;
import com.codenvy.api.workspace.server.stack.StackPermissionsImpl;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.persist.jpa.JpaPersistModule;

import org.eclipse.che.api.core.jdbc.jpa.eclipselink.EntityListenerInjectionManagerInitializer;
import org.eclipse.che.api.core.jdbc.jpa.guice.JpaInitializer;
import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.eclipse.che.api.workspace.server.jpa.JpaStackDao;
import org.eclipse.che.api.workspace.server.jpa.JpaWorkspaceDao;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.eclipse.che.api.workspace.server.model.impl.stack.StackImpl;
import org.eclipse.che.commons.test.tck.repository.JpaTckRepository;
import org.eclipse.che.commons.test.tck.repository.TckRepository;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import javax.persistence.spi.PersistenceUnitTransactionType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_DRIVER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_PASSWORD;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_URL;
import static org.eclipse.persistence.config.PersistenceUnitProperties.JDBC_USER;
import static org.eclipse.persistence.config.PersistenceUnitProperties.TRANSACTION_TYPE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Max Shaposhnik (mshaposhnik@codenvy.com)
 */
public class OnPremisesJpaStackDaoTest {

    private EntityManager manager;

    private OnPremisesJpaStackDao dao;

    StackPermissionsImpl[] permissionses;

    UserImpl[] users;

    StackImpl[] stacks;

    @BeforeClass
    public void setupEntities() throws Exception {
        permissionses = new StackPermissionsImpl[] {new StackPermissionsImpl("user1", "stack1", Arrays.asList("read", "use", "search")),
                                                    new StackPermissionsImpl("user1", "stack2", Arrays.asList("read", "search")),
                                                    new StackPermissionsImpl("*",     "stack3", Arrays.asList("read", "search")),
                                                    new StackPermissionsImpl("user1", "stack4", Arrays.asList("read", "run")),
                                                    new StackPermissionsImpl("user2", "stack1", Arrays.asList("read", "use"))};

        users = new UserImpl[] {new UserImpl("user1", "user1@com.com", "usr1"),
                                new UserImpl("user2", "user2@com.com", "usr2")};

        stacks = new StackImpl[] {
                new StackImpl("stack1", "st1", null, null, null, Arrays.asList("tag1", "tag2"), null, null, null, null),
                new StackImpl("stack2", "st2", null, null, null, null, null, null, null, null),
                new StackImpl("stack3", "st3", null, null, null, Arrays.asList("tag1", "tag2"), null, null, null, null),
                new StackImpl("stack4", "st4", null, null, null, null, null, null, null, null)};

        Injector injector =
                Guice.createInjector(new TestModule(), new OnPremisesJpaMachineModule(), new PermissionsModule(),
                                     new SystemPermissionsJpaModule());
        manager = injector.getInstance(EntityManager.class);
        dao = injector.getInstance(OnPremisesJpaStackDao.class);
    }

    @BeforeMethod
    public void setUp() throws Exception {
        manager.getTransaction().begin();
        for (UserImpl user : users) {
            manager.persist(user);
        }

        for (StackImpl recipe : stacks) {
            manager.persist(recipe);
        }

        for (StackPermissionsImpl recipePermissions : permissionses) {
            manager.persist(recipePermissions);
        }
        manager.getTransaction().commit();
        manager.clear();
    }

    @AfterMethod
    public void cleanup() {
        manager.getTransaction().begin();

        manager.createQuery("SELECT p FROM StackPermissions p", StackPermissionsImpl.class)
               .getResultList()
               .forEach(manager::remove);

        manager.createQuery("SELECT r FROM Stack r", StackImpl.class)
               .getResultList()
               .forEach(manager::remove);

        manager.createQuery("SELECT u FROM Usr u", UserImpl.class)
               .getResultList()
               .forEach(manager::remove);
        manager.getTransaction().commit();
    }

    @AfterClass
    public void shutdown() throws Exception {
        manager.getEntityManagerFactory().close();
    }


    @Test
    public void shouldFindStackByPermissions() throws Exception {
        List<StackImpl> results = dao.searchStacks(users[0].getId(), null, 0, 0);
        assertEquals(results.size(), 3);
        assertTrue(results.contains(stacks[0]));
        assertTrue(results.contains(stacks[1]));
        assertTrue(results.contains(stacks[2]));
    }

    @Test
    public void shouldFindRecipeByPermissionsAndTags() throws Exception {
        List<StackImpl> results = dao.searchStacks(users[0].getId(), Collections.singletonList("tag2"), 0, 0);
        assertEquals(results.size(), 2);
        assertTrue(results.contains(stacks[0]));
        assertTrue(results.contains(stacks[2]));
    }

    @Test
    public void shouldNotFindRecipeByNonexistentTags() throws Exception {
        List<StackImpl> results = dao.searchStacks(users[0].getId(), Collections.singletonList("unexisted_tag2"), 0, 0);
        assertTrue(results.isEmpty());
    }

    private class TestModule extends AbstractModule {

        @Override
        protected void configure() {

            bind(JpaWorkspaceDao.class).to(OnPremisesJpaWorkspaceDao.class);

            bind(new TypeLiteral<TckRepository<StackPermissionsImpl>>() {
            }).toInstance(new JpaTckRepository<>(StackPermissionsImpl.class));
            bind(new TypeLiteral<TckRepository<UserImpl>>() {}).toInstance(new JpaTckRepository<>(UserImpl.class));
            bind(new TypeLiteral<TckRepository<WorkerImpl>>() {}).toInstance(new JpaTckRepository<>(WorkerImpl.class));
            bind(new TypeLiteral<TckRepository<WorkspaceImpl>>() {}).toInstance(new JpaTckRepository<>(WorkspaceImpl.class));

            Map properties = new HashMap();
            if (System.getProperty("jdbc.driver") != null) {
                properties.put(TRANSACTION_TYPE,
                               PersistenceUnitTransactionType.RESOURCE_LOCAL.name());

                properties.put(JDBC_DRIVER, System.getProperty("jdbc.driver"));
                properties.put(JDBC_URL, System.getProperty("jdbc.url"));
                properties.put(JDBC_USER, System.getProperty("jdbc.user"));
                properties.put(JDBC_PASSWORD, System.getProperty("jdbc.password"));
            }
            JpaPersistModule main = new JpaPersistModule("main");
            main.properties(properties);
            install(main);
            bind(JpaInitializer.class).asEagerSingleton();
            bind(EntityListenerInjectionManagerInitializer.class).asEagerSingleton();
        }
    }
}
