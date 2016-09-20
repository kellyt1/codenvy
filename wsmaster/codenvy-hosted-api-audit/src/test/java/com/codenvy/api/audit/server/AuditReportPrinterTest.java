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
import com.codenvy.api.permission.server.model.impl.AbstractPermissions;

import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceConfigImpl;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.everrest.assured.EverrestJetty;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;

/**
 * Tests for {@link AuditReportPrinter}
 *
 * @author Igor Vinokur
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class AuditReportPrinterTest {

    private static final String AUDIT_REPORT_HEADER                                               =
            "Number of all users: 2\n" +
            "Number of users licensed: 15\n" +
            "Date when license expires: 01 January 2016\n";
    private static final String AUDIT_REPORT_HEADER_WITHOUT_LICENSE                               =
            "Number of all users: 2\n" +
            "[ERROR] Failed to retrieve license!\n";
    private static final String USER_INFO_WITH_HIS_WORKSPACES_INFO                                =
            "user@email.com is owner of 1 workspace and has permissions in 2 workspaces\n" +
            "   └ Workspace1Name, is owner: true, permissions: [read, use, run, configure, setPermissions, delete]\n" +
            "   └ Workspace2Name, is owner: false, permissions: [read, use, run, configure, setPermissions]\n";
    private static final String USER_INFO_WITH_HIS_WORKSPACES_INFO_WITHOUT_WORKSPACE2_PERMISSIONS =
            "user@email.com is owner of 1 workspace and has permissions in 2 workspaces\n" +
            "   └ Workspace1Name, is owner: true, permissions: [read, use, run, configure, setPermissions, delete]\n" +
            "   └ Workspace2Name, is owner: false, permissions: [ERROR] Failed to retrieve workspace permissions!\n";
    private static final String GET_USER_WORKSPACES_ERROR                                         =
            "[ERROR] Failed to receive list of related workspaces for user User1Id!\n";


    @Mock
    UserImpl user;
    private AuditReportPrinter  auditReportPrinter;
    @Mock
    private WorkspaceImpl       workspace1;
    @Mock
    private WorkspaceImpl       workspace2;
    @Mock
    private AbstractPermissions ws1User1Permissions;
    @Mock
    private AbstractPermissions ws2User1Permissions;

    private Path auditReport;


    @BeforeMethod
    public void setup() throws Exception {
        //User
        user = mock(UserImpl.class);
        when(user.getEmail()).thenReturn("user@email.com");
        when(user.getId()).thenReturn("User1Id");
        when(user.getName()).thenReturn("User1");
        //Workspace config
        WorkspaceConfigImpl ws1config = mock(WorkspaceConfigImpl.class);
        WorkspaceConfigImpl ws2config = mock(WorkspaceConfigImpl.class);
        when(ws1config.getName()).thenReturn("Workspace1Name");
        when(ws2config.getName()).thenReturn("Workspace2Name");
        //Workspace
        workspace1 = mock(WorkspaceImpl.class);
        workspace2 = mock(WorkspaceImpl.class);
        when(workspace1.getNamespace()).thenReturn("User1");
        when(workspace2.getNamespace()).thenReturn("User2");
        when(workspace1.getId()).thenReturn("Workspace1Id");
        when(workspace2.getId()).thenReturn("Workspace2Id");
        when(workspace1.getConfig()).thenReturn(ws1config);
        when(workspace2.getConfig()).thenReturn(ws2config);
        //Permissions
        ws1User1Permissions = mock(AbstractPermissions.class);
        ws2User1Permissions = mock(AbstractPermissions.class);
        when(ws1User1Permissions.getUserId()).thenReturn("User1Id");
        when(ws2User1Permissions.getUserId()).thenReturn("User1Id");
        when(ws1User1Permissions.getActions()).thenReturn(asList("read", "use", "run", "configure", "setPermissions", "delete"));
        when(ws2User1Permissions.getActions()).thenReturn(asList("read", "use", "run", "configure", "setPermissions"));

        auditReport = Files.createTempFile("report", ".txt");

        auditReportPrinter = new AuditReportPrinter();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        Files.delete(auditReport);
    }

    @Test
    public void shouldWriteAuditReportHeaderToFile() throws Exception {
        //given
        CodenvyLicense license = mock(CodenvyLicense.class);
        when(license.getNumberOfUsers()).thenReturn(15);
        when(license.getExpirationDate()).thenReturn(new SimpleDateFormat("dd MMMM yyyy").parse("01 January 2016"));

        //when
        auditReportPrinter.printHeader(auditReport, 2, license);

        //then
        assertEquals(AUDIT_REPORT_HEADER, readFileToString(auditReport.toFile()));
    }

    @Test
    public void shouldWriteAuditReportHeaderToFileWithoutLicenseInfo() throws Exception {
        //when
        auditReportPrinter.printHeader(auditReport, 2, null);

        //then
        assertEquals(AUDIT_REPORT_HEADER_WITHOUT_LICENSE, readFileToString(auditReport.toFile()));
    }

    @Test
    public void shouldWriteUserInfoWithHisWorkspacesInfoToFile() throws Exception {
        //given
        Map<String, List<AbstractPermissions>> map = new HashMap<>();
        map.put("Workspace1Id", singletonList(ws1User1Permissions));
        map.put("Workspace2Id", singletonList(ws2User1Permissions));

        //when
        auditReportPrinter.printUserInfoWithHisWorkspacesInfo(auditReport,
                                                              user,
                                                              asList(workspace1, workspace2),
                                                              map);

        //then
        assertEquals(USER_INFO_WITH_HIS_WORKSPACES_INFO, readFileToString(auditReport.toFile()));
    }

    @Test
    public void shouldWriteUserInfoWithHisWorkspacesInfoWithoutWSToFileWithoutWorkspace1Permissions() throws Exception {
        //given
        Map<String, List<AbstractPermissions>> map = new HashMap<>();
        map.put("Workspace1Id", singletonList(ws1User1Permissions));
        map.put("Workspace2Id", emptyList());

        //when
        auditReportPrinter.printUserInfoWithHisWorkspacesInfo(auditReport,
                                                              user,
                                                              asList(workspace1, workspace2),
                                                              map);

        //then
        assertEquals(USER_INFO_WITH_HIS_WORKSPACES_INFO_WITHOUT_WORKSPACE2_PERMISSIONS, readFileToString(auditReport.toFile()));
    }

    @Test
    public void shouldPrintErrorIfWorkspacesListIsNull() throws Exception {
        //when
        auditReportPrinter.printUserInfoWithHisWorkspacesInfo(auditReport,
                                                              user,
                                                              null,
                                                              emptyMap());

        //then
        assertEquals(GET_USER_WORKSPACES_ERROR, readFileToString(auditReport.toFile()));
    }
}
