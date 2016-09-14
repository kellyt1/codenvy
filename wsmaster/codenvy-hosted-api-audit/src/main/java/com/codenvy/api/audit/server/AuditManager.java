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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Files;

import org.apache.commons.io.FileUtils;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.Page;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

/**
 * Facade for audit report related operations.
 *
 * @author Igor Vinokur
 */
@Singleton
public class AuditManager {

    private static final Logger LOG = LoggerFactory.getLogger(AuditService.class);

    private final AdminUserDao          adminUserDao;
    private final WorkspaceManager      workspaceManager;
    private final PermissionManager     permissionManager;
    private final CodenvyLicenseManager licenseManager;

    @Inject
    public AuditManager(AdminUserDao adminUserDao,
                        WorkspaceManager workspaceManager,
                        PermissionManager permissionManager,
                        CodenvyLicenseManager licenseManager) {

        //TODO: use getAll() method from JpaUserDao, when it will be merged to master.
        this.adminUserDao = adminUserDao;
        this.workspaceManager = workspaceManager;
        this.permissionManager = permissionManager;
        this.licenseManager = licenseManager;
    }

    /**
     * Creates a file for report in temporary directory inside systems temp directory.
     *
     * @return created file for report
     */
    public File initializeFileReportInTempDirectory() {
        final File tempDir = Files.createTempDir();
        return new File(tempDir, "report.txt");
    }

    /**
     * Prints audit report to given file.
     *
     * @param file file for report
     * @throws ServerException if an error occurs
     */
    public void printAuditReportToFile(File file) throws ServerException {
        try {
            printHeader(file);
            printAllUsersInfo(file);
        } catch (Exception exception) {
            deleteReport(file);
            throw new ServerException(exception.getMessage(), exception);
        }
    }

    private void printHeader(File file) throws ServerException {
        //TODO: rework to getTotalCount() method from JpaUserDao, when it will be merged to master.
        printRowToReport("Number of all users: " + adminUserDao.getAll(1, 0).getTotalItemsCount() + "\n", file);
        final CodenvyLicense license;
        try {
            license = licenseManager.load();
            printRowToReport("Number of users licensed: " + license.getNumberOfUsers() + "\n", file);
            printRowToReport(
                    "Date when license expires: " + new SimpleDateFormat("dd MMMM yyyy").format(license.getExpirationDate()) + "\n", file);
        } catch (LicenseException e) {
            printRowToReport("Failed to retrieve license!\n", file);
            LOG.error(e.getMessage(), e);
        }
    }

    private void printAllUsersInfo(File file) throws ServerException {
        int skipItems = 0;
        while (true) {
            List<UserImpl> users = adminUserDao.getAll(20, skipItems).getItems();
            if (users.size() == 0) {
                break;
            } else {
                skipItems += users.size();
            }
            for (UserImpl user : users) {
                printUserInfo(user, file);
            }
        }
    }

    private void printUserInfo(UserImpl user, File file) throws ServerException {
        //Print error if failed to retrieve the list of his workspaces
        List<WorkspaceImpl> workspaces;
        try {
            workspaces = workspaceManager.getWorkspaces(user.getId());
        } catch (ServerException e) {
            printRowToReport("Failed to receive list of related workspaces for user " + user.getEmail() + "!\n", file);
            LOG.error(e.getMessage(), e);
            return;
        }

        int workspacesNumber = workspaces.size();
        long ownWorkspacesNumber = workspaces.stream().filter(workspace -> workspace.getNamespace().equals(user.getName())).count();
        printRowToReport(user.getEmail() + " is owner of " +
                         ownWorkspacesNumber + " workspace" + (ownWorkspacesNumber > 1 | ownWorkspacesNumber == 0 ? "s" : "") +
                         " and has permissions in " + workspacesNumber + " workspace" +
                         (workspacesNumber > 1 | workspacesNumber == 0 ? "s" : "") + "\n", file);
        for (WorkspaceImpl workspace : workspaces) {
            printUserWorkspaceInfo(workspace, user, file);
        }
    }

    private void printUserWorkspaceInfo(WorkspaceImpl workspace, UserImpl user, File file) throws ServerException {
        printRowToReport("   └ " + workspace.getConfig().getName() +
                         ", is owner: " + workspace.getNamespace().equals(user.getName()) + ", permissions: ", file);
        try {
            printRowToReport(getWorkspacePermissions(workspace.getId(), user.getId()).getActions().toString() + "\n", file);
        } catch (NotFoundException e) {
            printRowToReport("Failed to retrieve workspace permissions! " + e.getMessage() + "\n", file);
        } catch (ConflictException e) {
            LOG.error(e.getMessage(), e);
            printRowToReport("Failed to retrieve workspace Id!\n", file);
        }
    }

    private void printRowToReport(String row, File report) throws ServerException {
        try {
            Files.append(row, report, Charset.defaultCharset());
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new ServerException("Failed to generate audit report. " + e.getMessage(), e);
        }
    }

    private PermissionsImpl getWorkspacePermissions(String workspaceId, String userId)
            throws ServerException, NotFoundException, ConflictException {
        List<PermissionsImpl> permissions;
        permissions = permissionManager.getByInstance("workspace", workspaceId);
        Optional<PermissionsImpl> optional = permissions.stream()
                                                        .filter(wsPermissions -> wsPermissions.getUser().equals(userId))
                                                        .findFirst();

        if (optional.isPresent()) {
            return optional.get();
        }
        else {
            throw new NotFoundException("Permissions for user " + userId + " in workspace " + workspaceId +
                                        " was not found while generating audit report");
        }
    }

    @VisibleForTesting
    void deleteReport(File report) {
        try {
            FileUtils.deleteDirectory(new File(report.getParent()));
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
