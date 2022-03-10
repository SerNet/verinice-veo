/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Daniel Murygin.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.rest;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.veo.rest.ControllerConstants.ANY_AUTH;
import static org.veo.rest.ControllerConstants.ANY_INT;
import static org.veo.rest.ControllerConstants.ANY_STRING;
import static org.veo.rest.ControllerConstants.DESCRIPTION_PARAM;
import static org.veo.rest.ControllerConstants.DESIGNATOR_PARAM;
import static org.veo.rest.ControllerConstants.DISPLAY_NAME_PARAM;
import static org.veo.rest.ControllerConstants.NAME_PARAM;
import static org.veo.rest.ControllerConstants.PAGE_NUMBER_DEFAULT_VALUE;
import static org.veo.rest.ControllerConstants.PAGE_NUMBER_PARAM;
import static org.veo.rest.ControllerConstants.PAGE_SIZE_DEFAULT_VALUE;
import static org.veo.rest.ControllerConstants.PAGE_SIZE_PARAM;
import static org.veo.rest.ControllerConstants.SORT_COLUMN_DEFAULT_VALUE;
import static org.veo.rest.ControllerConstants.SORT_COLUMN_PARAM;
import static org.veo.rest.ControllerConstants.SORT_ORDER_DEFAULT_VALUE;
import static org.veo.rest.ControllerConstants.SORT_ORDER_PARAM;
import static org.veo.rest.ControllerConstants.SORT_ORDER_PATTERN;
import static org.veo.rest.ControllerConstants.STATUS_PARAM;
import static org.veo.rest.ControllerConstants.SUB_TYPE_PARAM;
import static org.veo.rest.ControllerConstants.UNIT_PARAM;
import static org.veo.rest.ControllerConstants.UPDATED_BY_PARAM;
import static org.veo.rest.ControllerConstants.UUID_DESCRIPTION;
import static org.veo.rest.ControllerConstants.UUID_EXAMPLE;
import static org.veo.rest.ControllerConstants.UUID_PARAM;
import static org.veo.rest.ControllerConstants.UUID_REGEX;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

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
import org.springframework.web.context.request.WebRequest;

import com.github.JanLoebel.jsonschemavalidation.JsonSchemaValidation;

import org.veo.adapter.IdRefResolver;
import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.dto.AbstractControlDto;
import org.veo.adapter.presenter.api.dto.PageDto;
import org.veo.adapter.presenter.api.dto.SearchQueryDto;
import org.veo.adapter.presenter.api.dto.create.CreateControlDto;
import org.veo.adapter.presenter.api.dto.full.FullControlDto;
import org.veo.adapter.presenter.api.io.mapper.CreateOutputMapper;
import org.veo.adapter.presenter.api.io.mapper.GetElementsInputMapper;
import org.veo.adapter.presenter.api.io.mapper.PagingMapper;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.Key;
import org.veo.core.usecase.base.CreateElementUseCase;
import org.veo.core.usecase.base.DeleteElementUseCase;
import org.veo.core.usecase.base.GetElementsUseCase;
import org.veo.core.usecase.base.ModifyElementUseCase;
import org.veo.core.usecase.base.ModifyElementUseCase.InputData;
import org.veo.core.usecase.control.CreateControlUseCase;
import org.veo.core.usecase.control.GetControlUseCase;
import org.veo.core.usecase.control.GetControlsUseCase;
import org.veo.core.usecase.control.UpdateControlUseCase;
import org.veo.rest.annotations.UnitUuidParam;
import org.veo.rest.common.RestApiResponse;
import org.veo.rest.security.ApplicationUser;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;

/**
 * REST service which provides methods to manage controls.
 */
@RestController
@RequestMapping(ControlController.URL_BASE_PATH)
@Slf4j
public class ControlController extends AbstractElementController<Control, FullControlDto> {

    public static final String URL_BASE_PATH = "/" + Control.PLURAL_TERM;

    private final CreateControlUseCase createControlUseCase;
    private final GetControlsUseCase getControlsUseCase;
    private final UpdateControlUseCase updateControlUseCase;
    private final DeleteElementUseCase deleteElementUseCase;

    public ControlController(CreateControlUseCase createControlUseCase,
            GetControlUseCase getControlUseCase, GetControlsUseCase getControlsUseCase,
            UpdateControlUseCase updateControlUseCase, DeleteElementUseCase deleteElementUseCase) {
        super(Control.class, getControlUseCase);
        this.createControlUseCase = createControlUseCase;
        this.getControlsUseCase = getControlsUseCase;
        this.updateControlUseCase = updateControlUseCase;
        this.deleteElementUseCase = deleteElementUseCase;
    }

    @GetMapping
    @Operation(summary = "Loads all controls")
    public @Valid CompletableFuture<PageDto<FullControlDto>> getControls(
            @Parameter(required = false, hidden = true) Authentication auth,
            @UnitUuidParam @RequestParam(value = UNIT_PARAM, required = false) String unitUuid,
            @RequestParam(value = DISPLAY_NAME_PARAM, required = false) String displayName,
            @RequestParam(value = SUB_TYPE_PARAM, required = false) String subType,
            @RequestParam(value = STATUS_PARAM, required = false) String status,
            @RequestParam(value = DESCRIPTION_PARAM, required = false) String description,
            @RequestParam(value = DESIGNATOR_PARAM, required = false) String designator,
            @RequestParam(value = NAME_PARAM, required = false) String name,
            @RequestParam(value = UPDATED_BY_PARAM, required = false) String updatedBy,
            @RequestParam(value = PAGE_SIZE_PARAM,
                          required = false,
                          defaultValue = PAGE_SIZE_DEFAULT_VALUE) Integer pageSize,
            @RequestParam(value = PAGE_NUMBER_PARAM,
                          required = false,
                          defaultValue = PAGE_NUMBER_DEFAULT_VALUE) Integer pageNumber,
            @RequestParam(value = SORT_COLUMN_PARAM,
                          required = false,
                          defaultValue = SORT_COLUMN_DEFAULT_VALUE) String sortColumn,
            @RequestParam(value = SORT_ORDER_PARAM,
                          required = false,
                          defaultValue = SORT_ORDER_DEFAULT_VALUE) @Pattern(regexp = SORT_ORDER_PATTERN) String sortOrder) {
        Client client = null;
        try {
            client = getAuthenticatedClient(auth);
        } catch (NoSuchElementException e) {
            return CompletableFuture.supplyAsync(PageDto::emptyPage);
        }

        return getControls(GetElementsInputMapper.map(client, unitUuid, displayName, subType,
                                                      status, description, designator, name,
                                                      updatedBy,
                                                      PagingMapper.toConfig(pageSize, pageNumber,
                                                                            sortColumn,
                                                                            sortOrder)));
    }

    private CompletableFuture<PageDto<FullControlDto>> getControls(
            GetElementsUseCase.InputData inputData) {
        return useCaseInteractor.execute(getControlsUseCase, inputData,
                                         output -> PagingMapper.toPage(output.getElements(),
                                                                       entityToDtoTransformer::transformControl2Dto));
    }

    @Override
    @Operation(summary = "Loads a control")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Control loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = AbstractControlDto.class))),
            @ApiResponse(responseCode = "404", description = "Control not found") })
    @GetMapping(ControllerConstants.UUID_PARAM_SPEC)
    public @Valid CompletableFuture<ResponseEntity<FullControlDto>> getElement(
            @Parameter(required = false, hidden = true) Authentication auth,
            @Parameter(required = true,
                       example = UUID_EXAMPLE,
                       description = UUID_DESCRIPTION) @PathVariable String uuid,
            WebRequest request) {
        return super.getElement(auth, uuid, request);
    }

    @Override
    @Operation(summary = "Loads the parts of a control")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Parts loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            array = @ArraySchema(schema = @Schema(implementation = FullControlDto.class)))),
            @ApiResponse(responseCode = "404", description = "Control not found") })
    @GetMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}/parts")
    public @Valid CompletableFuture<ResponseEntity<List<FullControlDto>>> getElementParts(
            @Parameter(required = false, hidden = true) Authentication auth,
            @Parameter(required = true,
                       example = UUID_EXAMPLE,
                       description = UUID_DESCRIPTION) @PathVariable String uuid,
            WebRequest request) {
        return super.getElementParts(auth, uuid, request);
    }

    @PostMapping()
    @Operation(summary = "Creates a control")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Control created") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> createControl(
            @Parameter(hidden = true) ApplicationUser user,
            @Valid @NotNull @RequestBody @JsonSchemaValidation(Control.SINGULAR_TERM) CreateControlDto dto) {
        return useCaseInteractor.execute(createControlUseCase,
                                         (Supplier<CreateElementUseCase.InputData<Control>>) () -> {

                                             Client client = getClient(user);
                                             IdRefResolver idRefResolver = createIdRefResolver(client);
                                             return new CreateElementUseCase.InputData<>(
                                                     dtoToEntityTransformer.transformDto2Control(dto,
                                                                                                 idRefResolver),
                                                     client);
                                         }, output -> {
                                             ApiResponseBody body = CreateOutputMapper.map(output.getEntity());
                                             return RestApiResponse.created(URL_BASE_PATH, body);
                                         });
    }

    @PutMapping(ControllerConstants.UUID_PARAM_SPEC)
    @Operation(summary = "Updates a control")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Control updated"),
            @ApiResponse(responseCode = "404", description = "Control not found") })
    public CompletableFuture<FullControlDto> updateControl(
            @Parameter(hidden = true) ApplicationUser user,
            @RequestHeader(ControllerConstants.IF_MATCH_HEADER) @NotBlank String eTag,
            @Parameter(required = true,
                       example = UUID_EXAMPLE,
                       description = UUID_DESCRIPTION) @PathVariable String uuid,
            @Valid @NotNull @RequestBody @JsonSchemaValidation(Control.SINGULAR_TERM) FullControlDto controlDto) {
        controlDto.applyResourceId(uuid);
        return useCaseInteractor.execute(updateControlUseCase, new Supplier<InputData<Control>>() {
            @Override
            public InputData<Control> get() {
                Client client = getClient(user);
                IdRefResolver idRefResolver = createIdRefResolver(client);
                return new ModifyElementUseCase.InputData<>(
                        dtoToEntityTransformer.transformDto2Control(controlDto, idRefResolver),
                        client, eTag, user.getUsername());
            }
        },

                                         output -> entityToDtoTransformer.transformControl2Dto(output.getEntity()));
    }

    @DeleteMapping(ControllerConstants.UUID_PARAM_SPEC)
    @Operation(summary = "Deletes a control")
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Control deleted"),
            @ApiResponse(responseCode = "404", description = "Control not found") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteControl(
            @Parameter(required = false, hidden = true) Authentication auth,
            @Parameter(required = true,
                       example = UUID_EXAMPLE,
                       description = UUID_DESCRIPTION) @PathVariable String uuid) {
        Client client = getAuthenticatedClient(auth);
        return useCaseInteractor.execute(deleteElementUseCase,
                                         new DeleteElementUseCase.InputData(Control.class,
                                                 Key.uuidFrom(uuid), client),
                                         output -> ResponseEntity.noContent()
                                                                 .build());
    }

    @Override
    @SuppressFBWarnings("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
    protected String buildSearchUri(String id) {
        return linkTo(methodOn(ControlController.class).runSearch(ANY_AUTH, id, ANY_INT, ANY_INT,
                                                                  ANY_STRING, ANY_STRING))
                                                                                          .withSelfRel()
                                                                                          .getHref();
    }

    @GetMapping(value = "/searches/{searchId}")
    @Operation(summary = "Finds controls for the search.")
    public @Valid CompletableFuture<PageDto<FullControlDto>> runSearch(
            @Parameter(required = false, hidden = true) Authentication auth,
            @PathVariable String searchId,
            @RequestParam(value = PAGE_SIZE_PARAM,
                          required = false,
                          defaultValue = PAGE_SIZE_DEFAULT_VALUE) Integer pageSize,
            @RequestParam(value = PAGE_NUMBER_PARAM,
                          required = false,
                          defaultValue = PAGE_NUMBER_DEFAULT_VALUE) Integer pageNumber,
            @RequestParam(value = SORT_COLUMN_PARAM,
                          required = false,
                          defaultValue = SORT_COLUMN_DEFAULT_VALUE) String sortColumn,
            @RequestParam(value = SORT_ORDER_PARAM,
                          required = false,
                          defaultValue = SORT_ORDER_DEFAULT_VALUE) @Pattern(regexp = SORT_ORDER_PATTERN) String sortOrder) {
        try {
            return getControls(GetElementsInputMapper.map(getAuthenticatedClient(auth),
                                                          SearchQueryDto.decodeFromSearchId(searchId),
                                                          PagingMapper.toConfig(pageSize,
                                                                                pageNumber,
                                                                                sortColumn,
                                                                                sortOrder)));
        } catch (IOException e) {
            log.error("Could not decode search URL: {}", e.getLocalizedMessage());
            return null;
        }
    }

    @Override
    protected FullControlDto entity2Dto(Control entity) {
        return entityToDtoTransformer.transformControl2Dto(entity);
    }
}
