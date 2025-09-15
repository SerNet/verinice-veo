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
import org.veo.adapter.presenter.api.dto.full.AssetRiskDto;
import org.veo.adapter.presenter.api.dto.full.FullAssetDto;
import org.veo.adapter.presenter.api.io.mapper.CategorizedRiskValueMapper;
import org.veo.adapter.presenter.api.io.mapper.PagingMapper;
import org.veo.adapter.presenter.api.io.mapper.QueryInputMapper;
import org.veo.adapter.presenter.api.unit.GetRequirementImplementationsByControlImplementationInputMapper;
import org.veo.core.entity.Asset;
import org.veo.core.entity.Client;
import org.veo.core.entity.Control;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.inspection.Finding;
import org.veo.core.entity.ref.TypedId;
import org.veo.core.usecase.InspectElementUseCase;
import org.veo.core.usecase.UseCase.EntityId;
import org.veo.core.usecase.asset.CreateAssetRiskUseCase;
import org.veo.core.usecase.asset.GetAssetRiskUseCase;
import org.veo.core.usecase.asset.GetAssetRisksUseCase;
import org.veo.core.usecase.asset.GetAssetUseCase;
import org.veo.core.usecase.asset.UpdateAssetRiskUseCase;
import org.veo.core.usecase.base.DeleteElementUseCase;
import org.veo.core.usecase.base.GetElementsUseCase;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.compliance.GetRequirementImplementationUseCase;
import org.veo.core.usecase.compliance.GetRequirementImplementationsByControlImplementationUseCase;
import org.veo.core.usecase.compliance.UpdateRequirementImplementationUseCase;
import org.veo.core.usecase.decision.EvaluateElementUseCase;
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

/** REST service which provides methods to manage assets. */
@RestController
@RequestMapping(AssetController.URL_BASE_PATH)
@Slf4j
public class AssetController extends AbstractCompositeElementController<Asset, FullAssetDto>
    implements AssetRiskResource, RiskAffectedResource {
  public static final String EMBED_RISKS_PARAM = "embedRisks";
  private final DeleteRiskUseCase deleteRiskUseCase;
  private final UpdateAssetRiskUseCase updateAssetRiskUseCase;
  private final GetAssetRisksUseCase getAssetRisksUseCase;
  private final GetRequirementImplementationsByControlImplementationUseCase
      getRequirementImplementationsByControlImplementationUseCase;
  private final GetRequirementImplementationUseCase getRequirementImplementationUseCase;
  private final UpdateRequirementImplementationUseCase updateRequirementImplementationUseCase;

  public AssetController(
      GetAssetUseCase getAssetUseCase,
      GetElementsUseCase getElementsUseCase,
      DeleteElementUseCase deleteElementUseCase,
      CreateAssetRiskUseCase createAssetRiskUseCase,
      GetAssetRiskUseCase getAssetRiskUseCase,
      DeleteRiskUseCase deleteRiskUseCase,
      UpdateAssetRiskUseCase updateAssetRiskUseCase,
      GetAssetRisksUseCase getAssetRisksUseCase,
      EvaluateElementUseCase evaluateElementUseCase,
      InspectElementUseCase inspectElementUseCase,
      GetRequirementImplementationsByControlImplementationUseCase
          getRequirementImplementationsByControlImplementationUseCase,
      GetRequirementImplementationUseCase getRequirementImplementationUseCase,
      UpdateRequirementImplementationUseCase updateRequirementImplementationUseCase) {
    super(
        ElementType.ASSET,
        getAssetUseCase,
        evaluateElementUseCase,
        inspectElementUseCase,
        getElementsUseCase);
    this.deleteElementUseCase = deleteElementUseCase;
    this.createAssetRiskUseCase = createAssetRiskUseCase;
    this.getAssetRiskUseCase = getAssetRiskUseCase;
    this.deleteRiskUseCase = deleteRiskUseCase;
    this.updateAssetRiskUseCase = updateAssetRiskUseCase;
    this.getAssetRisksUseCase = getAssetRisksUseCase;
    this.getAssetUseCase = getAssetUseCase;
    this.getRequirementImplementationUseCase = getRequirementImplementationUseCase;
    this.getRequirementImplementationsByControlImplementationUseCase =
        getRequirementImplementationsByControlImplementationUseCase;
    this.updateRequirementImplementationUseCase = updateRequirementImplementationUseCase;
  }

  public static final String URL_BASE_PATH = "/" + Asset.PLURAL_TERM;

  private final GetAssetUseCase getAssetUseCase;
  private final DeleteElementUseCase deleteElementUseCase;
  private final CreateAssetRiskUseCase createAssetRiskUseCase;
  private final GetAssetRiskUseCase getAssetRiskUseCase;

  @GetMapping
  @Operation(summary = "Loads all assets")
  public @Valid Future<PageDto<FullAssetDto>> getAssets(
      @Parameter(hidden = true) ApplicationUser user,
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
    Client client = getClient(user);
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

  @Operation(summary = "Loads an asset")
  @ApiResponse(
      responseCode = "200",
      description = "Asset loaded",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = FullAssetDto.class)))
  @ApiResponse(responseCode = "404", description = "Asset not found")
  @GetMapping(ControllerConstants.UUID_PARAM_SPEC)
  public @Valid Future<ResponseEntity<FullAssetDto>> getAsset(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid,
      @RequestParam(name = EMBED_RISKS_PARAM, required = false, defaultValue = "false")
          @Parameter(name = EMBED_RISKS_PARAM, description = EMBED_RISKS_DESC)
          Boolean embedRisksParam,
      WebRequest request) {
    boolean embedRisks = (embedRisksParam != null) && embedRisksParam;
    if (getEtag(Asset.class, uuid).map(request::checkNotModified).orElse(false)) {
      return null;
    }
    CompletableFuture<FullAssetDto> entityFuture =
        useCaseInteractor.execute(
            getAssetUseCase,
            new GetAssetUseCase.InputData(uuid, null, embedRisks),
            output -> entity2Dto(output.element(), embedRisks));
    return entityFuture.thenApply(
        dto -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(dto));
  }

  @Override
  @Operation(summary = "Loads the parts of an asset")
  @ApiResponse(
      responseCode = "200",
      description = "Parts loaded",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = FullAssetDto.class))))
  @ApiResponse(responseCode = "404", description = "Asset not found")
  @GetMapping(value = "/{" + UUID_PARAM + "}/parts")
  public CompletableFuture<ResponseEntity<List<FullAssetDto>>> getElementParts(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid,
      WebRequest request) {
    return super.getElementParts(uuid, request);
  }

  @DeleteMapping(ControllerConstants.UUID_PARAM_SPEC)
  @Operation(summary = "Deletes an asset")
  @ApiResponse(responseCode = "204", description = "Asset deleted")
  @ApiResponse(responseCode = "404", description = "Asset not found")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteAsset(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid) {
    return useCaseInteractor.execute(
        deleteElementUseCase,
        new DeleteElementUseCase.InputData(Asset.class, uuid),
        output -> ResponseEntity.noContent().build());
  }

  @Operation(
      summary =
          "Evaluates decisions and inspections on a transient asset without persisting anything")
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
      @Parameter(required = true, hidden = true) ApplicationUser auth,
      @Valid @RequestBody FullAssetDto element,
      @RequestParam(value = DOMAIN_PARAM) String domainId) {
    return super.evaluate(auth, element, domainId);
  }

  @Override
  public Future<List<AssetRiskDto>> getRisks(UUID assetId) {
    var input = new EntityId(assetId);

    return useCaseInteractor.execute(
        getAssetRisksUseCase,
        input,
        output ->
            output.getRisks().stream()
                .map(risk -> AssetRiskDto.from(risk, referenceAssembler))
                .toList());
  }

  @Override
  public Future<ResponseEntity<AssetRiskDto>> getRisk(UUID assetId, UUID scenarioId) {
    return getRiskInternal(assetId, scenarioId);
  }

  private CompletableFuture<ResponseEntity<AssetRiskDto>> getRiskInternal(
      UUID assetId, UUID scenarioId) {
    var input = new GetAssetRiskUseCase.InputData(assetId, scenarioId);

    var riskFuture =
        useCaseInteractor.execute(
            getAssetRiskUseCase,
            input,
            output -> AssetRiskDto.from(output.getRisk(), referenceAssembler));

    return riskFuture.thenApply(
        riskDto ->
            ResponseEntity.ok()
                .eTag(
                    ETag.from(
                        riskDto.getAsset().getId(),
                        riskDto.getScenario().getId(),
                        riskDto.getVersion()))
                .body(riskDto));
  }

  @Override
  public CompletableFuture<ResponseEntity<ApiResponseBody>> createRisk(
      ApplicationUser user, @Valid @NotNull AssetRiskDto dto, UUID assetId) {

    var input =
        new CreateAssetRiskUseCase.InputData(
            getClient(user.getClientId()),
            assetId,
            urlAssembler.toKey(dto.getScenario()),
            urlAssembler.toKeys(dto.getDomainReferences()),
            urlAssembler.toKey(dto.getMitigation()),
            urlAssembler.toKey(dto.getRiskOwner()),
            CategorizedRiskValueMapper.map(dto.getDomainsWithRiskValues()));

    return useCaseInteractor.execute(
        createAssetRiskUseCase,
        input,
        output -> {
          if (!output.isNewlyCreatedRisk()) return RestApiResponse.noContent();

          var url =
              String.format(
                  "%s/%s/%s",
                  URL_BASE_PATH,
                  output.getRisk().getEntity().getIdAsString(),
                  AssetRiskResource.RESOURCE_NAME);
          var body =
              new ApiResponseBody(
                  true,
                  Optional.of(output.getRisk().getScenario().getIdAsString()),
                  "Asset risk created successfully.");
          return RestApiResponse.created(url, body);
        });
  }

  @Override
  public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteRisk(
      ApplicationUser user, UUID assetId, UUID scenarioId) {

    Client client = getClient(user.getClientId());
    var input = new DeleteRiskUseCase.InputData(user, Asset.class, client, assetId, scenarioId);

    return useCaseInteractor.execute(
        deleteRiskUseCase, input, output -> ResponseEntity.noContent().build());
  }

  @Override
  public CompletableFuture<ResponseEntity<AssetRiskDto>> updateRisk(
      ApplicationUser user, UUID assetId, UUID scenarioId, AssetRiskDto dto, String eTag) {
    var client = getClient(user.getClientId());
    var input =
        new UpdateAssetRiskUseCase.InputData(
            client,
            assetId,
            urlAssembler.toKey(dto.getScenario()),
            urlAssembler.toKeys(dto.getDomainReferences()),
            urlAssembler.toKey(dto.getMitigation()),
            urlAssembler.toKey(dto.getRiskOwner()),
            eTag,
            CategorizedRiskValueMapper.map(dto.getDomainsWithRiskValues()));

    // update risk and return saved risk with updated ETag, timestamps etc.:
    return useCaseInteractor
        .execute(updateAssetRiskUseCase, input, output -> null)
        .thenCompose(o -> this.getRiskInternal(assetId, scenarioId));
  }

  @Operation(summary = "Runs inspections on a persisted asset")
  @ApiResponse(
      responseCode = "200",
      description = "Inspections have run",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = Finding.class))))
  @ApiResponse(responseCode = "404", description = "Asset not found")
  @GetMapping(value = UUID_PARAM_SPEC + "/inspection")
  public @Valid CompletableFuture<ResponseEntity<Set<Finding>>> inspect(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID uuid,
      @RequestParam(value = DOMAIN_PARAM) UUID domainId) {
    return inspect(uuid, domainId, Asset.class);
  }

  @Override
  public Future<ResponseEntity<RequirementImplementationDto>> getRequirementImplementation(
      UUID riskAffectedId, UUID controlId) {
    return useCaseInteractor.execute(
        getRequirementImplementationUseCase,
        new GetRequirementImplementationUseCase.InputData(
            TypedId.from(riskAffectedId, Asset.class), TypedId.from(controlId, Control.class)),
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
            TypedId.from(riskAffectedId, Asset.class),
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
            Asset.class,
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
  protected FullAssetDto entity2Dto(Asset entity) {
    return entityToDtoTransformer.transformAsset2Dto(entity, false);
  }

  private FullAssetDto entity2Dto(Asset entity, boolean embedRisks) {
    return entityToDtoTransformer.transformAsset2Dto(entity, false, embedRisks);
  }
}
