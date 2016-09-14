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
package com.codenvy.api.audit.server;

import com.codenvy.api.license.CodenvyLicense;
import com.codenvy.api.license.LicenseException;
import com.codenvy.api.license.server.CodenvyLicenseManager;
import com.codenvy.api.permission.server.PermissionManager;
import com.codenvy.api.permission.server.PermissionsImpl;
import com.codenvy.api.user.server.dao.AdminUserDao;
import com.google.common.io.Files;

import org.apache.commons.io.FileUtils;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.Page;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceConfigImpl;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.everrest.assured.EverrestJetty;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;
import java.text.SimpleDateFormat;

import static com.jayway.restassured.RestAssured.given;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * Tests for {@link AuditService}.
 *
 * @author Igor Vinokur
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class AuditManagerTest {

    private static final String FULL_AUDIT_REPORT =
            "Number of all users: 2\n" +
            "Number of users licensed: 15\n" +
            "Date when license expires: 01 January 2016\n" +
            "user1@email.com is owner of 1 workspace and has permissions in 2 workspaces\n" +
            "   └ Workspace1Name, is owner: true, permissions: [read, use, run, configure, setPermissions, delete]\n" +
            "   └ Workspace2Name, is owner: false, permissions: [read, use, run, configure, setPermissions]\n" +
            "user2@email.com is owner of 1 workspace and has permissions in 1 workspace\n" +
            "   └ Workspace2Name, is owner: true, permissions: [read, use, run, configure, setPermissions, delete]\n";
    private static final String AUDIT_REPORT_WITHOUT_LICENSE =
            "Number of all users: 2\n" +
            "Failed to retrieve license!\n" +
            "user1@email.com is owner of 1 workspace and has permissions in 2 workspaces\n" +
            "   └ Workspace1Name, is owner: true, permissions: [read, use, run, configure, setPermissions, delete]\n" +
            "   └ Workspace2Name, is owner: false, permissions: [read, use, run, configure, setPermissions]\n" +
            "user2@email.com is owner of 1 workspace and has permissions in 1 workspace\n" +
            "   └ Workspace2Name, is owner: true, permissions: [read, use, run, configure, setPermissions, delete]\n";
    private static final String AUDIT_REPORT_WITHOUT_USER_WORKSPACES =
            "Number of all users: 2\n" +
            "Number of users licensed: 15\n" +
            "Date when license expires: 01 January 2016\n" +
            "Failed to receive list of related workspaces for user user1@email.com!\n" +
            "user2@email.com is owner of 1 workspace and has permissions in 1 workspace\n" +
            "   └ Workspace2Name, is owner: true, permissions: [read, use, run, configure, setPermissions, delete]\n";
    private static final String AUDIT_REPORT_WITHOUT_WORKSPACE_PERMISSIONS =
            "Number of all users: 2\n" +
            "Number of users licensed: 15\n" +
            "Date when license expires: 01 January 2016\n" +
            "user1@email.com is owner of 1 workspace and has permissions in 2 workspaces\n" +
            "   └ Workspace1Name, is owner: true, permissions: [read, use, run, configure, setPermissions, delete]\n" +
            "   └ Workspace2Name, is owner: false, permissions: Failed to retrieve workspace permissions! Permissions was not found\n" +
            "user2@email.com is owner of 1 workspace and has permissions in 1 workspace\n" +
            "   └ Workspace2Name, is owner: true, permissions: Failed to retrieve workspace permissions! Permissions was not found\n";
    private static final String AUDIT_REPORT_WITHOUT_WORKSPACE_ID =
            "Number of all users: 2\n" +
            "Number of users licensed: 15\n" +
            "Date when license expires: 01 January 2016\n" +
            "user1@email.com is owner of 1 workspace and has permissions in 2 workspaces\n" +
            "   └ Workspace1Name, is owner: true, permissions: [read, use, run, configure, setPermissions, delete]\n" +
            "   └ Workspace2Name, is owner: false, permissions: Failed to retrieve workspace Id!\n" +
            "user2@email.com is owner of 1 workspace and has permissions in 1 workspace\n" +
            "   └ Workspace2Name, is owner: true, permissions: Failed to retrieve workspace Id!\n";

    private File report;

    @Mock
    private AdminUserDao          adminUserDao;
    @Mock
    private WorkspaceManager      workspaceManager;
    @Mock
    private PermissionManager     permissionManager;
    @Mock
    private CodenvyLicenseManager licenseManager;
    @InjectMocks
    private AuditManager          auditManager;

    @BeforeMethod
    public void setUp() throws Exception {
        //License
        CodenvyLicense license = mock(CodenvyLicense.class);
        when(license.getNumberOfUsers()).thenReturn(15);
        when(license.getExpirationDate()).thenReturn(new SimpleDateFormat("dd MMMM yyyy").parse("01 January 2016"));
        when(licenseManager.load()).thenReturn(license);
        //User
        UserImpl user1 = mock(UserImpl.class);
        UserImpl user2 = mock(UserImpl.class);
        when(user1.getEmail()).thenReturn("user1@email.com");
        when(user2.getEmail()).thenReturn("user2@email.com");
        when(user1.getId()).thenReturn("User1Id");
        when(user2.getId()).thenReturn("User2Id");
        when(user1.getName()).thenReturn("User1");
        when(user2.getName()).thenReturn("User2");
        //Workspace config
        WorkspaceConfigImpl ws1config = mock(WorkspaceConfigImpl.class);
        WorkspaceConfigImpl ws2config = mock(WorkspaceConfigImpl.class);
        when(ws1config.getName()).thenReturn("Workspace1Name");
        when(ws2config.getName()).thenReturn("Workspace2Name");
        //Workspace
        WorkspaceImpl workspace1 = mock(WorkspaceImpl.class);
        WorkspaceImpl workspace2 = mock(WorkspaceImpl.class);
        when(workspace1.getNamespace()).thenReturn("User1");
        when(workspace2.getNamespace()).thenReturn("User2");
        when(workspace1.getId()).thenReturn("Workspace1Id");
        when(workspace2.getId()).thenReturn("Workspace2Id");
        when(workspace1.getConfig()).thenReturn(ws1config);
        when(workspace2.getConfig()).thenReturn(ws2config);
        when(workspaceManager.getWorkspaces("User1Id")).thenReturn(asList(workspace1, workspace2));
        when(workspaceManager.getWorkspaces("User2Id")).thenReturn(singletonList(workspace2));
        //Permissions
        PermissionsImpl ws1User1Permissions = mock(PermissionsImpl.class);
        PermissionsImpl ws2User1Permissions = mock(PermissionsImpl.class);
        PermissionsImpl ws2User2Permissions = mock(PermissionsImpl.class);
        when(ws1User1Permissions.getUser()).thenReturn("User1Id");
        when(ws2User1Permissions.getUser()).thenReturn("User1Id");
        when(ws2User2Permissions.getUser()).thenReturn("User2Id");
        when(ws1User1Permissions.getActions()).thenReturn(asList("read", "use", "run", "configure", "setPermissions", "delete"));
        when(ws2User1Permissions.getActions()).thenReturn(asList("read", "use", "run", "configure", "setPermissions"));
        when(ws2User2Permissions.getActions()).thenReturn(asList("read", "use", "run", "configure", "setPermissions", "delete"));
        when(permissionManager.getByInstance(anyString(), eq("Workspace1Id"))).thenReturn(singletonList(ws1User1Permissions));
        when(permissionManager.getByInstance(anyString(), eq("Workspace2Id"))).thenReturn(asList(ws2User1Permissions, ws2User2Permissions));
        //Page
        Page page = mock(Page.class);
        Page emptyPage = mock(Page.class);
        when(page.getItems()).thenReturn(asList(user1, user2));
        when(page.getTotalItemsCount()).thenReturn(2L);
        when(emptyPage.getItems()).thenReturn(emptyList());
        when(adminUserDao.getAll(1, 0)).thenReturn(page);
        when(adminUserDao.getAll(20, 0)).thenReturn(page);
        when(adminUserDao.getAll(20, 2)).thenReturn(emptyPage);

        report = new File(Files.createTempDir(), "report.txt");
    }
    @AfterMethod
    public void cleanTempDirectory() throws Exception {
        auditManager.deleteReport(report);
    }

    @Test
    public void shouldReturnFullAuditReport() throws Exception {
        //when
        auditManager.printAuditReportToFile(report);

        //then
        assertEquals(FileUtils.readFileToString(report), FULL_AUDIT_REPORT);
    }

    @Test
    public void shouldReturnAuditReportWithoutLicenseInfoIfFailedToRetrieveLicense() throws Exception {
        //given
        when(licenseManager.load()).thenThrow(new LicenseException("Failed to retrieve license info"));

        //when
        auditManager.printAuditReportToFile(report);

        //then
        assertEquals(FileUtils.readFileToString(report), AUDIT_REPORT_WITHOUT_LICENSE);
    }

    @Test
    public void shouldReturnAuditReportWithoutUserWorkspacesIfFailedToRetrieveTheListOfHisWorkspaces() throws Exception {
        //given
        when(workspaceManager.getWorkspaces(eq("User1Id"))).thenThrow(new ServerException("Failed to retrieve workspaces"));

        //when
        auditManager.printAuditReportToFile(report);

        //then
        assertEquals(FileUtils.readFileToString(report), AUDIT_REPORT_WITHOUT_USER_WORKSPACES);
    }

    @Test
    public void shouldReturnAuditReportWithoutWorkspacePermissionsIfFailedToRetrievePermissionsOfTheWorkspaces() throws Exception {
        //given
        when(permissionManager.getByInstance(eq("workspace"), eq("Workspace2Id")))
                .thenThrow(new NotFoundException("Permissions was not found"));

        //when
        auditManager.printAuditReportToFile(report);

        //then
        assertEquals(FileUtils.readFileToString(report), AUDIT_REPORT_WITHOUT_WORKSPACE_PERMISSIONS);
    }

    @Test
    public void shouldReturnAuditReportWithoutWorkspacePermissionsIfFailedToRetrieveWorkspaceId() throws Exception {
        //given
        when(permissionManager.getByInstance(eq("workspace"), eq("Workspace2Id")))
                .thenThrow(new ConflictException("Failed to retrieve workspace Id"));

        //when
        auditManager.printAuditReportToFile(report);

        //then
        assertEquals(FileUtils.readFileToString(report), AUDIT_REPORT_WITHOUT_WORKSPACE_ID);
    }
}
