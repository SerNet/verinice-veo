/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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

import static org.veo.core.entity.DomainBase.INSPECTION_ID_MAX_LENGTH;
import static org.veo.rest.ControllerConstants.ABBREVIATION_PARAM;
import static org.veo.rest.ControllerConstants.CUSTOM_ASPECTS_PARAM;
import static org.veo.rest.ControllerConstants.DESCRIPTION_PARAM;
import static org.veo.rest.ControllerConstants.ELEMENT_TYPE_PARAM;
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
import static org.veo.rest.ControllerConstants.SUB_TYPE_PARAM;
import static org.veo.rest.ControllerConstants.UNIT_PARAM;
import static org.veo.rest.ControllerConstants.UUID_DESCRIPTION;
import static org.veo.rest.ControllerConstants.UUID_EXAMPLE;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.common.DomainUpdateFailedResponseBody;
import org.veo.adapter.presenter.api.dto.PageDto;
import org.veo.adapter.presenter.api.dto.ShortCatalogItemDto;
import org.veo.adapter.presenter.api.dto.ShortInspectionDto;
import org.veo.adapter.presenter.api.dto.ShortProfileDto;
import org.veo.adapter.presenter.api.dto.ShortProfileItemDto;
import org.veo.adapter.presenter.api.dto.UpdatableDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullProfileDto;
import org.veo.adapter.presenter.api.io.mapper.PagingMapper;
import org.veo.adapter.presenter.api.io.mapper.QueryInputMapper;
import org.veo.adapter.service.domaintemplate.dto.ExportDomainDto;
import org.veo.adapter.service.domaintemplate.dto.ExportProfileDto;
import org.veo.core.entity.BreakingChange;
import org.veo.core.entity.Domain;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.IncarnationConfiguration;
import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.inspection.Inspection;
import org.veo.core.entity.ref.TypedId;
import org.veo.core.entity.ref.TypedSymbolicId;
import org.veo.core.entity.riskdefinition.RiskDefinition;
import org.veo.core.entity.state.TemplateItemIncarnationDescriptionState;
import org.veo.core.entity.statistics.CatalogItemsTypeCount;
import org.veo.core.entity.statistics.ElementStatusCounts;
import org.veo.core.usecase.UpdateDomainUseCase;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCase.EntityId;
import org.veo.core.usecase.catalogitem.ApplyProfileIncarnationDescriptionUseCase;
import org.veo.core.usecase.catalogitem.GetCatalogItemUseCase;
import org.veo.core.usecase.catalogitem.GetProfileIncarnationDescriptionUseCase;
import org.veo.core.usecase.catalogitem.QueryCatalogItemsUseCase;
import org.veo.core.usecase.domain.EvaluateRiskDefinitionUseCase;
import org.veo.core.usecase.domain.ExportDomainUseCase;
import org.veo.core.usecase.domain.GetBreakingChangesUseCase;
import org.veo.core.usecase.domain.GetCatalogItemsTypeCountUseCase;
import org.veo.core.usecase.domain.GetDomainUpdatesUseCase;
import org.veo.core.usecase.domain.GetDomainUseCase;
import org.veo.core.usecase.domain.GetDomainsUseCase;
import org.veo.core.usecase.domain.GetElementStatusCountUseCase;
import org.veo.core.usecase.domain.GetInspectionUseCase;
import org.veo.core.usecase.domain.GetInspectionsUseCase;
import org.veo.core.usecase.profile.GetIncarnationConfigurationUseCase;
import org.veo.core.usecase.profile.GetProfileItemUseCase;
import org.veo.core.usecase.profile.GetProfileItemsUseCase;
import org.veo.core.usecase.profile.GetProfileUseCase;
import org.veo.core.usecase.profile.GetProfilesUseCase;
import org.veo.rest.annotations.UnitUuidParam;
import org.veo.rest.common.RestApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST service which provides methods to manage domains.
 *
 * <p>Uses async calls with {@code CompletableFuture} to parallelize long-running operations (i.e.
 * network calls to the database or to other HTTP services).
 *
 * @see <a href=
 *     "https://spring.io/guides/gs/async-method">https://spring.io/guides/gs/async-method/</a>
 */
@RestController
@RequestMapping(DomainController.URL_BASE_PATH)
@RequiredArgsConstructor
@Slf4j
public class DomainController extends AbstractEntityController {

  public static final String URL_BASE_PATH = "/" + Domain.PLURAL_TERM;

  private final GetDomainUseCase getDomainUseCase;
  private final GetDomainsUseCase getDomainsUseCase;
  private final GetDomainUpdatesUseCase getDomainUpdatesUseCase;
  private final ExportDomainUseCase exportDomainUseCase;
  private final UpdateDomainUseCase updateDomainUseCase;
  private final GetElementStatusCountUseCase getElementStatusCountUseCase;
  private final GetCatalogItemUseCase getCatalogItemUseCase;
  private final GetCatalogItemsTypeCountUseCase getCatalogItemsTypeCountUseCase;
  private final QueryCatalogItemsUseCase queryCatalogItemsUseCase;
  private final GetIncarnationConfigurationUseCase getIncarnationConfigurationUseCase;
  private final GetProfileItemsUseCase getProfileItemsUseCase;
  private final GetProfileItemUseCase getProfileItemUseCase;
  private final GetProfilesUseCase getProfilesUseCase;
  private final GetProfileUseCase getProfileUseCase;
  private final GetInspectionUseCase getInspectionUseCase;
  private final GetInspectionsUseCase getInspectionsUseCase;
  private final GetBreakingChangesUseCase getBreakingChangesUseCase;
  private final EvaluateRiskDefinitionUseCase evaluateRiskDefinitionUseCase;

  private final ApplyProfileIncarnationDescriptionUseCase applyProfileIncarnationDescriptionUseCase;
  private final GetProfileIncarnationDescriptionUseCase getProfileIncarnationDescriptionUseCase;

  @GetMapping
  @Operation(summary = "Loads all domains")
  @ApiResponse(
      responseCode = "200",
      description = "Domains loaded",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = FullDomainDto.class))))
  public @Valid Future<List<FullDomainDto>> getDomains() {
    return useCaseInteractor.execute(
        getDomainsUseCase,
        UseCase.EmptyInput.INSTANCE,
        output ->
            output.objects().stream().map(entityToDtoTransformer::transformDomain2Dto).toList());
  }

  @GetMapping(value = "/{id}")
  @Operation(summary = "Loads a domain")
  @ApiResponse(
      responseCode = "200",
      description = "Domain loaded",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = FullDomainDto.class)))
  @ApiResponse(responseCode = "404", description = "Domain not found")
  public @Valid Future<ResponseEntity<FullDomainDto>> getDomain(
      @PathVariable UUID id, WebRequest request) {
    if (getEtag(Domain.class, id).map(request::checkNotModified).orElse(false)) {
      return null;
    }
    CompletableFuture<FullDomainDto> domainFuture =
        useCaseInteractor.execute(
            getDomainUseCase,
            new EntityId(id),
            output -> entityToDtoTransformer.transformDomain2Dto(output.domain()));
    return domainFuture.thenApply(
        domainDto -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(domainDto));
  }

  @GetMapping(value = "/{id}/export")
  @Operation(summary = "Export a domain")
  @ApiResponse(
      responseCode = "200",
      description = "Domain exported",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ExportDomainDto.class)))
  @ApiResponse(responseCode = "404", description = "Domain not found")
  public CompletableFuture<ResponseEntity<ExportDomainDto>> exportDomain(@PathVariable UUID id) {
    return useCaseInteractor
        .execute(
            exportDomainUseCase,
            new EntityId(id),
            o -> entityToDtoTransformer.transformDomain2ExportDto(o.exportDomain()))
        .thenApply(
            domainDto -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(domainDto));
  }

  @GetMapping("/{domainId}/incarnation-configuration")
  @Operation(summary = "Loads the incarnation configuration for the domain")
  public @Valid Future<IncarnationConfiguration> getIncarnationConfiguration(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId) {
    return useCaseInteractor.execute(
        getIncarnationConfigurationUseCase,
        new EntityId(domainId),
        GetIncarnationConfigurationUseCase.OutputData::incarnationConfiguration);
  }

  @GetMapping("/{domainId}/profiles")
  @Operation(summary = "Loads all profiles in the domain")
  public @Valid Future<List<ShortProfileDto>> getProfiles(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId) {
    return useCaseInteractor.execute(
        getProfilesUseCase,
        new GetProfilesUseCase.InputData(TypedId.from(domainId, Domain.class)),
        out ->
            out.profiles().stream().map(entityToDtoTransformer::transformProfile2ListDto).toList());
  }

  @GetMapping("/{domainId}/profiles/{profileId}")
  @Operation(summary = "Loads a profile in the domain")
  public @Valid Future<ResponseEntity<FullProfileDto>> getProfile(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID profileId,
      WebRequest request) {
    if (getEtag(Profile.class, profileId).map(request::checkNotModified).orElse(false)) {
      return null;
    }

    return useCaseInteractor
        .execute(
            getProfileUseCase,
            new GetProfileUseCase.InputData(TypedId.from(profileId, Profile.class), false),
            t -> entityToDtoTransformer.transformProfile2Dto(t.profile()))
        .thenApply(
            profileDto -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(profileDto));
  }

  @GetMapping("/{domainId}/profiles/{profileId}/items")
  @Operation(summary = "Loads all profile items in the profile")
  public @Valid Future<List<ShortProfileItemDto>> getProfileItems(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID profileId) {
    return useCaseInteractor.execute(
        getProfileItemsUseCase,
        new GetProfileItemsUseCase.InputData(
            TypedId.from(domainId, Domain.class), TypedId.from(profileId, Profile.class)),
        out ->
            out.items().stream()
                .map(entityToDtoTransformer::transformShortProfileItem2Dto)
                .toList());
  }

  @GetMapping("/{domainId}/profiles/{profileId}/items/{itemId}")
  @Operation(summary = "Loads a profile item in the profile")
  public @Valid Future<ResponseEntity<ShortProfileItemDto>> getProfileItem(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID profileId,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID itemId) {
    return useCaseInteractor.execute(
        getProfileItemUseCase,
        new GetProfileItemUseCase.InputData(
            TypedId.from(domainId, Domain.class),
            TypedSymbolicId.from(
                itemId, ProfileItem.class, TypedId.from(profileId, Profile.class))),
        out ->
            ResponseEntity.ok()
                .cacheControl(defaultCacheControl)
                .body(entityToDtoTransformer.transformShortProfileItem2Dto(out.profileItems())));
  }

  @GetMapping("/{domainId}/profiles/{profileId}/export")
  @Operation(summary = "export a profile from a domain")
  public @Valid Future<ResponseEntity<ExportProfileDto>> exportProfile(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID profileId) {

    return useCaseInteractor
        .execute(
            getProfileUseCase,
            new GetProfileUseCase.InputData(TypedId.from(profileId, Profile.class), true),
            o -> entityToDtoTransformer.transformProfile2ExportDto(o.profile()))
        .thenApply(
            profileDto -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(profileDto));
  }

  @GetMapping("/{domainId}/catalog-items")
  @Operation(summary = "Loads catalog items in a domain")
  public @Valid Future<PageDto<ShortCatalogItemDto>> getCatalogItems(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @RequestParam(value = ELEMENT_TYPE_PARAM, required = false) ElementType elementType,
      @RequestParam(value = SUB_TYPE_PARAM, required = false) String subType,
      @RequestParam(value = ABBREVIATION_PARAM, required = false) String abbreviation,
      @RequestParam(value = NAME_PARAM, required = false) String name,
      @RequestParam(value = DESCRIPTION_PARAM, required = false) String description,
      @RequestParam(value = CUSTOM_ASPECTS_PARAM, required = false) List<String> customAspectKeys,
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
          String sortOrder) {
    return useCaseInteractor.execute(
        queryCatalogItemsUseCase,
        QueryInputMapper.map(
            domainId,
            elementType,
            subType,
            abbreviation,
            name,
            description,
            PagingMapper.toConfig(pageSize, pageNumber, sortColumn, sortOrder)),
        out ->
            PagingMapper.toPage(
                out.page(),
                i -> entityToDtoTransformer.transformShortCatalogItem2Dto(i, customAspectKeys)));
  }

  @GetMapping("/{domainId}/catalog-items/{itemId}")
  @Operation(summary = "Loads a catalog item in a domain")
  @ApiResponse(responseCode = "200", description = "Catalog item found")
  @ApiResponse(responseCode = "304", description = "Not modified")
  @ApiResponse(responseCode = "404", description = "Domain or catalog item not found")
  public @Valid Future<ResponseEntity<ShortCatalogItemDto>> getCatalogItem(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID itemId,
      WebRequest request) {
    return useCaseInteractor.execute(
        getCatalogItemUseCase,
        new GetCatalogItemUseCase.InputData(itemId, domainId),
        out ->
            RestApiResponse.okOrNotModified(
                out.catalogItem(), entityToDtoTransformer::transformShortCatalogItem2Dto, request));
  }

  @GetMapping(value = "/{id}/element-status-count")
  @Operation(summary = "Retrieve element counts grouped by subType and status")
  @ApiResponse(
      responseCode = "200",
      description = "Elements counted",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ElementStatusCounts.class)))
  @ApiResponse(responseCode = "404", description = "Domain not found")
  public @Valid CompletableFuture<ResponseEntity<ElementStatusCounts>> getElementStatusCount(
      @PathVariable UUID id, @UnitUuidParam @RequestParam(value = UNIT_PARAM) String unitId) {

    return useCaseInteractor
        .execute(
            getElementStatusCountUseCase,
            new GetElementStatusCountUseCase.InputData(UUID.fromString(unitId), id),
            GetElementStatusCountUseCase.OutputData::result)
        .thenApply(counts -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(counts));
  }

  @GetMapping(value = "/{domainId}/inspections")
  @Operation(summary = "Retrieve inspections")
  @ApiResponse(responseCode = "200", description = "Inspections found")
  @ApiResponse(responseCode = "404", description = "Domain not found")
  public @Valid Future<ResponseEntity<List<ShortInspectionDto>>> getInspections(
      @PathVariable UUID domainId) {
    return useCaseInteractor
        .execute(
            getInspectionsUseCase,
            new GetInspectionsUseCase.InputData(domainId),
            out ->
                entityToDtoTransformer.transformInspections2ShortDtos(
                    out.inspections(), out.domain()))
        .thenApply(
            inspections -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(inspections));
  }

  @GetMapping(value = "/{domainId}/inspections/{inspectionId}")
  @Operation(summary = "Retrieve inspection")
  @ApiResponse(responseCode = "200", description = "Inspection found")
  @ApiResponse(responseCode = "404", description = "Domain or inspection not found")
  public @Valid Future<ResponseEntity<Inspection>> getInspection(
      @PathVariable UUID domainId,
      @PathVariable @Size(min = 1, max = INSPECTION_ID_MAX_LENGTH) String inspectionId) {
    return useCaseInteractor
        .execute(
            getInspectionUseCase,
            new GetInspectionUseCase.InputData(domainId, inspectionId),
            GetInspectionUseCase.OutputData::inspection)
        .thenApply(
            inspection -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(inspection));
  }

  @GetMapping(value = "/{id}/catalog-items/type-count")
  @Operation(summary = "Retrieve catalog item counts grouped by sub type.")
  @ApiResponse(
      responseCode = "200",
      description = "catalog items counted",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = CatalogItemsTypeCount.class)))
  @ApiResponse(responseCode = "404", description = "Domain not found")
  public @Valid CompletableFuture<ResponseEntity<CatalogItemsTypeCount>> getCatalogItemsTypeCount(
      @PathVariable UUID id) {
    return useCaseInteractor
        .execute(
            getCatalogItemsTypeCountUseCase,
            new EntityId(id),
            GetCatalogItemsTypeCountUseCase.OutputData::result)
        .thenApply(counts -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(counts));
  }

  @PostMapping("/{id}/profiles/{profileId}/incarnation")
  @Operation(summary = "Incarnates all profile items in the unit.")
  @ApiResponse(responseCode = "204", description = "Profile applied")
  @ApiResponse(responseCode = "404", description = "Domain or unit not found")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> applyProfile(
      @PathVariable UUID id,
      @PathVariable UUID profileId,
      @RequestParam(name = UNIT_PARAM) UUID unitId) {
    return useCaseInteractor
        .execute(
            getProfileIncarnationDescriptionUseCase,
            new GetProfileIncarnationDescriptionUseCase.InputData(
                unitId, id, null, profileId, true),
            out ->
                out.references().stream()
                    .map(d -> (TemplateItemIncarnationDescriptionState<ProfileItem, Profile>) d)
                    .toList())
        .thenCompose(
            references ->
                useCaseInteractor.execute(
                    applyProfileIncarnationDescriptionUseCase,
                    new ApplyProfileIncarnationDescriptionUseCase.InputData(unitId, references),
                    out -> ResponseEntity.noContent().build()));
  }

  @PostMapping("/{domainId}/update")
  @Operation(
      summary =
          "Update the domain to a newer template, migrating all associated units in the client.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Client updated to new domain version"),
    @ApiResponse(responseCode = "404", description = "Domain or domain template not found"),
    @ApiResponse(
        responseCode = "409",
        description = "Update failed due to conflicted elements",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = DomainUpdateFailedResponseBody.class)))
  })
  public CompletableFuture<ResponseEntity<ApiResponseBody>> updateDomain(
      @PathVariable UUID domainId, @RequestParam(name = "template") UUID domainTemplateId) {
    return useCaseInteractor.execute(
        updateDomainUseCase,
        new UpdateDomainUseCase.InputData(domainId, domainTemplateId),
        out ->
            RestApiResponse.created(
                referenceAssembler.targetReferenceOf(out.newDomain()),
                out.newDomain().getId(),
                "Domain updated"));
  }

  @GetMapping(value = "/{domainId}/breaking-changes")
  @Operation(summary = "Retrieve breaking changes wrt. domain template")
  @ApiResponse(responseCode = "200", description = "Breaking changes computed")
  @ApiResponse(responseCode = "404", description = "Domain not found or has no domain template")
  public @Valid Future<ResponseEntity<List<BreakingChange>>> getBreakingChanges(
      @PathVariable UUID domainId, WebRequest request) {
    if (getEtag(Domain.class, domainId).map(request::checkNotModified).orElse(false)) {
      return null;
    }
    return useCaseInteractor
        .execute(
            getBreakingChangesUseCase,
            new EntityId(domainId),
            GetBreakingChangesUseCase.OutputData::breakingChanges)
        .thenApply(
            breakingChanges ->
                ResponseEntity.ok().cacheControl(defaultCacheControl).body(breakingChanges));
  }

  @GetMapping("/{domainId}/risk-definitions/{riskDefinitionId}")
  @Operation(summary = "Loads a risk definition from a domain.")
  @ApiResponse(responseCode = "200", description = "Risk definition evaluated")
  public CompletableFuture<ResponseEntity<RiskDefinition>> getRiskDefinition(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Parameter(
              required = true,
              example = "DSRA",
              description = "Risk definition identifier - unique within this domain")
          @PathVariable
          String riskDefinitionId) {

    return useCaseInteractor.execute(
        evaluateRiskDefinitionUseCase,
        new EvaluateRiskDefinitionUseCase.InputData(
            domainId, riskDefinitionId, null, Collections.emptySet()),
        out -> ResponseEntity.ok(out.riskDefinition()));
  }

  @GetMapping(value = "/updates")
  @Operation(summary = "Loads available updates for all domains")
  @ApiResponse(
      responseCode = "200",
      description = "Domain updates loaded",
      useReturnTypeSchema = true)
  public @Valid Future<List<UpdatableDomainDto>> getDomainUpdates() {
    return useCaseInteractor.execute(
        getDomainUpdatesUseCase,
        UseCase.EmptyInput.INSTANCE,
        outputData ->
            outputData.updatableDomains().stream()
                .map(u -> UpdatableDomainDto.from(u, referenceAssembler))
                .toList());
  }
}
