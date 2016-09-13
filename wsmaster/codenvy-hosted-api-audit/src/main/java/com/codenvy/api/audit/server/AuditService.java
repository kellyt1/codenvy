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
import org.apache.commons.compress.utils.IOUtils;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;

/**
 * Defines Audit report REST API.
 *
 * @author Igor Vinokur
 */
@Path("/audit")
public class AuditService extends Service {

    private static final Logger LOG = LoggerFactory.getLogger(AuditService.class);

    private final AdminUserDao          adminUserDao;
    private final WorkspaceManager      workspaceManager;
    private final PermissionManager     permissionManager;
    private final CodenvyLicenseManager licenseManager;

    @Inject
    public AuditService(AdminUserDao adminUserDao,
                        WorkspaceManager workspaceManager,
                        PermissionManager permissionManager,
                        CodenvyLicenseManager licenseManager) {
        this.adminUserDao = adminUserDao;
        this.workspaceManager = workspaceManager;
        this.permissionManager = permissionManager;
        this.licenseManager = licenseManager;
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public StreamingOutput getReport() throws ServerException {
        final File tempDir = Files.createTempDir();
        final File file = new File(tempDir, "report.txt");
        printHeader(file);
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
        return output -> {
            output.write(IOUtils.toByteArray(new FileInputStream(file)));
            try {
                FileUtils.deleteDirectory(tempDir);
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        };
    }

    private void printHeader(File file) throws ServerException {
        //TODO: rework to getTotalCount() method from JpaUserDao, when it will be merged to master.
        appendToFile("Number of all users: " + adminUserDao.getAll(1, 0).getTotalItemsCount() + "\n", file);
        final CodenvyLicense license;
        try {
            license = licenseManager.load();
            appendToFile("Number of users licensed: " + license.getNumberOfUsers() + "\n", file);
            appendToFile(
                    "Date when license expires: " + new SimpleDateFormat("dd MMMM yyyy").format(license.getExpirationDate()) + "\n", file);
        } catch (LicenseException e) {
            appendToFile("Failed to retrieve license!\n", file);
            LOG.error(e.getMessage(), e);
        }
    }

    private void printUserInfo(UserImpl user, File file) throws ServerException {
        //Print error if failed to retrieve the list of his workspaces
        List<WorkspaceImpl> workspaces;
        try {
            workspaces = workspaceManager.getWorkspaces(user.getId());
        } catch (ServerException e) {
            appendToFile("Failed to receive list of related workspaces for user " + user.getEmail() +"!\n", file);
            LOG.error(e.getMessage(), e);
            return;
        }

        int workspacesNumber = workspaces.size();
        long ownWorkspacesNumber = workspaces.stream().filter(workspace -> workspace.getNamespace().equals(user.getName())).count();
        appendToFile(user.getEmail() + " is owner of " +
                     ownWorkspacesNumber + " workspace" + (ownWorkspacesNumber > 1 | ownWorkspacesNumber == 0 ? "s" : "") +
                     " and has permissions in " + workspacesNumber + " workspace" +
                     (workspacesNumber > 1 | workspacesNumber == 0 ? "s" : "") + "\n", file);
        for (WorkspaceImpl workspace : workspaces) {
            printUserWorkspaceInfo(workspace, user, file);
        }
    }

    private void printUserWorkspaceInfo(WorkspaceImpl workspace, UserImpl user, File file) throws ServerException {
        appendToFile("   └ " + workspace.getConfig().getName() +
                     ", is owner: " + workspace.getNamespace().equals(user.getName()) + ", permissions: ", file);
        try {
            appendToFile(getWorkspacePermissions(workspace.getId(), user.getId()).getActions().toString() + "\n", file);
        } catch (NotFoundException e) {
            appendToFile("Failed to retrieve workspace permissions! " + e.getMessage() + "\n", file);
        } catch (ConflictException e) {
            LOG.error(e.getMessage(), e);
            appendToFile("Failed to retrieve workspace Id!\n", file);
        }
    }

    private void appendToFile(String row, File file) throws ServerException {
        try {
            Files.append(row, file, Charset.defaultCharset());
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
}
