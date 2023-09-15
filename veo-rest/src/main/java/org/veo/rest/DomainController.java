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

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;
import static org.veo.rest.ControllerConstants.ANY_AUTH;
import static org.veo.rest.ControllerConstants.ELEMENT_TYPE_PARAM;
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
import static org.veo.rest.ControllerConstants.UUID_REGEX;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

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

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.dto.PageDto;
import org.veo.adapter.presenter.api.dto.SearchQueryDto;
import org.veo.adapter.presenter.api.dto.ShortCatalogItemDto;
import org.veo.adapter.presenter.api.dto.ShortProfileDto;
import org.veo.adapter.presenter.api.dto.ShortProfileItemDto;
import org.veo.adapter.presenter.api.dto.full.FullDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullProfileDto;
import org.veo.adapter.presenter.api.io.mapper.PagingMapper;
import org.veo.adapter.presenter.api.io.mapper.QueryInputMapper;
import org.veo.core.ExportDto;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.Key;
import org.veo.core.entity.Profile;
import org.veo.core.entity.ProfileItem;
import org.veo.core.entity.statistics.CatalogItemsTypeCount;
import org.veo.core.entity.statistics.ElementStatusCounts;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.catalogitem.ApplyProfileIncarnationDescriptionUseCase;
import org.veo.core.usecase.catalogitem.GetProfileIncarnationDescriptionUseCase;
import org.veo.core.usecase.catalogitem.QueryCatalogItemsUseCase;
import org.veo.core.usecase.domain.ApplyJsonProfileUseCase;
import org.veo.core.usecase.domain.ExportDomainUseCase;
import org.veo.core.usecase.domain.GetCatalogItemsTypeCountUseCase;
import org.veo.core.usecase.domain.GetDomainUseCase;
import org.veo.core.usecase.domain.GetDomainsUseCase;
import org.veo.core.usecase.domain.GetElementStatusCountUseCase;
import org.veo.core.usecase.profile.GetProfileItemUseCase;
import org.veo.core.usecase.profile.GetProfileItemsUseCase;
import org.veo.core.usecase.profile.GetProfileUseCase;
import org.veo.core.usecase.profile.GetProfilesUseCase;
import org.veo.core.usecase.service.TypedId;
import org.veo.persistence.entity.jpa.ProfileReferenceFactoryImpl;
import org.veo.rest.annotations.UnitUuidParam;

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
  private final GetCatalogItemsTypeCountUseCase getCatalogItemsTypeCountUseCase;
  private final ApplyJsonProfileUseCase applyJsonProfileUseCase;
  private final QueryCatalogItemsUseCase queryCatalogItemsUseCase;
  private final GetProfileItemsUseCase getProfileItemsUseCase;
  private final GetProfileItemUseCase getProfileItemUseCase;
  private final GetProfilesUseCase getProfilesUseCase;
  private final GetProfileUseCase getProfileUseCase;

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
            output.getObjects().stream()
                .map(u -> entityToDtoTransformer.transformDomain2Dto(u))
                .toList());
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
      @Parameter(hidden = true) Authentication auth, @PathVariable String id, WebRequest request) {
    Client client = getAuthenticatedClient(auth);
    if (getEtag(Domain.class, id).map(request::checkNotModified).orElse(false)) {
      return null;
    }
    CompletableFuture<FullDomainDto> domainFuture =
        useCaseInteractor.execute(
            getDomainUseCase,
            new UseCase.IdAndClient(Key.uuidFrom(id), client),
            output -> entityToDtoTransformer.transformDomain2Dto(output.getDomain()));
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
  public @Valid CompletableFuture<ResponseEntity<ExportDto>> exportDomain(
      @Parameter(hidden = true) Authentication auth, @PathVariable String id, WebRequest request) {
    Client client = getAuthenticatedClient(auth);
    CompletableFuture<ExportDto> domainFuture =
        useCaseInteractor.execute(
            exportDomainUseCase,
            new UseCase.IdAndClient(Key.uuidFrom(id), client),
            ExportDomainUseCase.OutputData::getExportDomain);
    return domainFuture.thenApply(
        domainDto -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(domainDto));
  }

  @GetMapping("/{domainId}/profiles")
  @Operation(summary = "Loads all profiles in the domain")
  public @Valid Future<List<ShortProfileDto>> getProfiles(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String domainId) {
    return useCaseInteractor.execute(
        getProfilesUseCase,
        new GetProfilesUseCase.InputData(
            getAuthenticatedClient(auth), TypedId.from(domainId, Domain.class)),
        out ->
            out.getProfiles().stream()
                .map(entityToDtoTransformer::transformProfile2ListDto)
                .toList());
  }

  @GetMapping("/{domainId}/profiles/{profileId}")
  @Operation(summary = "Loads a profile in the domain")
  public @Valid Future<ResponseEntity<FullProfileDto>> getProfile(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String domainId,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String profileId,
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
                TypedId.from(profileId, Profile.class)),
            t -> entityToDtoTransformer.transformProfile2Dto(t.getProfile()))
        .thenApply(
            profileDto -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(profileDto));
  }

  @GetMapping("/{domainId}/profiles/{profileId}/items")
  @Operation(summary = "Loads all profile items in the profile")
  public @Valid Future<List<ShortProfileItemDto>> getProfileItems(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String domainId,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String profileId,
      WebRequest request) {
    return useCaseInteractor.execute(
        getProfileItemsUseCase,
        new GetProfileItemsUseCase.InputData(
            getAuthenticatedClient(auth),
            TypedId.from(domainId, Domain.class),
            TypedId.from(profileId, Profile.class)),
        out ->
            out.getItems().stream()
                .map(v -> entityToDtoTransformer.transformShortProfileItem2Dto(v))
                .toList());
  }

  @GetMapping("/{domainId}/profiles/{profileId}/items/{itemId}")
  @Operation(summary = "Loads a profile item in the profile")
  public @Valid Future<ResponseEntity<ShortProfileItemDto>> getProfileItem(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String domainId,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String profileId,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String itemId,
      WebRequest request) {
    return useCaseInteractor.execute(
        getProfileItemUseCase,
        new GetProfileItemUseCase.InputData(
            getAuthenticatedClient(auth),
            TypedId.from(domainId, Domain.class),
            TypedId.from(profileId, Profile.class),
            TypedId.from(itemId, ProfileItem.class)),
        out ->
            ResponseEntity.ok()
                .cacheControl(defaultCacheControl)
                .body(entityToDtoTransformer.transformShortProfileItem2Dto(out.getProfileItems())));
  }

  @GetMapping("/{domainId}/catalog-items")
  @Operation(summary = "Loads catalog items in a domain")
  public @Valid Future<PageDto<ShortCatalogItemDto>> getCatalogItems(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String domainId,
      @RequestParam(value = ELEMENT_TYPE_PARAM, required = false) String elementType,
      @RequestParam(value = SUB_TYPE_PARAM, required = false) String subType,
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
          String sortOrder) {
    return useCaseInteractor.execute(
        queryCatalogItemsUseCase,
        QueryInputMapper.map(
            getAuthenticatedClient(auth),
            domainId,
            elementType,
            subType,
            PagingMapper.toConfig(pageSize, pageNumber, sortColumn, sortOrder)),
        out ->
            PagingMapper.toPage(
                out.getPage(), entityToDtoTransformer::transformShortCatalogItem2Dto));
  }

  @Override
  @SuppressFBWarnings // ignore warning on call to method proxy factory
  protected String buildSearchUri(String id) {
    return linkTo(methodOn(DomainController.class).runSearch(ANY_AUTH, id)).withSelfRel().getHref();
  }

  @GetMapping(value = "/searches/{searchId}")
  @Operation(summary = "Finds domains for the search.")
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
      @PathVariable String id,
      @UnitUuidParam @RequestParam(value = UNIT_PARAM) String unitId,
      WebRequest request) {
    Client client = getAuthenticatedClient(auth);

    return useCaseInteractor
        .execute(
            getElementStatusCountUseCase,
            new GetElementStatusCountUseCase.InputData(
                Key.uuidFrom(unitId), Key.uuidFrom(id), client),
            GetElementStatusCountUseCase.OutputData::getResult)
        .thenApply(counts -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(counts));
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
      @Parameter(hidden = true) Authentication auth, @PathVariable String id, WebRequest request) {
    Client client = getAuthenticatedClient(auth);
    return useCaseInteractor
        .execute(
            getCatalogItemsTypeCountUseCase,
            new GetCatalogItemsTypeCountUseCase.InputData(Key.uuidFrom(id), client),
            GetCatalogItemsTypeCountUseCase.OutputData::getResult)
        .thenApply(counts -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(counts));
  }

  @PostMapping("/{id}/profiles/{profileKey}/units/{unitId}")
  @Operation(summary = "Apply a profile to a unit. Adds all profile elements & risks to the unit.")
  @ApiResponse(responseCode = "204", description = "Profile applied")
  @ApiResponse(responseCode = "404", description = "Domain not found")
  @ApiResponse(responseCode = "404", description = "Unit not found")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> applyJsonProfile(
      @Parameter(required = true, hidden = true) Authentication auth,
      @PathVariable @Pattern(regexp = UUID_REGEX) String id,
      @PathVariable String profileKey,
      @PathVariable @Pattern(regexp = UUID_REGEX) String unitId) {
    return useCaseInteractor.execute(
        applyJsonProfileUseCase,
        new ApplyJsonProfileUseCase.InputData(
            getAuthenticatedClient(auth).getId(),
            Key.uuidFrom(id),
            ProfileReferenceFactoryImpl.getInstance().createProfileRef(profileKey),
            Key.uuidFrom(unitId)),
        out -> ResponseEntity.noContent().build());
  }

  @PostMapping("/{id}/profilesnew/{profileKey}/units/{unitId}")
  @Operation(summary = "Apply a profile to a unit. Adds all profile elements & risks to the unit.")
  @ApiResponse(responseCode = "204", description = "Profile applied")
  @ApiResponse(responseCode = "404", description = "Domain not found")
  @ApiResponse(responseCode = "404", description = "Unit not found")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> applyProfile(
      @Parameter(required = true, hidden = true) Authentication auth,
      @PathVariable @Pattern(regexp = UUID_REGEX) String id,
      @PathVariable @Pattern(regexp = UUID_REGEX) String profileKey,
      @PathVariable @Pattern(regexp = UUID_REGEX) String unitId) {
    return useCaseInteractor
        .execute(
            getProfileIncarnationDescriptionUseCase,
            new GetProfileIncarnationDescriptionUseCase.InputData(
                getAuthenticatedClient(auth), Key.uuidFrom(unitId), null, Key.uuidFrom(profileKey)),
            GetProfileIncarnationDescriptionUseCase.OutputData::getReferences)
        .thenCompose(
            references ->
                useCaseInteractor.execute(
                    applyProfileIncarnationDescriptionUseCase,
                    new ApplyProfileIncarnationDescriptionUseCase.InputData(
                        getAuthenticatedClient(auth), Key.uuidFrom(unitId), references),
                    out -> ResponseEntity.noContent().build()));
  }

  @InitBinder
  public void initBinder(WebDataBinder dataBinder) {
    dataBinder.registerCustomEditor(
        EntityType.class, new IgnoreCaseEnumConverter<>(EntityType.class));
  }
}
