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
package com.codenvy.api.workspace.server.filters;

import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.rest.ApiExceptionMapper;
import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.api.workspace.server.WorkspaceService;
import org.eclipse.che.api.workspace.server.stack.StackService;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.subject.Subject;
import org.eclipse.che.dto.server.DtoFactory;
import org.everrest.assured.EverrestJetty;
import org.everrest.core.Filter;
import org.everrest.core.GenericContainerRequest;
import org.everrest.core.RequestFilter;
import org.everrest.core.resource.GenericResourceMethod;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.lang.reflect.Method;

import static com.codenvy.api.workspace.server.stack.StackDomain.DELETE;
import static com.codenvy.api.workspace.server.stack.StackDomain.READ;
import static com.codenvy.api.workspace.server.stack.StackDomain.UPDATE;
import static com.jayway.restassured.RestAssured.given;
import static org.everrest.assured.JettyHttpServer.ADMIN_USER_NAME;
import static org.everrest.assured.JettyHttpServer.ADMIN_USER_PASSWORD;
import static org.everrest.assured.JettyHttpServer.SECURE_PATH;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * Tests for {@link StackPermissionsFilter}
 *
 * @author Sergii Leschenko
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class StackPermissionsFilterTest {
    @SuppressWarnings("unused")
    private static final ApiExceptionMapper MAPPER = new ApiExceptionMapper();
    @SuppressWarnings("unused")
    private static final EnvironmentFilter  FILTER = new EnvironmentFilter();

    @SuppressWarnings("unused")
    @InjectMocks
    StackPermissionsFilter permissionsFilter;

    @Mock
    private static Subject subject;

    @Mock
    StackService service;

    @Test
    public void shouldNotCheckPermissionsOnStackCreating() throws Exception {
        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .when()
                                         .post(SECURE_PATH + "/stack");

        assertEquals(response.getStatusCode(), 204);
        verify(service).createStack(any());
        verifyZeroInteractions(subject);
    }

    @Test
    public void shouldCheckPermissionsOnStackReading() throws Exception {
        when(subject.hasPermission("stack", "stack123", READ)).thenReturn(true);

        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .when()
                                         .get(SECURE_PATH + "/stack/stack123");

        assertEquals(response.getStatusCode(), 204);
        verify(service).getStack("stack123");
        verify(subject).hasPermission(eq("stack"), eq("stack123"), Matchers.eq(READ));
    }

    @Test
    public void shouldCheckPermissionsOnStackUpdating() throws Exception {
        when(subject.hasPermission("stack", "stack123", UPDATE)).thenReturn(true);

        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .when()
                                         .put(SECURE_PATH + "/stack/stack123");

        assertEquals(response.getStatusCode(), 204);
        verify(service).updateStack(any(), eq("stack123"));
        verify(subject).hasPermission(eq("stack"), eq("stack123"), Matchers.eq(UPDATE));
    }

    @Test
    public void shouldCheckPermissionsOnStackRemoving() throws Exception {
        when(subject.hasPermission("stack", "stack123", DELETE)).thenReturn(true);

        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .when()
                                         .delete(SECURE_PATH + "/stack/stack123");

        assertEquals(response.getStatusCode(), 204);
        verify(service).removeStack(eq("stack123"));
        verify(subject).hasPermission(eq("stack"), eq("stack123"), Matchers.eq(DELETE));
    }

    @Test
    public void shouldNotCheckPermissionsOnStacksSearching() throws Exception {
        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .when()
                                         .get(SECURE_PATH + "/stack");

        assertEquals(response.getStatusCode(), 200);
        verify(service).searchStacks(anyListOf(String.class), anyInt(), anyInt());
        verifyZeroInteractions(subject);
    }

    @Test
    public void shouldCheckPermissionsOnIconReading() throws Exception {
        when(subject.hasPermission("stack", "stack123", READ)).thenReturn(true);

        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("application/json")
                                         .when()
                                         .get(SECURE_PATH + "/stack/stack123/icon");

        assertEquals(response.getStatusCode(), 204);
        verify(service).getIcon(eq("stack123"));
        verify(subject).hasPermission(eq("stack"), eq("stack123"), Matchers.eq(READ));
    }

    @Test
    public void shouldCheckPermissionsOnIconUploading() throws Exception {
        when(subject.hasPermission("stack", "stack123", UPDATE)).thenReturn(true);

        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("multipart/form-data")
                                         .multiPart("icon", "content", "image/png")
                                         .when()
                                         .post(SECURE_PATH + "/stack/stack123/icon");

        assertEquals(response.getStatusCode(), 204);
        verify(service).uploadIcon(any(), eq("stack123"));
        verify(subject).hasPermission(eq("stack"), eq("stack123"), Matchers.eq(UPDATE));
    }

    @Test
    public void shouldThrowForbiddenExceptionWhenUserDoesNotHavePermissionsForIconUpdating() {
        when(subject.hasPermission("stack", "stack123", UPDATE)).thenReturn(false);

        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("multipart/form-data")
                                         .multiPart("icon", "content", "image/png")
                                         .when()
                                         .post(SECURE_PATH + "/stack/stack123/icon");

        assertEquals(response.getStatusCode(), 403);
        Assert.assertEquals(unwrapError(response),
                            "The user does not have permission to " + UPDATE + " stack with id 'stack123'");
        verifyZeroInteractions(service);
    }

    @Test
    public void shouldCheckPermissionsOnIconRemoving() throws Exception {
        when(subject.hasPermission("stack", "stack123", UPDATE)).thenReturn(true);

        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType("multipart/form-data")
                                         .when()
                                         .delete(SECURE_PATH + "/stack/stack123/icon");

        assertEquals(response.getStatusCode(), 204);
        verify(service).removeIcon(eq("stack123"));
        verify(subject).hasPermission(eq("stack"), eq("stack123"), Matchers.eq(UPDATE));
    }

    @Test(expectedExceptions = ForbiddenException.class,
          expectedExceptionsMessageRegExp = "The user does not have permission to perform this operation")
    public void shouldThrowForbiddenExceptionWhenRequestedUnknownMethod() throws Exception {
        final GenericResourceMethod mock = mock(GenericResourceMethod.class);
        Method injectLinks = WorkspaceService.class.getMethod("getServiceDescriptor");
        when(mock.getMethod()).thenReturn(injectLinks);

        permissionsFilter.filter(mock, new Object[] {});
    }

    @Test(dataProvider = "coveredPaths")
    public void shouldThrowForbiddenExceptionWhenUserDoesNotHavePermissionsForPerformOperation(String path,
                                                                                               String method,
                                                                                               String action) throws Exception {
        when(subject.hasPermission(anyString(), anyString(), anyString())).thenReturn(false);

        Response response = request(given().auth()
                                           .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                           .when(),
                                    SECURE_PATH + path,
                                    method);

        assertEquals(response.getStatusCode(), 403);
        assertEquals(unwrapError(response), "The user does not have permission to " + action + " stack with id 'stack123'");
        verifyZeroInteractions(service);
    }

    @DataProvider(name = "coveredPaths")
    public Object[][] pathsProvider() {
        return new Object[][] {
                {"/stack/stack123", "get", READ},
                {"/stack/stack123", "put", UPDATE},
                {"/stack/stack123", "delete", DELETE},
                {"/stack/stack123/icon", "get", READ},
                {"/stack/stack123/icon", "delete", UPDATE}
        };
    }

    private Response request(RequestSpecification request, String path, String method) {
        switch (method) {
            case "post":
                return request.post(path);
            case "get":
                return request.get(path);
            case "delete":
                return request.delete(path);
            case "put":
                return request.put(path);
        }
        throw new RuntimeException("Unsupported method");
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
