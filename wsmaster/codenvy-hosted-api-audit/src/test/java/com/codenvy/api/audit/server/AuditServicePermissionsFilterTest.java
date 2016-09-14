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

import com.codenvy.api.permission.server.SystemDomain;
import com.jayway.restassured.response.Response;

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.subject.Subject;
import org.eclipse.che.dto.server.DtoFactory;
import org.everrest.assured.EverrestJetty;
import org.everrest.core.Filter;
import org.everrest.core.GenericContainerRequest;
import org.everrest.core.RequestFilter;
import org.everrest.core.resource.GenericResourceMethod;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

import static com.codenvy.api.permission.server.SystemDomain.MANAGE_CODENVY_ACTION;
import static com.jayway.restassured.RestAssured.given;
import static org.everrest.assured.JettyHttpServer.ADMIN_USER_NAME;
import static org.everrest.assured.JettyHttpServer.ADMIN_USER_PASSWORD;
import static org.everrest.assured.JettyHttpServer.SECURE_PATH;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * Tests for {@link AuditServicePermissionsFilter}
 *
 * @author Igor Vinokur
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class AuditServicePermissionsFilterTest {
    @SuppressWarnings("unused")
    private static final EnvironmentFilter  FILTER = new EnvironmentFilter();

    @InjectMocks
    AuditServicePermissionsFilter filter;

    @Mock
    private static Subject subject;

    @Mock
    AuditService service;

    @Test
    public void shouldCheckPermissionsByUsersFetching() throws Exception {
        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/octet-stream")
                                         .when()
                                         .get(SECURE_PATH + "/audit");

        assertEquals(response.getStatusCode(), 204);
        verify(service).downloadReport();
        verify(subject).checkPermission(SystemDomain.DOMAIN_ID, null, MANAGE_CODENVY_ACTION);
    }

    @Test(expectedExceptions = ForbiddenException.class,
          expectedExceptionsMessageRegExp = "User is not authorized to perform this operation")
    public void shouldThrowForbiddenExceptionWhenRequestedUnknownMethod() throws Exception {
        final GenericResourceMethod mock = mock(GenericResourceMethod.class);
        Method getServiceDescriptor = AuditService.class.getMethod("getServiceDescriptor");
        when(mock.getMethod()).thenReturn(getServiceDescriptor);

        filter.filter(mock, new Object[] {});
    }

    @Test
    public void shouldThrowForbiddenExceptionWhenUserDoesNotHavePermissionsForAudit() throws Exception {
        doThrow(new ForbiddenException("The user does not have permission to get audit report"))
                .when(subject).checkPermission(SystemDomain.DOMAIN_ID, null, MANAGE_CODENVY_ACTION);

        Response response = given().auth()
                                   .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                   .contentType("application/octet-stream")
                                   .when()
                                   .get(SECURE_PATH + "/audit");

        assertEquals(response.getStatusCode(), 403);
        assertEquals(unwrapError(response), "The user does not have permission to get audit report");

        verifyZeroInteractions(service);
    }

    private static String unwrapError(Response response) {
        return unwrapDto(response, ServiceError.class).getMessage();
    }

    private static <T> T unwrapDto(Response response, Class<T> dtoClass) {
        return DtoFactory.getInstance().createDtoFromJson(response.body().print(), dtoClass);
    }

    @Filter
    public static class EnvironmentFilter implements RequestFilter {
        public void doFilter(GenericContainerRequest request) {
            EnvironmentContext.getCurrent().setSubject(subject);
        }
    }
}
