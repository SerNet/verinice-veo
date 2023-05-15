/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2018  Alexander Koderman.
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
import static org.veo.rest.ControllerConstants.UUID_REGEX;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import jakarta.validation.Valid;
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
import org.veo.adapter.presenter.api.dto.PageDto;
import org.veo.adapter.presenter.api.dto.SearchQueryDto;
import org.veo.adapter.presenter.api.dto.create.CreateProcessDto;
import org.veo.adapter.presenter.api.dto.full.FullProcessDto;
import org.veo.adapter.presenter.api.dto.full.ProcessRiskDto;
import org.veo.adapter.presenter.api.io.mapper.CategorizedRiskValueMapper;
import org.veo.adapter.presenter.api.io.mapper.CreateElementInputMapper;
import org.veo.adapter.presenter.api.io.mapper.CreateOutputMapper;
import org.veo.adapter.presenter.api.io.mapper.GetProcessesInputMapper;
import org.veo.adapter.presenter.api.io.mapper.PagingMapper;
import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.entity.Process;
import org.veo.core.entity.inspection.Finding;
import org.veo.core.usecase.InspectElementUseCase;
import org.veo.core.usecase.base.CreateElementUseCase;
import org.veo.core.usecase.base.DeleteElementUseCase;
import org.veo.core.usecase.base.ModifyElementUseCase.InputData;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.decision.EvaluateElementUseCase;
import org.veo.core.usecase.process.CreateProcessRiskUseCase;
import org.veo.core.usecase.process.CreateProcessUseCase;
import org.veo.core.usecase.process.GetProcessRiskUseCase;
import org.veo.core.usecase.process.GetProcessRisksUseCase;
import org.veo.core.usecase.process.GetProcessUseCase;
import org.veo.core.usecase.process.GetProcessesUseCase;
import org.veo.core.usecase.process.UpdateProcessRiskUseCase;
import org.veo.core.usecase.process.UpdateProcessUseCase;
import org.veo.core.usecase.risk.DeleteRiskUseCase;
import org.veo.core.usecase.service.IdRefResolver;
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

/**
 * Controller for the resource API of "Process" entities.
 *
 * <p>A process is a business entity
 */
@RestController
@RequestMapping(ProcessController.URL_BASE_PATH)
@Slf4j
public class ProcessController extends AbstractElementController<Process, FullProcessDto>
    implements ProcessRiskResource {

  public static final String URL_BASE_PATH = "/" + Process.PLURAL_TERM;
  public static final String EMBED_RISKS_PARAM = "embedRisks";

  private final CreateProcessUseCase createProcessUseCase;
  private final UpdateProcessUseCase updateProcessUseCase;
  private final DeleteElementUseCase deleteElementUseCase;
  private final GetProcessesUseCase getProcessesUseCase;
  private final GetProcessRiskUseCase getProcessRiskUseCase;
  private final CreateProcessRiskUseCase createProcessRiskUseCase;
  private final GetProcessRisksUseCase getProcessRisksUseCase;
  private final DeleteRiskUseCase deleteRiskUseCase;
  private final UpdateProcessRiskUseCase updateProcessRiskUseCase;
  private final GetProcessUseCase getProcessUseCase;

  public ProcessController(
      CreateProcessUseCase createProcessUseCase,
      GetProcessUseCase getProcessUseCase,
      UpdateProcessUseCase putProcessUseCase,
      DeleteElementUseCase deleteElementUseCase,
      GetProcessesUseCase getProcessesUseCase,
      CreateProcessRiskUseCase createProcessRiskUseCase,
      GetProcessRiskUseCase getProcessRiskUseCase,
      GetProcessRisksUseCase getProcessRisksUseCase,
      DeleteRiskUseCase deleteRiskUseCase,
      UpdateProcessRiskUseCase updateProcessRiskUseCase,
      EvaluateElementUseCase evaluateElementUseCase,
      InspectElementUseCase inspectElementUseCase) {
    super(Process.class, getProcessUseCase, evaluateElementUseCase, inspectElementUseCase);
    this.createProcessUseCase = createProcessUseCase;
    this.updateProcessUseCase = putProcessUseCase;
    this.deleteElementUseCase = deleteElementUseCase;
    this.getProcessesUseCase = getProcessesUseCase;
    this.createProcessRiskUseCase = createProcessRiskUseCase;
    this.getProcessRiskUseCase = getProcessRiskUseCase;
    this.getProcessRisksUseCase = getProcessRisksUseCase;
    this.deleteRiskUseCase = deleteRiskUseCase;
    this.updateProcessRiskUseCase = updateProcessRiskUseCase;
    this.getProcessUseCase = getProcessUseCase;
  }

  @Operation(summary = "Loads a process")
  @ApiResponse(
      responseCode = "200",
      description = "Process loaded",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = FullProcessDto.class)))
  @ApiResponse(responseCode = "404", description = "Process not found")
  @GetMapping(ControllerConstants.UUID_PARAM_SPEC)
  public @Valid Future<ResponseEntity<FullProcessDto>> getProcess(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String uuid,
      @RequestParam(name = EMBED_RISKS_PARAM, required = false, defaultValue = "false")
          @Parameter(name = EMBED_RISKS_PARAM, description = EMBED_RISKS_DESC)
          Boolean embedRisksParam,
      WebRequest request) {
    boolean embedRisks = (embedRisksParam != null) && embedRisksParam;
    ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
    Client client = getClient(user.getClientId());
    if (getEtag(Process.class, uuid).map(request::checkNotModified).orElse(false)) {
      return null;
    }
    CompletableFuture<FullProcessDto> entityFuture =
        useCaseInteractor.execute(
            getProcessUseCase,
            new GetProcessUseCase.InputData(Key.uuidFrom(uuid), client, embedRisks),
            output -> entity2Dto(output.getElement(), embedRisks));
    return entityFuture.thenApply(
        dto -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(dto));
  }

  @Override
  @Operation(summary = "Loads the parts of a process")
  @ApiResponse(
      responseCode = "200",
      description = "Parts loaded",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = FullProcessDto.class))))
  @ApiResponse(responseCode = "404", description = "Process not found")
  @GetMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}/parts")
  public @Valid CompletableFuture<ResponseEntity<List<FullProcessDto>>> getElementParts(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String uuid,
      WebRequest request) {
    return super.getElementParts(auth, uuid, request);
  }

  @PostMapping()
  @Operation(summary = "Creates a process")
  @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Process created")})
  public CompletableFuture<ResponseEntity<ApiResponseBody>> createProcess(
      @Parameter(hidden = true) ApplicationUser user,
      @Valid @NotNull @RequestBody CreateProcessDto dto,
      @Parameter(description = SCOPE_IDS_DESCRIPTION)
          @RequestParam(name = SCOPE_IDS_PARAM, required = false)
          List<String> scopeIds) {

    return useCaseInteractor.execute(
        createProcessUseCase,
        (Supplier<CreateElementUseCase.InputData<Process>>)
            () -> {
              Client client = getClient(user);
              IdRefResolver idRefResolver = createIdRefResolver(client);
              return CreateElementInputMapper.map(
                  dtoToEntityTransformer.transformDto2Process(dto, idRefResolver),
                  client,
                  scopeIds);
            },
        output -> {
          ApiResponseBody body = CreateOutputMapper.map(output.getEntity());
          return RestApiResponse.created(URL_BASE_PATH, body);
        });
  }

  @PutMapping(value = "/{id}")
  @Operation(summary = "Updates a process")
  @ApiResponse(responseCode = "200", description = "Process updated")
  @ApiResponse(responseCode = "404", description = "Process not found")
  public @Valid CompletableFuture<ResponseEntity<FullProcessDto>> updateProcess(
      @Parameter(hidden = true) ApplicationUser user,
      @RequestHeader(ControllerConstants.IF_MATCH_HEADER) @NotBlank String eTag,
      @PathVariable String id,
      @Valid @RequestBody FullProcessDto processDto) {
    processDto.applyResourceId(id);
    return useCaseInteractor.execute(
        updateProcessUseCase,
        new InputData<>(id, processDto, getClient(user), eTag, user.getUsername()),
        output -> toResponseEntity(output.getEntity()));
  }

  @DeleteMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
  @Operation(summary = "Deletes a process")
  @ApiResponse(responseCode = "204", description = "Process deleted")
  @ApiResponse(responseCode = "404", description = "Process not found")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteProcess(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String uuid) {
    return useCaseInteractor.execute(
        deleteElementUseCase,
        new DeleteElementUseCase.InputData(
            Process.class, Key.uuidFrom(uuid), getAuthenticatedClient(auth)),
        output -> ResponseEntity.noContent().build());
  }

  @GetMapping
  @Operation(summary = "Loads all processes")
  public @Valid Future<PageDto<FullProcessDto>> getProcesses(
      @Parameter(hidden = true) Authentication auth,
      @UnitUuidParam @RequestParam(value = UNIT_PARAM, required = false) String parentUuid,
      @RequestParam(value = DISPLAY_NAME_PARAM, required = false) String displayName,
      @RequestParam(value = SUB_TYPE_PARAM, required = false) String subType,
      @RequestParam(value = STATUS_PARAM, required = false) String status,
      @RequestParam(value = CHILD_ELEMENT_IDS_PARAM, required = false) List<String> childElementIds,
      @RequestParam(value = HAS_PARENT_ELEMENTS_PARAM, required = false) Boolean hasParentElements,
      @RequestParam(value = HAS_CHILD_ELEMENTS_PARAM, required = false) Boolean hasChildElements,
      @RequestParam(value = DESCRIPTION_PARAM, required = false) String description,
      @RequestParam(value = DESIGNATOR_PARAM, required = false) String designator,
      @RequestParam(value = NAME_PARAM, required = false) String name,
      @RequestParam(value = UPDATED_BY_PARAM, required = false) String updatedBy,
      @RequestParam(
              value = PAGE_SIZE_PARAM,
              required = false,
              defaultValue = PAGE_SIZE_DEFAULT_VALUE)
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

    return getProcesses(
        GetProcessesInputMapper.map(
            client,
            parentUuid,
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
            updatedBy,
            PagingMapper.toConfig(pageSize, pageNumber, sortColumn, sortOrder),
            embedRisks));
  }

  private CompletableFuture<PageDto<FullProcessDto>> getProcesses(
      GetProcessesUseCase.InputData inputData) {

    return useCaseInteractor.execute(
        getProcessesUseCase,
        inputData,
        output ->
            PagingMapper.toPage(
                output.getElements(), process -> entity2Dto(process, inputData.isEmbedRisks())));
  }

  @Override
  @SuppressFBWarnings("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
  protected String buildSearchUri(String id) {
    return linkTo(
            methodOn(ProcessController.class)
                .runSearch(ANY_AUTH, id, ANY_INT, ANY_INT, ANY_STRING, ANY_STRING, ANY_BOOLEAN))
        .withSelfRel()
        .getHref();
  }

  @GetMapping(value = "/searches/{searchId}")
  @Operation(summary = "Finds processes for the search.")
  public @Valid Future<PageDto<FullProcessDto>> runSearch(
      @Parameter(hidden = true) Authentication auth,
      @PathVariable String searchId,
      @RequestParam(
              value = PAGE_SIZE_PARAM,
              required = false,
              defaultValue = PAGE_SIZE_DEFAULT_VALUE)
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
      return getProcesses(
          GetProcessesInputMapper.map(
              getAuthenticatedClient(auth),
              SearchQueryDto.decodeFromSearchId(searchId),
              PagingMapper.toConfig(pageSize, pageNumber, sortColumn, sortOrder),
              embedRisks));
    } catch (IOException e) {
      log.error("Could not decode search URL: {}", e.getLocalizedMessage());
      return null;
    }
  }

  @Operation(
      summary =
          "Evaluates decisions and inspections on a transient process without persisting anything")
  @ApiResponse(
      responseCode = "200",
      description = "Element evaluated",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = EvaluateElementOutputSchema.class)))
  @PostMapping(value = "/evaluation")
  public @Valid CompletableFuture<ResponseEntity<EvaluateElementUseCase.OutputData>> evaluate(
      @Parameter(required = true, hidden = true) Authentication auth,
      @Valid @RequestBody FullProcessDto element,
      @RequestParam(value = DOMAIN_PARAM) String domainId) {
    return super.evaluate(auth, element, domainId);
  }

  @Override
  public @Valid CompletableFuture<List<ProcessRiskDto>> getRisks(
      @Parameter(hidden = true) ApplicationUser user, String processId) {

    Client client = getClient(user.getClientId());
    var input = new GetProcessRisksUseCase.InputData(client, Key.uuidFrom(processId));

    return useCaseInteractor.execute(
        getProcessRisksUseCase,
        input,
        output ->
            output.getRisks().stream()
                .map(risk -> ProcessRiskDto.from(risk, referenceAssembler))
                .toList());
  }

  @Override
  public @Valid Future<ResponseEntity<ProcessRiskDto>> getRisk(
      @Parameter(hidden = true) ApplicationUser user, String processId, String scenarioId) {

    Client client = getClient(user.getClientId());
    return getRisk(client, processId, scenarioId);
  }

  private CompletableFuture<ResponseEntity<ProcessRiskDto>> getRisk(
      Client client, String processId, String scenarioId) {
    var input =
        new GetProcessRiskUseCase.InputData(
            client, Key.uuidFrom(processId), Key.uuidFrom(scenarioId));

    var riskFuture =
        useCaseInteractor.execute(
            getProcessRiskUseCase,
            input,
            output -> ProcessRiskDto.from(output.getRisk(), referenceAssembler));

    return riskFuture.thenApply(
        riskDto ->
            ResponseEntity.ok()
                .eTag(
                    ETag.from(
                        riskDto.getProcess().getId(),
                        riskDto.getScenario().getId(),
                        riskDto.getVersion()))
                .body(riskDto));
  }

  @Override
  public CompletableFuture<ResponseEntity<ApiResponseBody>> createRisk(
      ApplicationUser user, @Valid @NotNull ProcessRiskDto dto, String processId) {
    var input =
        new CreateProcessRiskUseCase.InputData(
            getClient(user.getClientId()),
            Key.uuidFrom(processId),
            urlAssembler.toKey(dto.getScenario()),
            urlAssembler.toKeys(dto.getDomainReferences()),
            urlAssembler.toKey(dto.getMitigation()),
            urlAssembler.toKey(dto.getRiskOwner()),
            CategorizedRiskValueMapper.map(dto.getDomainsWithRiskValues()));

    return useCaseInteractor.execute(
        createProcessRiskUseCase,
        input,
        output -> {
          if (!output.isNewlyCreatedRisk()) return RestApiResponse.noContent();

          var url =
              String.format(
                  "%s/%s/%s",
                  URL_BASE_PATH,
                  output.getRisk().getEntity().getId().uuidValue(),
                  ProcessRiskResource.RESOURCE_NAME);
          var body =
              new ApiResponseBody(
                  true,
                  Optional.of(output.getRisk().getScenario().getId().uuidValue()),
                  "Process risk created successfully.");
          return RestApiResponse.created(url, body);
        });
  }

  @Override
  public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteRisk(
      ApplicationUser user, String processId, String scenarioId) {

    Client client = getClient(user.getClientId());
    var input =
        new DeleteRiskUseCase.InputData(
            Process.class, client, Key.uuidFrom(processId), Key.uuidFrom(scenarioId));

    return useCaseInteractor.execute(
        deleteRiskUseCase, input, output -> ResponseEntity.noContent().build());
  }

  @Override
  public @Valid CompletableFuture<ResponseEntity<ProcessRiskDto>> updateRisk(
      ApplicationUser user,
      String processId,
      String scenarioId,
      @Valid @NotNull ProcessRiskDto dto,
      String eTag) {
    var client = getClient(user.getClientId());
    var input =
        new UpdateProcessRiskUseCase.InputData(
            client,
            Key.uuidFrom(processId),
            urlAssembler.toKey(dto.getScenario()),
            urlAssembler.toKeys(dto.getDomainReferences()),
            urlAssembler.toKey(dto.getMitigation()),
            urlAssembler.toKey(dto.getRiskOwner()),
            eTag,
            CategorizedRiskValueMapper.map(dto.getDomainsWithRiskValues()));

    // update risk and return saved risk with updated ETag, timestamps etc.:
    return useCaseInteractor
        .execute(updateProcessRiskUseCase, input, output -> null)
        .thenCompose(o -> this.getRisk(client, processId, scenarioId));
  }

  @Operation(summary = "Runs inspections on a persisted process")
  @ApiResponse(
      responseCode = "200",
      description = "Inspections have run",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = Finding.class))))
  @ApiResponse(responseCode = "404", description = "Process not found")
  @GetMapping(value = UUID_PARAM_SPEC + "/inspection")
  public @Valid CompletableFuture<ResponseEntity<Set<Finding>>> inspect(
      @Parameter(required = true, hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String uuid,
      @RequestParam(value = DOMAIN_PARAM) String domainId) {
    return inspect(auth, uuid, domainId, Process.class);
  }

  @Override
  protected FullProcessDto entity2Dto(Process entity) {
    return entity2Dto(entity, false);
  }

  private FullProcessDto entity2Dto(Process entity, boolean embedRisks) {
    return entityToDtoTransformer.transformProcess2Dto(entity, embedRisks);
  }
}
