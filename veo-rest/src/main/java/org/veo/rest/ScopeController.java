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
import static org.veo.rest.ControllerConstants.ABBREVIATION_PARAM;
import static org.veo.rest.ControllerConstants.ANY_AUTH;
import static org.veo.rest.ControllerConstants.ANY_BOOLEAN;
import static org.veo.rest.ControllerConstants.ANY_INT;
import static org.veo.rest.ControllerConstants.ANY_STRING;
import static org.veo.rest.ControllerConstants.CHILD_ELEMENT_IDS_PARAM;
import static org.veo.rest.ControllerConstants.DESCRIPTION_PARAM;
import static org.veo.rest.ControllerConstants.DESIGNATOR_PARAM;
import static org.veo.rest.ControllerConstants.DISPLAY_NAME_PARAM;
import static org.veo.rest.ControllerConstants.DOMAIN_PARAM;
import static org.veo.rest.ControllerConstants.EMBED_RISKS_DESC;
import static org.veo.rest.ControllerConstants.HAS_CHILD_ELEMENTS_PARAM;
import static org.veo.rest.ControllerConstants.HAS_PARENT_ELEMENTS_PARAM;
import static org.veo.rest.ControllerConstants.IF_MATCH_HEADER;
import static org.veo.rest.ControllerConstants.IF_MATCH_HEADER_NOT_BLANK_MESSAGE;
import static org.veo.rest.ControllerConstants.NAME_PARAM;
import static org.veo.rest.ControllerConstants.PAGE_NUMBER_DEFAULT_VALUE;
import static org.veo.rest.ControllerConstants.PAGE_NUMBER_PARAM;
import static org.veo.rest.ControllerConstants.PAGE_SIZE_DEFAULT_VALUE;
import static org.veo.rest.ControllerConstants.PAGE_SIZE_PARAM;
import static org.veo.rest.ControllerConstants.SCOPE_IDS_DESCRIPTION;
import static org.veo.rest.ControllerConstants.SCOPE_IDS_PARAM;
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
import static org.veo.rest.ControllerConstants.UUID_PARAM_SPEC;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

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

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.dto.AbstractElementDto;
import org.veo.adapter.presenter.api.dto.FullElementDto;
import org.veo.adapter.presenter.api.dto.PageDto;
import org.veo.adapter.presenter.api.dto.RequirementImplementationDto;
import org.veo.adapter.presenter.api.dto.SearchQueryDto;
import org.veo.adapter.presenter.api.dto.create.CreateScopeDto;
import org.veo.adapter.presenter.api.dto.full.FullScopeDto;
import org.veo.adapter.presenter.api.dto.full.ScopeRiskDto;
import org.veo.adapter.presenter.api.io.mapper.CategorizedRiskValueMapper;
import org.veo.adapter.presenter.api.io.mapper.CreateElementInputMapper;
import org.veo.adapter.presenter.api.io.mapper.PagingMapper;
import org.veo.adapter.presenter.api.io.mapper.QueryInputMapper;
import org.veo.adapter.presenter.api.unit.GetRequirementImplementationsByControlImplementationInputMapper;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.Scope;
import org.veo.core.entity.inspection.Finding;
import org.veo.core.entity.ref.TypedId;
import org.veo.core.usecase.InspectElementUseCase;
import org.veo.core.usecase.base.CreateElementUseCase;
import org.veo.core.usecase.base.DeleteElementUseCase;
import org.veo.core.usecase.base.GetElementUseCase;
import org.veo.core.usecase.base.GetElementsUseCase;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.compliance.GetRequirementImplementationUseCase;
import org.veo.core.usecase.compliance.GetRequirementImplementationsByControlImplementationUseCase;
import org.veo.core.usecase.compliance.UpdateRequirementImplementationUseCase;
import org.veo.core.usecase.decision.EvaluateElementUseCase;
import org.veo.core.usecase.risk.DeleteRiskUseCase;
import org.veo.core.usecase.scope.CreateScopeRiskUseCase;
import org.veo.core.usecase.scope.GetScopeRiskUseCase;
import org.veo.core.usecase.scope.GetScopeRisksUseCase;
import org.veo.core.usecase.scope.GetScopeUseCase;
import org.veo.core.usecase.scope.UpdateScopeRiskUseCase;
import org.veo.core.usecase.scope.UpdateScopeUseCase;
import org.veo.rest.annotations.UnitUuidParam;
import org.veo.rest.common.RestApiResponse;
import org.veo.rest.schemas.EvaluateElementOutputSchema;
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

/** REST service which provides methods to manage scopes. */
@RestController
@RequestMapping(ScopeController.URL_BASE_PATH)
@Slf4j
public class ScopeController extends AbstractElementController<Scope, FullScopeDto>
    implements ScopeRiskResource, RiskAffectedResource {
  public static final String EMBED_RISKS_PARAM = "embedRisks";
  public static final String URL_BASE_PATH = "/" + Scope.PLURAL_TERM;

  private final CreateElementUseCase<Scope> createScopeUseCase;
  private final UpdateScopeUseCase updateScopeUseCase;
  private final DeleteElementUseCase deleteElementUseCase;
  private final GetScopeRiskUseCase getScopeRiskUseCase;
  private final CreateScopeRiskUseCase createScopeRiskUseCase;
  private final GetScopeRisksUseCase getScopeRisksUseCase;
  private final DeleteRiskUseCase deleteRiskUseCase;
  private final UpdateScopeRiskUseCase updateScopeRiskUseCase;
  private final GetRequirementImplementationsByControlImplementationUseCase
      getRequirementImplementationsByControlImplementationUseCase;
  private final GetRequirementImplementationUseCase getRequirementImplementationUseCase;
  private final UpdateRequirementImplementationUseCase updateRequirementImplementationUseCase;

  public ScopeController(
      GetScopeUseCase getElementUseCase,
      EvaluateElementUseCase evaluateElementUseCase,
      InspectElementUseCase inspectElementUseCase,
      CreateElementUseCase<Scope> createScopeUseCase,
      GetElementsUseCase getElementsUseCase,
      UpdateScopeUseCase updateScopeUseCase,
      DeleteElementUseCase deleteElementUseCase,
      GetScopeRiskUseCase getScopeRiskUseCase,
      CreateScopeRiskUseCase createScopeRiskUseCase,
      GetScopeRisksUseCase getScopeRisksUseCase,
      DeleteRiskUseCase deleteRiskUseCase,
      UpdateScopeRiskUseCase updateScopeRiskUseCase,
      GetRequirementImplementationsByControlImplementationUseCase
          getRequirementImplementationsByControlImplementationUseCase,
      GetRequirementImplementationUseCase getRequirementImplementationUseCase,
      UpdateRequirementImplementationUseCase updateRequirementImplementationUseCase) {
    super(
        Scope.class,
        getElementUseCase,
        evaluateElementUseCase,
        inspectElementUseCase,
        getElementsUseCase);
    this.createScopeUseCase = createScopeUseCase;
    this.updateScopeUseCase = updateScopeUseCase;
    this.deleteElementUseCase = deleteElementUseCase;
    this.getScopeRiskUseCase = getScopeRiskUseCase;
    this.createScopeRiskUseCase = createScopeRiskUseCase;
    this.getScopeRisksUseCase = getScopeRisksUseCase;
    this.deleteRiskUseCase = deleteRiskUseCase;
    this.updateScopeRiskUseCase = updateScopeRiskUseCase;
    this.getRequirementImplementationsByControlImplementationUseCase =
        getRequirementImplementationsByControlImplementationUseCase;
    this.getRequirementImplementationUseCase = getRequirementImplementationUseCase;
    this.updateRequirementImplementationUseCase = updateRequirementImplementationUseCase;
  }

  @GetMapping
  @Operation(summary = "Loads all scopes")
  @ApiResponse(
      responseCode = "200",
      description = "Members loaded",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = FullScopeDto.class))))
  @ApiResponse(responseCode = "404", description = "Scope not found")
  public @Valid Future<PageDto<FullScopeDto>> getScopes(
      @Parameter(hidden = true) Authentication auth,
      @UnitUuidParam @RequestParam(value = UNIT_PARAM, required = false) UUID unitUuid,
      @RequestParam(value = DISPLAY_NAME_PARAM, required = false) String displayName,
      @RequestParam(value = SUB_TYPE_PARAM, required = false) String subType,
      @RequestParam(value = STATUS_PARAM, required = false) String status,
      @RequestParam(value = CHILD_ELEMENT_IDS_PARAM, required = false) List<UUID> childElementIds,
      @RequestParam(value = HAS_PARENT_ELEMENTS_PARAM, required = false) Boolean hasParentElements,
      @RequestParam(value = HAS_CHILD_ELEMENTS_PARAM, required = false) Boolean hasChildElements,
      @RequestParam(value = DESCRIPTION_PARAM, required = false) String description,
      @RequestParam(value = DESIGNATOR_PARAM, required = false) String designator,
      @RequestParam(value = NAME_PARAM, required = false) String name,
      @RequestParam(value = ABBREVIATION_PARAM, required = false) String abbreviation,
      @RequestParam(value = UPDATED_BY_PARAM, required = false) String updatedBy,
      @RequestParam(
              value = PAGE_SIZE_PARAM,
              required = false,
              defaultValue = PAGE_SIZE_DEFAULT_VALUE)
          @Min(1)
          Integer pageSize,
      @RequestParam(
              value = PAGE_NUMBER_PARAM,
              required = false,
              defaultValue = PAGE_NUMBER_DEFAULT_VALUE)
          Integer pageNumber,
      @RequestParam(
              value = SORT_COLUMN_PARAM,
              required = false,
              defaultValue = SORT_COLUMN_DEFAULT_VALUE)
          String sortColumn,
      @RequestParam(
              value = SORT_ORDER_PARAM,
              required = false,
              defaultValue = SORT_ORDER_DEFAULT_VALUE)
          @Pattern(regexp = SORT_ORDER_PATTERN)
          String sortOrder,
      @RequestParam(name = EMBED_RISKS_PARAM, required = false, defaultValue = "false")
          @Parameter(name = EMBED_RISKS_PARAM, description = EMBED_RISKS_DESC)
          Boolean embedRisksParam) {
    Client client = getAuthenticatedClient(auth);
    boolean embedRisks = (embedRisksParam != null) && embedRisksParam;
    return getElements(
        QueryInputMapper.map(
                client,
                unitUuid,
                null,
                displayName,
                subType,
                status,
                childElementIds,
                hasChildElements,
                hasParentElements,
                null,
                null,
                description,
                designator,
                name,
                abbreviation,
                updatedBy,
                PagingMapper.toConfig(pageSize, pageNumber, sortColumn, sortOrder))
            .withEmbedRisks(embedRisks),
        e -> entity2Dto(e, embedRisks));
  }

  @GetMapping(UUID_PARAM_SPEC)
  @Operation(summary = "Loads a scope")
  @ApiResponse(
      responseCode = "200",
      description = "Scope loaded",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = FullScopeDto.class)))
  @ApiResponse(responseCode = "404", description = "Scope not found")
  public @Valid Future<ResponseEntity<FullScopeDto>> getScope(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid,
      @RequestParam(name = EMBED_RISKS_PARAM, required = false, defaultValue = "false")
          @Parameter(name = EMBED_RISKS_PARAM, description = EMBED_RISKS_DESC)
          Boolean embedRisksParam,
      WebRequest request) {
    Client client = getAuthenticatedClient(auth);
    boolean embedRisks = (embedRisksParam != null) && embedRisksParam;
    if (getEtag(Scope.class, uuid).map(request::checkNotModified).orElse(false)) {
      return null;
    }
    CompletableFuture<FullScopeDto> scopeFuture =
        useCaseInteractor.execute(
            getElementUseCase,
            new GetElementUseCase.InputData(uuid, client, embedRisks),
            output ->
                entityToDtoTransformer.transformScope2Dto(output.element(), false, embedRisks));
    return scopeFuture.thenApply(
        scopeDto -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(scopeDto));
  }

  @GetMapping(value = "/{" + UUID_PARAM + "}/members")
  @Operation(summary = "Loads the members of a scope")
  @ApiResponse(
      responseCode = "200",
      description = "Members loaded",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = FullElementDto.class))))
  @ApiResponse(responseCode = "404", description = "Scope not found")
  public @Valid CompletableFuture<ResponseEntity<List<AbstractElementDto>>> getMembers(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid,
      WebRequest request) {
    Client client = getAuthenticatedClient(auth);
    if (getEtag(Scope.class, uuid).map(request::checkNotModified).orElse(false)) {
      return null;
    }
    return useCaseInteractor.execute(
        getElementUseCase,
        new GetElementUseCase.InputData(uuid, client),
        output -> {
          Scope scope = output.element();
          return ResponseEntity.ok()
              .cacheControl(defaultCacheControl)
              .body(
                  scope.getMembers().stream()
                      .map(member -> entityToDtoTransformer.transform2Dto(member, false))
                      .toList());
        });
  }

  @PostMapping()
  @Operation(summary = "Creates a scope")
  @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Scope created")})
  @Deprecated
  public CompletableFuture<ResponseEntity<ApiResponseBody>> createScope(
      @Parameter(hidden = true) ApplicationUser user,
      @Valid @NotNull @RequestBody CreateScopeDto createScopeDto,
      @Parameter(description = SCOPE_IDS_DESCRIPTION)
          @RequestParam(name = SCOPE_IDS_PARAM, required = false)
          List<UUID> scopeIds) {
    return useCaseInteractor.execute(
        createScopeUseCase,
        CreateElementInputMapper.map(createScopeDto, getClient(user), scopeIds),
        output -> {
          Scope scope = output.entity();
          ApiResponseBody apiResponseBody =
              new ApiResponseBody(
                  true, Optional.of(scope.getIdAsString()), "Scope created successfully.");
          return RestApiResponse.created(URL_BASE_PATH, apiResponseBody);
        });
  }

  @PutMapping(UUID_PARAM_SPEC)
  @Operation(summary = "Updates a scope")
  @ApiResponse(responseCode = "200", description = "Scope updated")
  @ApiResponse(responseCode = "404", description = "Scope not found")
  @Deprecated
  public CompletableFuture<ResponseEntity<FullScopeDto>> updateScope(
      @Parameter(hidden = true) ApplicationUser user,
      @RequestHeader(IF_MATCH_HEADER) @NotBlank(message = IF_MATCH_HEADER_NOT_BLANK_MESSAGE)
          String eTag,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid,
      @Valid @NotNull @RequestBody FullScopeDto scopeDto) {
    scopeDto.applyResourceId(uuid);
    return useCaseInteractor.execute(
        updateScopeUseCase,
        new UpdateScopeUseCase.InputData<>(
            uuid, scopeDto, getClient(user), eTag, user.getUsername()),
        output -> {
          var scope = output.entity();
          return ResponseEntity.ok()
              .eTag(ETag.from(uuid.toString(), scope.getVersion()))
              .body(entityToDtoTransformer.transformScope2Dto(scope, false));
        });
  }

  @DeleteMapping(UUID_PARAM_SPEC)
  @Operation(summary = "Deletes a scope")
  @ApiResponse(responseCode = "204", description = "Scope deleted")
  @ApiResponse(responseCode = "404", description = "Scope not found")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteScope(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid) {
    Client client = getAuthenticatedClient(auth);

    return useCaseInteractor.execute(
        deleteElementUseCase,
        new DeleteElementUseCase.InputData(Scope.class, uuid, client),
        output -> ResponseEntity.noContent().build());
  }

  @Override
  @SuppressFBWarnings("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
  protected String buildSearchUri(String id) {
    return linkTo(
            methodOn(ScopeController.class)
                .runSearch(ANY_AUTH, id, ANY_INT, ANY_INT, ANY_STRING, ANY_STRING, ANY_BOOLEAN))
        .withSelfRel()
        .getHref();
  }

  @GetMapping(value = "/searches/{searchId}")
  @Operation(summary = "Finds scopes for the search.")
  public @Valid Future<PageDto<FullScopeDto>> runSearch(
      @Parameter(hidden = true) Authentication auth,
      @PathVariable String searchId,
      @RequestParam(
              value = PAGE_SIZE_PARAM,
              required = false,
              defaultValue = PAGE_SIZE_DEFAULT_VALUE)
          @Min(1)
          Integer pageSize,
      @RequestParam(
              value = PAGE_NUMBER_PARAM,
              required = false,
              defaultValue = PAGE_NUMBER_DEFAULT_VALUE)
          Integer pageNumber,
      @RequestParam(
              value = SORT_COLUMN_PARAM,
              required = false,
              defaultValue = SORT_COLUMN_DEFAULT_VALUE)
          String sortColumn,
      @RequestParam(
              value = SORT_ORDER_PARAM,
              required = false,
              defaultValue = SORT_ORDER_DEFAULT_VALUE)
          @Pattern(regexp = SORT_ORDER_PATTERN)
          String sortOrder,
      @RequestParam(name = EMBED_RISKS_PARAM, required = false, defaultValue = "false")
          @Parameter(name = EMBED_RISKS_PARAM, description = EMBED_RISKS_DESC)
          Boolean embedRisksParam) {
    boolean embedRisks = (embedRisksParam != null) && embedRisksParam;
    try {
      return getElements(
          QueryInputMapper.map(
                  getAuthenticatedClient(auth),
                  SearchQueryDto.decodeFromSearchId(searchId),
                  PagingMapper.toConfig(pageSize, pageNumber, sortColumn, sortOrder))
              .withEmbedRisks(embedRisks),
          e -> entity2Dto(e, embedRisks));
    } catch (IOException e) {
      log.error("Could not decode search URL: {}", e.getLocalizedMessage());
      return null;
    }
  }

  @Override
  public Future<List<ScopeRiskDto>> getRisks(
      @Parameter(hidden = true) ApplicationUser user, UUID scopeId) {

    Client client = getClient(user.getClientId());
    var input = new GetScopeRisksUseCase.InputData(client, scopeId);

    return useCaseInteractor.execute(
        getScopeRisksUseCase,
        input,
        output ->
            output.getRisks().stream()
                .map(risk -> ScopeRiskDto.from(risk, referenceAssembler))
                .toList());
  }

  @Override
  public Future<ResponseEntity<ScopeRiskDto>> getRisk(
      @Parameter(hidden = true) ApplicationUser user, UUID scopeId, UUID scenarioId) {

    Client client = getClient(user.getClientId());
    return getRisk(client, scopeId, scenarioId);
  }

  private CompletableFuture<ResponseEntity<ScopeRiskDto>> getRisk(
      Client client, UUID scopeId, UUID scenarioId) {
    var input = new GetScopeRiskUseCase.InputData(client, scopeId, scenarioId);

    var riskFuture =
        useCaseInteractor.execute(
            getScopeRiskUseCase,
            input,
            output -> ScopeRiskDto.from(output.getRisk(), referenceAssembler));

    return riskFuture.thenApply(
        riskDto ->
            ResponseEntity.ok()
                .eTag(
                    ETag.from(
                        riskDto.getScope().getId(),
                        riskDto.getScenario().getId(),
                        riskDto.getVersion()))
                .body(riskDto));
  }

  @Override
  public CompletableFuture<ResponseEntity<ApiResponseBody>> createRisk(
      ApplicationUser user, @Valid @NotNull ScopeRiskDto dto, UUID scopeId) {

    var input =
        new CreateScopeRiskUseCase.InputData(
            getClient(user.getClientId()),
            scopeId,
            urlAssembler.toKey(dto.getScenario()),
            urlAssembler.toKeys(dto.getDomainReferences()),
            urlAssembler.toKey(dto.getMitigation()),
            urlAssembler.toKey(dto.getRiskOwner()),
            CategorizedRiskValueMapper.map(dto.getDomainsWithRiskValues()));

    return useCaseInteractor.execute(
        createScopeRiskUseCase,
        input,
        output -> {
          if (!output.isNewlyCreatedRisk()) return RestApiResponse.noContent();

          var url =
              String.format(
                  "%s/%s/%s",
                  URL_BASE_PATH,
                  output.getRisk().getEntity().getIdAsString(),
                  ScopeRiskResource.RESOURCE_NAME);
          var body =
              new ApiResponseBody(
                  true,
                  Optional.of(output.getRisk().getScenario().getIdAsString()),
                  "Scope risk created successfully.");
          return RestApiResponse.created(url, body);
        });
  }

  @Override
  public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteRisk(
      ApplicationUser user, UUID scopeId, UUID scenarioId) {

    Client client = getClient(user.getClientId());
    var input = new DeleteRiskUseCase.InputData(Scope.class, client, scopeId, scenarioId);

    return useCaseInteractor.execute(
        deleteRiskUseCase, input, output -> ResponseEntity.noContent().build());
  }

  @Operation(summary = "Runs inspections on a persisted scope")
  @ApiResponse(
      responseCode = "200",
      description = "Inspections have run",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = Finding.class))))
  @ApiResponse(responseCode = "404", description = "Scope not found")
  @GetMapping(value = UUID_PARAM_SPEC + "/inspection")
  public @Valid CompletableFuture<ResponseEntity<Set<Finding>>> inspect(
      @Parameter(required = true, hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid,
      @RequestParam(value = DOMAIN_PARAM) UUID domainId) {
    return inspect(auth, uuid, domainId, Scope.class);
  }

  @Operation(
      summary =
          "Evaluates decisions and inspections on a transient scope without persisting anything")
  @ApiResponse(
      responseCode = "200",
      description = "Element evaluated",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = EvaluateElementOutputSchema.class)))
  @PostMapping(value = "/evaluation")
  public CompletableFuture<ResponseEntity<EvaluateElementUseCase.OutputData>> evaluate(
      @Parameter(required = true, hidden = true) Authentication auth,
      @Valid @RequestBody FullScopeDto dto,
      @RequestParam(value = DOMAIN_PARAM) String domainId) {
    return super.evaluate(auth, dto, domainId);
  }

  @Override
  public CompletableFuture<ResponseEntity<ScopeRiskDto>> updateRisk(
      ApplicationUser user, UUID scopeId, UUID scenarioId, ScopeRiskDto dto, String eTag) {

    var client = getClient(user.getClientId());
    var input =
        new UpdateScopeRiskUseCase.InputData(
            client,
            scopeId,
            urlAssembler.toKey(dto.getScenario()),
            urlAssembler.toKeys(dto.getDomainReferences()),
            urlAssembler.toKey(dto.getMitigation()),
            urlAssembler.toKey(dto.getRiskOwner()),
            eTag,
            CategorizedRiskValueMapper.map(dto.getDomainsWithRiskValues()));

    // update risk and return saved risk with updated ETag, timestamps etc.:
    return useCaseInteractor
        .execute(updateScopeRiskUseCase, input, output -> null)
        .thenCompose(o -> this.getRisk(client, scopeId, scenarioId));
  }

  @Override
  public Future<ResponseEntity<RequirementImplementationDto>> getRequirementImplementation(
      Authentication auth, UUID riskAffectedId, UUID controlId) {
    return useCaseInteractor.execute(
        getRequirementImplementationUseCase,
        new GetRequirementImplementationUseCase.InputData(
            getAuthenticatedClient(auth),
            TypedId.from(riskAffectedId, Scope.class),
            TypedId.from(controlId, Control.class)),
        out ->
            ResponseEntity.ok()
                .eTag(out.eTag())
                .body(
                    entityToDtoTransformer.transformRequirementImplementation2Dto(
                        out.requirementImplementation())));
  }

  @Override
  public Future<ResponseEntity<ApiResponseBody>> updateRequirementImplementation(
      String eTag,
      Authentication auth,
      UUID riskAffectedId,
      UUID controlId,
      RequirementImplementationDto dto) {
    return useCaseInteractor.execute(
        updateRequirementImplementationUseCase,
        new UpdateRequirementImplementationUseCase.InputData(
            getAuthenticatedClient(auth),
            TypedId.from(riskAffectedId, Scope.class),
            TypedId.from(controlId, Control.class),
            dto,
            eTag),
        out -> ResponseEntity.noContent().eTag(out.eTag()).build());
  }

  @Override
  public Future<PageDto<RequirementImplementationDto>> getRequirementImplementations(
      Authentication auth,
      UUID riskAffectedId,
      UUID controlId,
      Integer pageSize,
      Integer pageNumber,
      String sortColumn,
      String sortOrder) {
    return useCaseInteractor.execute(
        getRequirementImplementationsByControlImplementationUseCase,
        GetRequirementImplementationsByControlImplementationInputMapper.map(
            getAuthenticatedClient(auth),
            Scope.class,
            riskAffectedId,
            controlId,
            pageSize,
            pageNumber,
            sortColumn,
            sortOrder),
        out ->
            PagingMapper.toPage(
                out.result(), entityToDtoTransformer::transformRequirementImplementation2Dto));
  }

  protected FullScopeDto entity2Dto(Scope entity) {
    return entityToDtoTransformer.transformScope2Dto(entity, false);
  }

  private FullScopeDto entity2Dto(Scope entity, boolean embedRisks) {
    return entityToDtoTransformer.transformScope2Dto(entity, false, embedRisks);
  }
}
