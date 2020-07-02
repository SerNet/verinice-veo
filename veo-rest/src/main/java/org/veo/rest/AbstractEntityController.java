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

import java.util.Collection;
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

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.veo.adapter.presenter.api.common.ModelObjectReference;
import org.veo.adapter.presenter.api.response.transformer.DtoTargetToEntityContext;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.entity.ModelObject;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.usecase.repository.ClientRepository;
import org.veo.core.usecase.repository.Repository;
import org.veo.core.usecase.repository.RepositoryProvider;
import org.veo.persistence.entity.jpa.transformer.DataTargetToEntityContext;
import org.veo.rest.security.ApplicationUser;

// TODO: VEO-115 this class (which is not abstract) is extended by controllers. Instead it should be a separate service.
// Having this here also forces the controllers to start their own transaction. That means we are back to the OSIV-pattern
// (open-session-view) that we explicitely disabled in the application.properties.
@SecurityRequirement(name = RestApplication.SECURITY_SCHEME_OAUTH)
public class AbstractEntityController {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private RepositoryProvider repositoryProvider;

    public AbstractEntityController() {
        super();
    }

    protected Client getClient(String clientId) {
        Key<UUID> id = Key.uuidFrom(clientId);
        DataTargetToEntityContext dataToEntityContext = DataTargetToEntityContext.getCompleteTransformationContext();
        dataToEntityContext.partialUnit();
        return clientRepository.findById(id, dataToEntityContext)
                               .orElseThrow();
    }

    protected Client getAuthenticatedClient(Authentication auth) {
        ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
        return getClient(user.getClientId());
    }

    protected DtoTargetToEntityContext configureDtoContext(Client client,
            Collection<ModelObjectReference<? extends ModelObject>> collection) {
        DtoTargetToEntityContext tcontext = DtoTargetToEntityContext.getCompleteTransformationContext();

        for (Domain d : client.getDomains()) {
            tcontext.addEntity(d);
        }
        DataTargetToEntityContext referencesTransformContext = DataTargetToEntityContext.getCompleteTransformationContext()
                                                                                        .noUnitDomains()
                                                                                        .noUnitParent()
                                                                                        .noUnitUnits()
                                                                                        .partialClient()
                                                                                        .partialAsset()
                                                                                        .partialDocument()
                                                                                        .partialControl()
                                                                                        .partialPerson()
                                                                                        .partialProcess();
        for (ModelObjectReference<? extends ModelObject> objectReference : collection) {
            if (objectReference.getType()
                               .equals(Domain.class)) {
                continue;// skip domains as we get them from the client
            }
            Repository<? extends ModelObject, Key<UUID>> entityRepository = repositoryProvider.getRepositoryFor(objectReference.getType());
            ModelObject modelObject = entityRepository.findById(Key.uuidFrom(objectReference.getId()),
                                                                referencesTransformContext)
                                                      .orElseThrow(() -> new NotFoundException(
                                                              "ref not found %s %s",
                                                              objectReference.getId(),
                                                              objectReference.getType()));
            tcontext.addEntity(modelObject);
        }
        return tcontext;
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

}