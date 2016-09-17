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
package com.codenvy.api.permission.shared.dto;

import com.codenvy.api.permission.shared.model.PermissionsDomain;

import org.eclipse.che.dto.shared.DTO;

import java.util.List;

/**
 * @author Sergii Leschenko
 */
@DTO
public interface DomainDto extends PermissionsDomain {
    @Override
    String getId();

    void setId(String id);

    DomainDto withId(String id);

    @Override
    List<String> getAllowedActions();

    void setAllowedActions(List<String> allowedActions);

    DomainDto withAllowedActions(List<String> allowedActions);

    @Override
    Boolean isInstanceRequired();

    void setInstanceRequired(Boolean isInstanceRequired);

    DomainDto withInstanceRequired(Boolean isInstanceRequired);
}
