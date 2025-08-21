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

import static org.veo.rest.ControllerConstants.ABBREVIATION_PARAM;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.dto.PageDto;
import org.veo.adapter.presenter.api.dto.RequirementImplementationDto;
import org.veo.adapter.presenter.api.dto.full.FullProcessDto;
import org.veo.adapter.presenter.api.dto.full.ProcessRiskDto;
import org.veo.adapter.presenter.api.io.mapper.CategorizedRiskValueMapper;
import org.veo.adapter.presenter.api.io.mapper.PagingMapper;
import org.veo.adapter.presenter.api.io.mapper.QueryInputMapper;
import org.veo.adapter.presenter.api.unit.GetRequirementImplementationsByControlImplementationInputMapper;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.Process;
import org.veo.core.entity.inspection.Finding;
import org.veo.core.entity.ref.TypedId;
import org.veo.core.usecase.InspectElementUseCase;
import org.veo.core.usecase.base.DeleteElementUseCase;
import org.veo.core.usecase.base.GetElementsUseCase;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.compliance.GetRequirementImplementationUseCase;
import org.veo.core.usecase.compliance.GetRequirementImplementationsByControlImplementationUseCase;
import org.veo.core.usecase.compliance.UpdateRequirementImplementationUseCase;
import org.veo.core.usecase.decision.EvaluateElementUseCase;
import org.veo.core.usecase.process.CreateProcessRiskUseCase;
import org.veo.core.usecase.process.GetProcessRiskUseCase;
import org.veo.core.usecase.process.GetProcessRisksUseCase;
import org.veo.core.usecase.process.GetProcessUseCase;
import org.veo.core.usecase.process.UpdateProcessRiskUseCase;
import org.veo.core.usecase.risk.DeleteRiskUseCase;
import org.veo.rest.annotations.UnitUuidParam;
import org.veo.rest.common.RestApiResponse;
import org.veo.rest.schemas.EvaluateElementOutputSchema;
import org.veo.rest.security.ApplicationUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for the resource API of "Process" entities.
 *
 * <p>A process is a business entity
 */
@RestController
@RequestMapping(ProcessController.URL_BASE_PATH)
@Slf4j
public class ProcessController extends AbstractCompositeElementController<Process, FullProcessDto>
    implements ProcessRiskResource, RiskAffectedResource {

  public static final String URL_BASE_PATH = "/" + Process.PLURAL_TERM;
  public static final String EMBED_RISKS_PARAM = "embedRisks";

  private final DeleteElementUseCase deleteElementUseCase;
  private final GetProcessRiskUseCase getProcessRiskUseCase;
  private final CreateProcessRiskUseCase createProcessRiskUseCase;
  private final GetProcessRisksUseCase getProcessRisksUseCase;
  private final DeleteRiskUseCase deleteRiskUseCase;
  private final UpdateProcessRiskUseCase updateProcessRiskUseCase;
  private final GetProcessUseCase getProcessUseCase;
  private final GetRequirementImplementationsByControlImplementationUseCase
      getRequirementImplementationsByControlImplementationUseCase;
  private final GetRequirementImplementationUseCase getRequirementImplementationUseCase;
  private final UpdateRequirementImplementationUseCase updateRequirementImplementationUseCase;

  public ProcessController(
      GetProcessUseCase getProcessUseCase,
      DeleteElementUseCase deleteElementUseCase,
      GetElementsUseCase getElementsUseCase,
      CreateProcessRiskUseCase createProcessRiskUseCase,
      GetProcessRiskUseCase getProcessRiskUseCase,
      GetProcessRisksUseCase getProcessRisksUseCase,
      DeleteRiskUseCase deleteRiskUseCase,
      UpdateProcessRiskUseCase updateProcessRiskUseCase,
      EvaluateElementUseCase evaluateElementUseCase,
      InspectElementUseCase inspectElementUseCase,
      GetRequirementImplementationsByControlImplementationUseCase
          getRequirementImplementationsByControlImplementationUseCase,
      GetRequirementImplementationUseCase getRequirementImplementationUseCase,
      UpdateRequirementImplementationUseCase updateRequirementImplementationUseCase) {
    super(
        ElementType.PROCESS,
        getProcessUseCase,
        evaluateElementUseCase,
        inspectElementUseCase,
        getElementsUseCase);
    this.deleteElementUseCase = deleteElementUseCase;
    this.createProcessRiskUseCase = createProcessRiskUseCase;
    this.getProcessRiskUseCase = getProcessRiskUseCase;
    this.getProcessRisksUseCase = getProcessRisksUseCase;
    this.deleteRiskUseCase = deleteRiskUseCase;
    this.updateProcessRiskUseCase = updateProcessRiskUseCase;
    this.getProcessUseCase = getProcessUseCase;
    this.getRequirementImplementationsByControlImplementationUseCase =
        getRequirementImplementationsByControlImplementationUseCase;
    this.getRequirementImplementationUseCase = getRequirementImplementationUseCase;
    this.updateRequirementImplementationUseCase = updateRequirementImplementationUseCase;
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
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid,
      @RequestParam(name = EMBED_RISKS_PARAM, required = false, defaultValue = "false")
          @Parameter(name = EMBED_RISKS_PARAM, description = EMBED_RISKS_DESC)
          Boolean embedRisksParam,
      WebRequest request) {
    boolean embedRisks = (embedRisksParam != null) && embedRisksParam;
    if (getEtag(Process.class, uuid).map(request::checkNotModified).orElse(false)) {
      return null;
    }
    CompletableFuture<FullProcessDto> entityFuture =
        useCaseInteractor.execute(
            getProcessUseCase,
            new GetProcessUseCase.InputData(uuid, null, embedRisks),
            output -> entity2Dto(output.element(), embedRisks));
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
  @GetMapping(value = "/{" + UUID_PARAM + "}/parts")
  public CompletableFuture<ResponseEntity<List<FullProcessDto>>> getElementParts(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid,
      WebRequest request) {
    return super.getElementParts(uuid, request);
  }

  @DeleteMapping(value = "/{" + UUID_PARAM + "}")
  @Operation(summary = "Deletes a process")
  @ApiResponse(responseCode = "204", description = "Process deleted")
  @ApiResponse(responseCode = "404", description = "Process not found")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteProcess(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid) {
    return useCaseInteractor.execute(
        deleteElementUseCase,
        new DeleteElementUseCase.InputData(Process.class, uuid),
        output -> ResponseEntity.noContent().build());
  }

  @GetMapping
  @Operation(summary = "Loads all processes")
  public @Valid Future<PageDto<FullProcessDto>> getProcesses(
      @Parameter(hidden = true) ApplicationUser user,
      @UnitUuidParam @RequestParam(value = UNIT_PARAM, required = false) UUID parentUuid,
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
    Client client = getClient(user);
    boolean embedRisks = (embedRisksParam != null) && embedRisksParam;

    return getElements(
        QueryInputMapper.map(
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
                abbreviation,
                updatedBy,
                PagingMapper.toConfig(pageSize, pageNumber, sortColumn, sortOrder))
            .withEmbedRisks(embedRisks),
        e -> entity2Dto(e, embedRisks));
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
  @Override
  public CompletableFuture<ResponseEntity<EvaluateElementUseCase.OutputData>> evaluate(
      @Parameter(required = true, hidden = true) ApplicationUser user,
      @Valid @RequestBody FullProcessDto element,
      @RequestParam(value = DOMAIN_PARAM) String domainId) {
    return super.evaluate(user, element, domainId);
  }

  @Override
  public Future<List<ProcessRiskDto>> getRisks(UUID processId) {

    var input = new GetProcessRisksUseCase.InputData(processId);

    return useCaseInteractor.execute(
        getProcessRisksUseCase,
        input,
        output ->
            output.getRisks().stream()
                .map(risk -> ProcessRiskDto.from(risk, referenceAssembler))
                .toList());
  }

  @Override
  public Future<ResponseEntity<ProcessRiskDto>> getRisk(UUID processId, UUID scenarioId) {
    return getRiskInternal(processId, scenarioId);
  }

  private CompletableFuture<ResponseEntity<ProcessRiskDto>> getRiskInternal(
      UUID processId, UUID scenarioId) {
    var input = new GetProcessRiskUseCase.InputData(processId, scenarioId);

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
      ApplicationUser user, @Valid @NotNull ProcessRiskDto dto, UUID processId) {
    var input =
        new CreateProcessRiskUseCase.InputData(
            getClient(user.getClientId()),
            processId,
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
                  output.getRisk().getEntity().getIdAsString(),
                  ProcessRiskResource.RESOURCE_NAME);
          var body =
              new ApiResponseBody(
                  true,
                  Optional.of(output.getRisk().getScenario().getIdAsString()),
                  "Process risk created successfully.");
          return RestApiResponse.created(url, body);
        });
  }

  @Override
  public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteRisk(
      ApplicationUser user, UUID processId, UUID scenarioId) {

    Client client = getClient(user.getClientId());
    var input = new DeleteRiskUseCase.InputData(user, Process.class, client, processId, scenarioId);

    return useCaseInteractor.execute(
        deleteRiskUseCase, input, output -> ResponseEntity.noContent().build());
  }

  @Override
  public CompletableFuture<ResponseEntity<ProcessRiskDto>> updateRisk(
      ApplicationUser user, UUID processId, UUID scenarioId, ProcessRiskDto dto, String eTag) {
    var client = getClient(user.getClientId());
    var input =
        new UpdateProcessRiskUseCase.InputData(
            client,
            processId,
            urlAssembler.toKey(dto.getScenario()),
            urlAssembler.toKeys(dto.getDomainReferences()),
            urlAssembler.toKey(dto.getMitigation()),
            urlAssembler.toKey(dto.getRiskOwner()),
            eTag,
            CategorizedRiskValueMapper.map(dto.getDomainsWithRiskValues()));

    // update risk and return saved risk with updated ETag, timestamps etc.:
    return useCaseInteractor
        .execute(updateProcessRiskUseCase, input, output -> null)
        .thenCompose(o -> this.getRiskInternal(processId, scenarioId));
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
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid,
      @RequestParam(value = DOMAIN_PARAM) UUID domainId) {
    return inspect(uuid, domainId, Process.class);
  }

  @Override
  public Future<ResponseEntity<RequirementImplementationDto>> getRequirementImplementation(
      UUID riskAffectedId, UUID controlId) {
    return useCaseInteractor.execute(
        getRequirementImplementationUseCase,
        new GetRequirementImplementationUseCase.InputData(
            TypedId.from(riskAffectedId, Process.class), TypedId.from(controlId, Control.class)),
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
      ApplicationUser user,
      UUID riskAffectedId,
      UUID controlId,
      RequirementImplementationDto dto) {
    return useCaseInteractor.execute(
        updateRequirementImplementationUseCase,
        new UpdateRequirementImplementationUseCase.InputData(
            user,
            getClient(user),
            TypedId.from(riskAffectedId, Process.class),
            TypedId.from(controlId, Control.class),
            dto,
            eTag),
        out -> ResponseEntity.noContent().eTag(out.eTag()).build());
  }

  @Override
  public Future<PageDto<RequirementImplementationDto>> getRequirementImplementations(
      ApplicationUser user,
      UUID riskAffectedId,
      UUID controlId,
      Integer pageSize,
      Integer pageNumber,
      String sortColumn,
      String sortOrder) {
    return useCaseInteractor.execute(
        getRequirementImplementationsByControlImplementationUseCase,
        GetRequirementImplementationsByControlImplementationInputMapper.map(
            getClient(user),
            Process.class,
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

  @Override
  protected FullProcessDto entity2Dto(Process entity) {
    return entity2Dto(entity, false);
  }

  private FullProcessDto entity2Dto(Process entity, boolean embedRisks) {
    return entityToDtoTransformer.transformProcess2Dto(entity, false, embedRisks);
  }
}
