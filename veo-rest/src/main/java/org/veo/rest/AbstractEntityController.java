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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import org.veo.adapter.ModelObjectReferenceResolver;
import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.adapter.presenter.api.dto.SearchQueryDto;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityTransformer;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.usecase.repository.ClientRepository;
import org.veo.core.usecase.repository.RepositoryProvider;
import org.veo.rest.common.ReferenceAssemblerImpl;
import org.veo.rest.common.SearchResponse;
import org.veo.rest.security.ApplicationUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;

// TODO: see VEO-115
@SecurityRequirement(name = RestApplication.SECURITY_SCHEME_OAUTH)
@Slf4j
public abstract class AbstractEntityController {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private RepositoryProvider repositoryProvider;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    ReferenceAssemblerImpl referenceAssembler;

    @Autowired
    EntityToDtoTransformer entityToDtoTransformer;

    @Autowired
    DtoToEntityTransformer dtoToEntityTransformer;

    @Autowired
    ReferenceAssembler urlAssembler;

    public AbstractEntityController() {
        super();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error("Error validating request", ex);
        return ex.getBindingResult()
                 .getAllErrors()
                 .stream()
                 .map(err -> (FieldError) err)
                 .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
    }

    @PostMapping(value = "/searches")
    @Operation(summary = "Creates a new search with the given search criteria.")
    public @Valid CompletableFuture<ResponseEntity<SearchResponse>> createSearch(
            @Parameter(required = false, hidden = true) Authentication auth,
            @Valid @RequestBody SearchQueryDto search) {
        return CompletableFuture.supplyAsync(() -> createSearchResponseBody(search));
    }

    protected Client getClient(String clientId) {
        Key<UUID> id = Key.uuidFrom(clientId);
        return clientRepository.findById(id)
                               .orElseThrow();
    }

    protected Client getAuthenticatedClient(Authentication auth) {
        ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
        return getClient(user);
    }

    protected Client getClient(ApplicationUser user) {
        return getClient(user.getClientId());
    }

    protected ModelObjectReferenceResolver createModelObjectReferenceResolver(Client client) {
        return new ModelObjectReferenceResolver(repositoryProvider, client);
    }

    protected abstract String buildSearchUri(String searchId);

    private ResponseEntity<SearchResponse> createSearchResponseBody(SearchQueryDto search) {
        try {
            return ResponseEntity.created(new URI(buildSearchUri(search.getSearchId())))
                                 .body(new SearchResponse(buildSearchUri(search.getSearchId())));
        } catch (IOException | URISyntaxException e) {
            log.error("Could not create search.", e);
            throw new IllegalArgumentException(String.format("Could not create search %s", search));
        }
    }
}
