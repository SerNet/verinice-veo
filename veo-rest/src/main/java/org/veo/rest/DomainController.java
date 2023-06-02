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
import static org.veo.rest.ControllerConstants.UNIT_PARAM;
import static org.veo.rest.ControllerConstants.UUID_REGEX;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
import org.veo.adapter.presenter.api.dto.SearchQueryDto;
import org.veo.adapter.presenter.api.dto.full.FullDomainDto;
import org.veo.core.ExportDto;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.Key;
import org.veo.core.entity.statistics.ElementStatusCounts;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.domain.ApplyProfileUseCase;
import org.veo.core.usecase.domain.ExportDomainUseCase;
import org.veo.core.usecase.domain.GetDomainUseCase;
import org.veo.core.usecase.domain.GetDomainsUseCase;
import org.veo.core.usecase.domain.GetElementStatusCountUseCase;
import org.veo.persistence.entity.jpa.ProfileReferenceFactoryImpl;
import org.veo.rest.annotations.UnitUuidParam;
import org.veo.rest.security.ApplicationUser;

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
  private final ApplyProfileUseCase applyProfileUseCase;

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

  @PostMapping("/{id}/profiles/{profileKey}/units/{unitId}")
  @Operation(summary = "Apply a profile to a unit. Adds all profile elements & risks to the unit.")
  @ApiResponse(responseCode = "204", description = "Profile applied")
  @ApiResponse(responseCode = "404", description = "Domain not found")
  @ApiResponse(responseCode = "404", description = "Unit not found")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> applyProfile(
      @Parameter(required = true, hidden = true) Authentication auth,
      @PathVariable @Pattern(regexp = UUID_REGEX) String id,
      @PathVariable String profileKey,
      @PathVariable @Pattern(regexp = UUID_REGEX) String unitId) {
    return useCaseInteractor.execute(
        applyProfileUseCase,
        new ApplyProfileUseCase.InputData(
            getAuthenticatedClient(auth).getId(),
            Key.uuidFrom(id),
            ProfileReferenceFactoryImpl.getInstance().createProfileRef(profileKey),
            Key.uuidFrom(unitId)),
        out -> ResponseEntity.noContent().build());
  }

  @InitBinder
  public void initBinder(WebDataBinder dataBinder) {
    dataBinder.registerCustomEditor(
        EntityType.class, new IgnoreCaseEnumConverter<>(EntityType.class));
  }

  protected Client getClientWithCatalogs(Authentication auth) {
    ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
    Key<UUID> id = Key.uuidFrom(user.getClientId());
    Optional<Client> client = clientRepository.findByIdFetchCatalogs(id);
    return client.orElseThrow();
  }
}
