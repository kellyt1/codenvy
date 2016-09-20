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

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.io.Files.append;

/**
 * Audit report printer.
 *
 * @author Igor Vinokur
 */
@Singleton
class AuditReportPrinter {

    private static final Logger LOG = LoggerFactory.getLogger(AuditService.class);

    /**
     * Prints header of audit report in format:
     * <p>
     * Number of all users: <value>
     * Number of users licensed: <value>
     * Date when license expires: <date>
     * <p>
     * If license is not defined it prints error message instead license rows.
     *
     * @param auditReport
     *         file of audit report
     * @param allUsersNumber
     *         all users number
     * @param license
     *         {@link CodenvyLicense} object
     * @throws ServerException
     *         if an error occurs
     */
    void printHeader(Path auditReport, int allUsersNumber, CodenvyLicense license) throws ServerException {
        printRow("Number of all users: " + allUsersNumber + "\n", auditReport);
        if (license != null) {
            printRow("Number of users licensed: " + license.getNumberOfUsers() + "\n", auditReport);
            printRow("Date when license expires: " + new SimpleDateFormat("dd MMMM yyyy").format(license.getExpirationDate()) + "\n",
                     auditReport);
        } else {
            printError("Failed to retrieve license", auditReport);
        }
    }

    /**
     * Print users info and his related workspaces info in format:
     * <p>
     * <user e-mail> is owner of <own workspaces number> workspace(s) and has permissions in <workspaces number> workspace(s)
     * └ <workspace name>, is owner: <true or false>, permissions: <list of permissions for current user>
     * .
     * .
     * .
     * └ <workspace name>, is owner: <true or false>, permissions: <list of permissions for current user>
     * <p>
     * If the list of user workspaces is not defined it prints error message.
     *
     * @param auditReport
     *         file of audit report
     * @param user
     *         {@link UserImpl} object
     * @param workspaces
     *         list of workspaces that are related to given user
     * @param wsPermissions
     *         map of permissions to workspaces
     * @throws ServerException
     *         if an error occurs
     */
    void printUserInfoWithHisWorkspacesInfo(Path auditReport, UserImpl user, List<WorkspaceImpl> workspaces,
                                            Map<String, List<AbstractPermissions>> wsPermissions)
            throws ServerException {

        if (workspaces == null) {
            printError("Failed to receive list of related workspaces for user " + user.getId(), auditReport);
            return;
        }

        int workspacesNumber = workspaces.size();
        long ownWorkspacesNumber = workspaces.stream().filter(workspace -> workspace.getNamespace().equals(user.getName())).count();
        printRow(user.getEmail() + " is owner of " +
                 ownWorkspacesNumber + " workspace" + (ownWorkspacesNumber > 1 | ownWorkspacesNumber == 0 ? "s" : "") +
                 " and has permissions in " + workspacesNumber + " workspace" +
                 (workspacesNumber > 1 | workspacesNumber == 0 ? "s" : "") + "\n", auditReport);
        for (WorkspaceImpl workspace : workspaces) {
            printUserWorkspaceInfo(workspace, user, wsPermissions, auditReport);
        }
    }

    /**
     * Prints error in format:
     * [ERROR] <error text>!
     *
     * @param error
     *         text of error
     * @param auditReport
     *         file of audit report
     * @throws ServerException
     *         if an error occurs
     */
    void printError(String error, Path auditReport) throws ServerException {
        printRow("[ERROR] " + error + "!\n", auditReport);
    }

    private void printUserWorkspaceInfo(WorkspaceImpl workspace, UserImpl user, Map<String, List<AbstractPermissions>> wsPermissions,
                                        Path auditReport)
            throws ServerException {
        printRow("   └ " + workspace.getConfig().getName() +
                 ", is owner: " + workspace.getNamespace().equals(user.getName()) + ", permissions: ", auditReport);
        try {
            String workspaceId = workspace.getId();
            printRow(
                    getWorkspacePermissionsOfUser(workspaceId, user.getId(), wsPermissions.get(workspaceId)).getActions().toString() + "\n",
                    auditReport);
        } catch (NotFoundException e) {
            LOG.error(e.getMessage(), e);
            printError("Failed to retrieve workspace permissions", auditReport);
        }
    }

    private void printRow(String row, Path auditReport) throws ServerException {
        try {
            append(row, auditReport.toFile(), Charset.defaultCharset());
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new ServerException("Failed to generate audit report. " + e.getMessage(), e);
        }
    }

    private AbstractPermissions getWorkspacePermissionsOfUser(String workspaceId, String userId, List<AbstractPermissions> permissions)
            throws ServerException, NotFoundException {

        Optional<AbstractPermissions> optional = permissions.stream()
                                                            .filter(wsPermissions -> wsPermissions.getUserId().equals(userId))
                                                            .findFirst();

        if (optional.isPresent()) {
            return optional.get();
        } else {
            throw new NotFoundException("Permissions for user " + userId + " in workspace " + workspaceId +
                                        " was not found while generating audit report");
        }
    }
}
