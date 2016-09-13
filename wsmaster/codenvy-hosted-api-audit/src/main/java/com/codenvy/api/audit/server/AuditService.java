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

import org.apache.commons.compress.utils.IOUtils;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.Service;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.FileInputStream;

/**
 * Defines Audit report REST API.
 *
 * @author Igor Vinokur
 */
@Path("/audit")
public class AuditService extends Service {

    private final AuditManager auditManager;

    @Inject
    public AuditService(AuditManager auditManager) {
        this.auditManager = auditManager;
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public StreamingOutput downloadReport() throws ServerException {
        final File report = auditManager.initializeFileReportInTempDirectory();

        auditManager.printAuditReportToFile(report);

        return output -> {
            output.write(IOUtils.toByteArray(new FileInputStream(report)));
            auditManager.deleteReport(report);
        };
    }
}
