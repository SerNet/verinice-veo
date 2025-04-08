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

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static org.veo.core.entity.DomainBase.INSPECTION_ID_MAX_LENGTH;
import static org.veo.rest.ControllerConstants.ABBREVIATION_PARAM;
import static org.veo.rest.ControllerConstants.ANY_AUTH;
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

import java.io.IOException;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.dto.PageDto;
import org.veo.adapter.presenter.api.dto.SearchQueryDto;
import org.veo.adapter.presenter.api.dto.ShortCatalogItemDto;
import org.veo.adapter.presenter.api.dto.ShortInspectionDto;
import org.veo.adapter.presenter.api.dto.ShortProfileDto;
import org.veo.adapter.presenter.api.dto.ShortProfileItemDto;
import org.veo.adapter.presenter.api.dto.full.FullDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullProfileDto;
import org.veo.adapter.presenter.api.io.mapper.PagingMapper;
import org.veo.adapter.presenter.api.io.mapper.QueryInputMapper;
import org.veo.adapter.service.domaintemplate.dto.ExportDomainDto;
import org.veo.adapter.service.domaintemplate.dto.ExportProfileDto;
import org.veo.core.entity.BreakingChange;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.IncarnationConfiguration;
import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.inspection.Inspection;
import org.veo.core.entity.ref.TypedId;
import org.veo.core.entity.ref.TypedSymbolicId;
import org.veo.core.entity.state.TemplateItemIncarnationDescriptionState;
import org.veo.core.entity.statistics.CatalogItemsTypeCount;
import org.veo.core.entity.statistics.ElementStatusCounts;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.catalogitem.ApplyProfileIncarnationDescriptionUseCase;
import org.veo.core.usecase.catalogitem.GetCatalogItemUseCase;
import org.veo.core.usecase.catalogitem.GetProfileIncarnationDescriptionUseCase;
import org.veo.core.usecase.catalogitem.QueryCatalogItemsUseCase;
import org.veo.core.usecase.domain.ExportDomainUseCase;
import org.veo.core.usecase.domain.GetBreakingChangesUseCase;
import org.veo.core.usecase.domain.GetCatalogItemsTypeCountUseCase;
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST service which provides methods to manage domains.
 *
 * <p>Uses async calls with {@code CompletableFuture} to parallelize long running operations (i.e.
 * network calls to the database or to other HTTP services).
 *
 * @see <a href=
 *     "https://spring.io/guides/gs/async-method">https://spring.io/guides/gs/async-method/</a>
 */
@RestController
@RequestMapping(DomainController.URL_BASE_PATH)
@RequiredArgsConstructor
@Slf4j
public class DomainController extends AbstractEntityControllerWithDefaultSearch {

  public static final String URL_BASE_PATH = "/" + Domain.PLURAL_TERM;

  private final GetDomainUseCase getDomainUseCase;
  private final GetDomainsUseCase getDomainsUseCase;
  private final ExportDomainUseCase exportDomainUseCase;
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
  public @Valid Future<List<FullDomainDto>> getDomains(
      @Parameter(hidden = true) Authentication auth) {

    Client client = getAuthenticatedClient(auth);

    final GetDomainsUseCase.InputData inputData = new GetDomainsUseCase.InputData(client);
    return useCaseInteractor.execute(
        getDomainsUseCase,
        inputData,
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
      @Parameter(hidden = true) Authentication auth, @PathVariable UUID id, WebRequest request) {
    Client client = getAuthenticatedClient(auth);
    if (getEtag(Domain.class, id).map(request::checkNotModified).orElse(false)) {
      return null;
    }
    CompletableFuture<FullDomainDto> domainFuture =
        useCaseInteractor.execute(
            getDomainUseCase,
            new UseCase.IdAndClient(id, client),
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
              schema = @Schema(implementation = FullDomainDto.class)))
  @ApiResponse(responseCode = "404", description = "Domain not found")
  public CompletableFuture<ResponseEntity<ExportDomainDto>> exportDomain(
      @Parameter(hidden = true) Authentication auth, @PathVariable UUID id, WebRequest request) {
    Client client = getAuthenticatedClient(auth);
    return useCaseInteractor
        .execute(
            exportDomainUseCase,
            new UseCase.IdAndClient(id, client),
            o -> entityToDtoTransformer.transformDomain2ExportDto(o.exportDomain()))
        .thenApply(
            domainDto -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(domainDto));
  }

  @GetMapping("/{domainId}/incarnation-configuration")
  @Operation(summary = "Loads the incarnation configuration for the domain")
  public @Valid Future<IncarnationConfiguration> getIncarnationConfiguration(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId) {
    return useCaseInteractor.execute(
        getIncarnationConfigurationUseCase,
        new GetIncarnationConfigurationUseCase.InputData(getAuthenticatedClient(auth), domainId),
        GetIncarnationConfigurationUseCase.OutputData::incarnationConfiguration);
  }

  @GetMapping("/{domainId}/profiles")
  @Operation(summary = "Loads all profiles in the domain")
  public @Valid Future<List<ShortProfileDto>> getProfiles(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId) {
    return useCaseInteractor.execute(
        getProfilesUseCase,
        new GetProfilesUseCase.InputData(
            getAuthenticatedClient(auth), TypedId.from(domainId, Domain.class)),
        out ->
            out.profiles().stream().map(entityToDtoTransformer::transformProfile2ListDto).toList());
  }

  @GetMapping("/{domainId}/profiles/{profileId}")
  @Operation(summary = "Loads a profile in the domain")
  public @Valid Future<ResponseEntity<FullProfileDto>> getProfile(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
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
            new GetProfileUseCase.InputData(
                getAuthenticatedClient(auth),
                TypedId.from(domainId, Domain.class),
                TypedId.from(profileId, Profile.class),
                false),
            t -> entityToDtoTransformer.transformProfile2Dto(t.profile()))
        .thenApply(
            profileDto -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(profileDto));
  }

  @GetMapping("/{domainId}/profiles/{profileId}/items")
  @Operation(summary = "Loads all profile items in the profile")
  public @Valid Future<List<ShortProfileItemDto>> getProfileItems(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID profileId,
      WebRequest request) {
    return useCaseInteractor.execute(
        getProfileItemsUseCase,
        new GetProfileItemsUseCase.InputData(
            getAuthenticatedClient(auth),
            TypedId.from(domainId, Domain.class),
            TypedId.from(profileId, Profile.class)),
        out ->
            out.items().stream()
                .map(entityToDtoTransformer::transformShortProfileItem2Dto)
                .toList());
  }

  @GetMapping("/{domainId}/profiles/{profileId}/items/{itemId}")
  @Operation(summary = "Loads a profile item in the profile")
  public @Valid Future<ResponseEntity<ShortProfileItemDto>> getProfileItem(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID profileId,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID itemId,
      WebRequest request) {
    return useCaseInteractor.execute(
        getProfileItemUseCase,
        new GetProfileItemUseCase.InputData(
            getAuthenticatedClient(auth),
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
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID profileId,
      WebRequest request) {

    return useCaseInteractor
        .execute(
            getProfileUseCase,
            new GetProfileUseCase.InputData(
                getAuthenticatedClient(auth),
                TypedId.from(domainId, Domain.class),
                TypedId.from(profileId, Profile.class),
                true),
            o -> entityToDtoTransformer.transformProfile2ExportDto(o.profile()))
        .thenApply(
            profileDto -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(profileDto));
  }

  @GetMapping("/{domainId}/catalog-items")
  @Operation(summary = "Loads catalog items in a domain")
  public @Valid Future<PageDto<ShortCatalogItemDto>> getCatalogItems(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @RequestParam(value = ELEMENT_TYPE_PARAM, required = false) String elementType,
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
            getAuthenticatedClient(auth),
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
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID itemId,
      WebRequest request) {
    return useCaseInteractor.execute(
        getCatalogItemUseCase,
        new GetCatalogItemUseCase.InputData(itemId, domainId, getAuthenticatedClient(auth)),
        out ->
            RestApiResponse.okOrNotModified(
                out.catalogItem(), entityToDtoTransformer::transformShortCatalogItem2Dto, request));
  }

  @Override
  @SuppressFBWarnings // ignore warning on call to method proxy factory
  @Deprecated
  protected String buildSearchUri(String id) {
    return MvcUriComponentsBuilder.fromMethodCall(
            UriComponentsBuilder.fromPath("/"), on(DomainController.class).runSearch(ANY_AUTH, id))
        .toUriString();
  }

  @GetMapping(value = "/searches/{searchId}")
  @Operation(summary = "Finds domains for the search.", deprecated = true)
  @Deprecated
  public @Valid Future<List<FullDomainDto>> runSearch(
      @Parameter(hidden = true) Authentication auth, @PathVariable String searchId) {
    // TODO: VEO-498 Implement Domain Search
    try {
      SearchQueryDto.decodeFromSearchId(searchId);
      return getDomains(auth);
    } catch (IOException e) {
      log.error("Could not decode search URL: {}", e.getLocalizedMessage());
      throw new IllegalArgumentException("Could not decode search URL.");
    }
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
      @Parameter(hidden = true) Authentication auth,
      @PathVariable UUID id,
      @UnitUuidParam @RequestParam(value = UNIT_PARAM) String unitId,
      WebRequest request) {
    Client client = getAuthenticatedClient(auth);

    return useCaseInteractor
        .execute(
            getElementStatusCountUseCase,
            new GetElementStatusCountUseCase.InputData(UUID.fromString(unitId), id, client),
            GetElementStatusCountUseCase.OutputData::result)
        .thenApply(counts -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(counts));
  }

  @GetMapping(value = "/{domainId}/inspections")
  @Operation(summary = "Retrieve inspections")
  @ApiResponse(responseCode = "200", description = "Inspections found")
  @ApiResponse(responseCode = "404", description = "Domain not found")
  public @Valid Future<ResponseEntity<List<ShortInspectionDto>>> getInspections(
      @Parameter(hidden = true) Authentication auth, @PathVariable UUID domainId) {
    return useCaseInteractor
        .execute(
            getInspectionsUseCase,
            new GetInspectionsUseCase.InputData(getAuthenticatedClient(auth).getId(), domainId),
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
      @Parameter(hidden = true) Authentication auth,
      @PathVariable UUID domainId,
      @PathVariable @Size(min = 1, max = INSPECTION_ID_MAX_LENGTH) String inspectionId) {
    return useCaseInteractor
        .execute(
            getInspectionUseCase,
            new GetInspectionUseCase.InputData(
                getAuthenticatedClient(auth).getId(), domainId, inspectionId),
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
      @Parameter(hidden = true) Authentication auth, @PathVariable UUID id, WebRequest request) {
    Client client = getAuthenticatedClient(auth);
    return useCaseInteractor
        .execute(
            getCatalogItemsTypeCountUseCase,
            new GetCatalogItemsTypeCountUseCase.InputData(id, client),
            GetCatalogItemsTypeCountUseCase.OutputData::result)
        .thenApply(counts -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(counts));
  }

  @PostMapping("/{id}/profiles/{profileId}/incarnation")
  @Operation(summary = "Incarnates all profile items in the unit.")
  @ApiResponse(responseCode = "204", description = "Profile applied")
  @ApiResponse(responseCode = "404", description = "Domain or unit not found")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> applyProfile(
      @Parameter(required = true, hidden = true) Authentication auth,
      @PathVariable UUID id,
      @PathVariable UUID profileId,
      @RequestParam(name = UNIT_PARAM) UUID unitId) {
    return useCaseInteractor
        .execute(
            getProfileIncarnationDescriptionUseCase,
            new GetProfileIncarnationDescriptionUseCase.InputData(
                getAuthenticatedClient(auth), unitId, id, null, profileId, true),
            out ->
                out.references().stream()
                    .map(d -> (TemplateItemIncarnationDescriptionState<ProfileItem, Profile>) d)
                    .toList())
        .thenCompose(
            references ->
                useCaseInteractor.execute(
                    applyProfileIncarnationDescriptionUseCase,
                    new ApplyProfileIncarnationDescriptionUseCase.InputData(
                        getAuthenticatedClient(auth), unitId, references),
                    out -> ResponseEntity.noContent().build()));
  }

  @GetMapping(value = "/{domainId}/breaking-changes")
  @Operation(summary = "Retrieve breaking changes wrt. domain template")
  @ApiResponse(responseCode = "200", description = "Breaking changes computed")
  @ApiResponse(responseCode = "404", description = "Domain not found or has no domain template")
  public @Valid Future<ResponseEntity<List<BreakingChange>>> getBreakingChanges(
      @Parameter(hidden = true) Authentication auth,
      @PathVariable UUID domainId,
      WebRequest request) {
    if (getEtag(Domain.class, domainId).map(request::checkNotModified).orElse(false)) {
      return null;
    }
    return useCaseInteractor
        .execute(
            getBreakingChangesUseCase,
            new GetBreakingChangesUseCase.InputData(getAuthenticatedClient(auth).getId(), domainId),
            GetBreakingChangesUseCase.OutputData::breakingChanges)
        .thenApply(
            breakingChanges ->
                ResponseEntity.ok().cacheControl(defaultCacheControl).body(breakingChanges));
  }

  @InitBinder
  public void initBinder(WebDataBinder dataBinder) {
    dataBinder.registerCustomEditor(
        EntityType.class, new IgnoreCaseEnumConverter<>(EntityType.class));
  }
}
