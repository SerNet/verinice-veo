/*******************************************************************************
 * Copyright (c) 2020 Urs Zeidler.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.rest;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import org.veo.adapter.presenter.api.response.IdentifiableDto;
import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.usecase.repository.ClientRepository;
import org.veo.rest.security.ApplicationUser;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@SecurityRequirement(name = RestApplication.SECURITY_SCHEME_OAUTH)
abstract class AbstractEntityController {

    @Autowired
    private ClientRepository clientRepository;

    protected Client getClient(String clientId) {
        Key<UUID> id = Key.uuidFrom(clientId);
        return clientRepository.findById(id)
                               .orElseThrow();
    }

    protected Client getAuthenticatedClient(Authentication auth) {
        ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
        return getClient(user.getClientId());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        return ex.getBindingResult()
                 .getAllErrors()
                 .stream()
                 .map(err -> (FieldError) err)
                 .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
    }

    protected void applyId(String resourceId, IdentifiableDto dto) {
        var dtoId = dto.getId();
        if (dtoId != null && !dtoId.equals(resourceId)) {
            throw new DeviatingIdException(
                    String.format("DTO ID %s does not match resource ID %s", dtoId, resourceId));
        }
        dto.setId(resourceId);
    }
}