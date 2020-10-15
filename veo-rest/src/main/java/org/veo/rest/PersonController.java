/*******************************************************************************
 * Copyright (c) 2020 Jochen Kemnade.
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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.veo.rest.ControllerConstants.ANY_AUTH;
import static org.veo.rest.ControllerConstants.DISPLAY_NAME_PARAM;
import static org.veo.rest.ControllerConstants.UNIT_PARAM;
import static org.veo.rest.ControllerConstants.UUID_PARAM;
import static org.veo.rest.ControllerConstants.UUID_REGEX;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.veo.adapter.ModelObjectReferenceResolver;
import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.dto.SearchQueryDto;
import org.veo.adapter.presenter.api.dto.create.CreatePersonDto;
import org.veo.adapter.presenter.api.dto.full.FullPersonDto;
import org.veo.adapter.presenter.api.io.mapper.CreatePersonOutputMapper;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityContext;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoContext;
import org.veo.core.entity.Client;
import org.veo.core.entity.EntityTypeNames;
import org.veo.core.entity.Key;
import org.veo.core.entity.Person;
import org.veo.core.usecase.base.DeleteEntityUseCase;
import org.veo.core.usecase.base.ModifyEntityUseCase;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.person.CreatePersonUseCase;
import org.veo.core.usecase.person.CreatePersonUseCase.InputData;
import org.veo.core.usecase.person.GetPersonUseCase;
import org.veo.core.usecase.person.GetPersonsUseCase;
import org.veo.core.usecase.person.UpdatePersonUseCase;
import org.veo.rest.annotations.ParameterUuid;
import org.veo.rest.annotations.UnitUuidParam;
import org.veo.rest.common.RestApiResponse;
import org.veo.rest.interactor.UseCaseInteractorImpl;
import org.veo.rest.security.ApplicationUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;

/**
 * REST service which provides methods to manage persons.
 */
@RestController
@RequestMapping(PersonController.URL_BASE_PATH)
@Slf4j
public class PersonController extends AbstractEntityController {

    public static final String URL_BASE_PATH = "/" + EntityTypeNames.PERSONS;

    private final UseCaseInteractorImpl useCaseInteractor;
    private final CreatePersonUseCase<ResponseEntity<ApiResponseBody>> createPersonUseCase;
    private final GetPersonUseCase<FullPersonDto> getPersonUseCase;
    private final GetPersonsUseCase<List<FullPersonDto>> getPersonsUseCase;
    private final UpdatePersonUseCase<FullPersonDto> updatePersonUseCase;
    private final DeleteEntityUseCase deleteEntityUseCase;
    private final ModelObjectReferenceResolver referenceResolver;

    public PersonController(UseCaseInteractorImpl useCaseInteractor,
            CreatePersonUseCase createPersonUseCase, GetPersonUseCase getPersonUseCase,
            GetPersonsUseCase getPersonsUseCase, UpdatePersonUseCase updatePersonUseCase,
            DeleteEntityUseCase deleteEntityUseCase,
            ModelObjectReferenceResolver referenceResolver) {
        this.useCaseInteractor = useCaseInteractor;
        this.createPersonUseCase = createPersonUseCase;
        this.getPersonUseCase = getPersonUseCase;
        this.getPersonsUseCase = getPersonsUseCase;
        this.updatePersonUseCase = updatePersonUseCase;
        this.deleteEntityUseCase = deleteEntityUseCase;
        this.referenceResolver = referenceResolver;
    }

    @GetMapping
    @Operation(summary = "Loads all persons")
    public @Valid CompletableFuture<List<FullPersonDto>> getPersons(
            @Parameter(required = false, hidden = true) Authentication auth,
            @UnitUuidParam @RequestParam(value = UNIT_PARAM, required = false) String unitUuid,
            @UnitUuidParam @RequestParam(value = DISPLAY_NAME_PARAM,
                                         required = false) String displayName) {
        Client client = null;
        try {
            client = getAuthenticatedClient(auth);
        } catch (NoSuchElementException e) {
            return CompletableFuture.supplyAsync(Collections::emptyList);
        }

        final GetPersonsUseCase.InputData inputData = new GetPersonsUseCase.InputData(client,
                Optional.ofNullable(unitUuid), Optional.ofNullable(displayName));
        EntityToDtoContext tcontext = EntityToDtoContext.getCompleteTransformationContext(referenceAssembler);
        return useCaseInteractor.execute(getPersonsUseCase, inputData, output -> {
            return output.getEntities()
                         .stream()
                         .map(u -> FullPersonDto.from(u, tcontext))
                         .collect(Collectors.toList());
        });
    }

    @GetMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Loads a person")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Person loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = FullPersonDto.class))),
            @ApiResponse(responseCode = "404", description = "Person not found") })
    public @Valid CompletableFuture<ResponseEntity<FullPersonDto>> getPerson(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid) {
        Client client = getAuthenticatedClient(auth);

        CompletableFuture<FullPersonDto> personFuture = useCaseInteractor.execute(getPersonUseCase,
                                                                                  new GetPersonUseCase.InputData(
                                                                                          Key.uuidFrom(uuid),
                                                                                          client),
                                                                                  output -> {
                                                                                      EntityToDtoContext tcontext = EntityToDtoContext.getCompleteTransformationContext(referenceAssembler);
                                                                                      return FullPersonDto.from(output.getPerson(),
                                                                                                                tcontext);
                                                                                  });

        return personFuture.thenApply(personDto -> ResponseEntity.ok()
                                                                 .eTag(ETag.from(personDto.getId(),
                                                                                 personDto.getVersion()))
                                                                 .body(personDto));
    }

    @PostMapping()
    @Operation(summary = "Creates a person")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Person created") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> createPerson(
            @Parameter(hidden = true) ApplicationUser user,
            @Valid @NotNull @RequestBody CreatePersonDto dto) {
        return useCaseInteractor.execute(createPersonUseCase,
                                         new Supplier<CreatePersonUseCase.InputData>() {

                                             @Override
                                             public InputData get() {
                                                 Client client = getClient(user);
                                                 DtoToEntityContext tcontext = referenceResolver.loadIntoContext(client,
                                                                                                                 dto.getReferences());
                                                 return new CreatePersonUseCase.InputData(
                                                         dto.toEntity(tcontext), client,
                                                         user.getUsername());
                                             }
                                         }, output -> {
                                             ApiResponseBody body = CreatePersonOutputMapper.map(output.getPerson());
                                             return RestApiResponse.created(URL_BASE_PATH, body);
                                         });
    }

    @PutMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Updates a person")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Person updated"),
            @ApiResponse(responseCode = "404", description = "Person not found") })
    public CompletableFuture<FullPersonDto> updatePerson(
            @Parameter(hidden = true) ApplicationUser user,
            @RequestHeader(ControllerConstants.IF_MATCH_HEADER) @NotBlank String eTag,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid,
            @Valid @NotNull @RequestBody FullPersonDto personDto) {
        applyId(uuid, personDto);
        return useCaseInteractor.execute(updatePersonUseCase,
                                         new Supplier<ModifyEntityUseCase.InputData<Person>>() {

                                             @Override
                                             public org.veo.core.usecase.base.ModifyEntityUseCase.InputData<Person> get() {
                                                 Client client = getClient(user);
                                                 DtoToEntityContext tcontext = referenceResolver.loadIntoContext(client,
                                                                                                                 personDto.getReferences());
                                                 return new ModifyEntityUseCase.InputData<Person>(
                                                         personDto.toEntity(tcontext), client, eTag,
                                                         user.getUsername());
                                             }
                                         },

                                         output -> FullPersonDto.from(output.getEntity(),
                                                                      EntityToDtoContext.getCompleteTransformationContext(referenceAssembler)));
    }

    @DeleteMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Deletes a person")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Person deleted"),
            @ApiResponse(responseCode = "404", description = "Person not found") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> deletePerson(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid) {
        Client client = getAuthenticatedClient(auth);
        return useCaseInteractor.execute(deleteEntityUseCase,
                                         new DeleteEntityUseCase.InputData(Person.class,
                                                 Key.uuidFrom(uuid), client),
                                         output -> ResponseEntity.ok()
                                                                 .build());
    }

    @Override
    protected String buildSearchUri(String id) {
        return linkTo(methodOn(PersonController.class).runSearch(ANY_AUTH, id)).withSelfRel()
                                                                               .getHref();
    }

    @GetMapping(value = "/searches/{searchId}")
    @Operation(summary = "Finds persons for the search.")
    public @Valid CompletableFuture<List<FullPersonDto>> runSearch(
            @Parameter(required = false, hidden = true) Authentication auth,
            @PathVariable String searchId) {
        // TODO VEO-38 replace this placeholder implementation with a search usecase:
        try {
            var searchQuery = SearchQueryDto.decodeFromSearchId(searchId);
            return getPersons(auth, searchQuery.getUnitId(), searchQuery.getDisplayName());
        } catch (IOException e) {
            log.error(String.format("Could not decode search URL: %s", e.getLocalizedMessage()));
            return null;
        }
    }
}
