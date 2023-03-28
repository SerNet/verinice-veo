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
import static org.veo.adapter.presenter.api.io.mapper.VersionMapper.parseVersion;
import static org.veo.rest.ControllerConstants.ANY_AUTH;
import static org.veo.rest.ControllerConstants.UNIT_PARAM;
import static org.veo.rest.ControllerConstants.UUID_REGEX;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import org.veo.adapter.presenter.api.Patterns;
import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.dto.AbstractElementDto;
import org.veo.adapter.presenter.api.dto.AbstractRiskDto;
import org.veo.adapter.presenter.api.dto.ElementTypeDefinitionDto;
import org.veo.adapter.presenter.api.dto.SearchQueryDto;
import org.veo.adapter.presenter.api.dto.UnitDumpDto;
import org.veo.adapter.presenter.api.dto.create.CreateDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullDomainDto;
import org.veo.adapter.presenter.api.io.mapper.CreateOutputMapper;
import org.veo.adapter.presenter.api.io.mapper.UnitDumpMapper;
import org.veo.adapter.service.ObjectSchemaParser;
import org.veo.adapter.service.domaintemplate.dto.CreateDomainTemplateFromDomainParameterDto;
import org.veo.core.ExportDto;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.Key;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.profile.ProfileDefinition;
import org.veo.core.entity.statistics.ElementStatusCounts;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.domain.ApplyProfileUseCase;
import org.veo.core.usecase.domain.CreateDomainUseCase;
import org.veo.core.usecase.domain.ExportDomainUseCase;
import org.veo.core.usecase.domain.GetDomainUseCase;
import org.veo.core.usecase.domain.GetDomainsUseCase;
import org.veo.core.usecase.domain.GetElementStatusCountUseCase;
import org.veo.core.usecase.domain.UpdateElementTypeDefinitionUseCase;
import org.veo.core.usecase.domaintemplate.CreateDomainTemplateFromDomainUseCase;
import org.veo.core.usecase.unit.GetUnitDumpUseCase;
import org.veo.persistence.entity.jpa.ProfileReferenceFactoryImpl;
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

  private final ObjectSchemaParser objectSchemaParser;
  private final GetDomainUseCase getDomainUseCase;
  private final GetDomainsUseCase getDomainsUseCase;
  private final ExportDomainUseCase exportDomainUseCase;
  private final UpdateElementTypeDefinitionUseCase updateElementTypeDefinitionUseCase;
  private final CreateDomainUseCase createDomainUseCase;
  private final CreateDomainTemplateFromDomainUseCase createDomainTemplateFromDomainUseCase;
  private final GetElementStatusCountUseCase getElementStatusCountUseCase;

  private final GetUnitDumpUseCase getUnitDumpUseCase;

  private final ApplyProfileUseCase applyProfileUseCase;

  @GetMapping
  @Operation(summary = "Loads all domains")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Domains loaded",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(schema = @Schema(implementation = FullDomainDto.class))))
  })
  public @Valid Future<List<FullDomainDto>> getDomains(
      @Parameter(required = false, hidden = true) Authentication auth) {

    Client client = null;
    try {
      client = getClientWithCatalogs(auth);
    } catch (NoSuchElementException e) {
      return CompletableFuture.supplyAsync(Collections::emptyList);
    }

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
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Domain loaded",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = FullDomainDto.class))),
    @ApiResponse(responseCode = "404", description = "Domain not found")
  })
  public @Valid Future<ResponseEntity<FullDomainDto>> getDomain(
      @Parameter(required = false, hidden = true) Authentication auth,
      @PathVariable String id,
      WebRequest request) {
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
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Domain exported",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = FullDomainDto.class))),
    @ApiResponse(responseCode = "404", description = "Domain not found")
  })
  public @Valid CompletableFuture<ResponseEntity<ExportDto>> exportDomain(
      @Parameter(required = false, hidden = true) Authentication auth,
      @PathVariable String id,
      WebRequest request) {
    Client client = getAuthenticatedClient(auth);
    CompletableFuture<ExportDto> domainFuture =
        useCaseInteractor.execute(
            exportDomainUseCase,
            new UseCase.IdAndClient(Key.uuidFrom(id), client),
            ExportDomainUseCase.OutputData::getExportDomain);
    return domainFuture.thenApply(
        domainDto -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(domainDto));
  }

  @PostMapping
  @Operation(summary = "Creates blank new domain")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Domain created"),
    @ApiResponse(responseCode = "409", description = "Templates with name already exist")
  })
  public CompletableFuture<ResponseEntity<ApiResponseBody>> createDomain(
      Authentication auth,
      @Pattern(
              regexp = Patterns.UUID,
              message = "ID must be a valid UUID string following RFC 4122.")
          @Valid
          @RequestBody
          CreateDomainDto domainDto) {
    return useCaseInteractor.execute(
        createDomainUseCase,
        new CreateDomainUseCase.InputData(
            getAuthenticatedClient(auth),
            domainDto.getName(),
            domainDto.getAbbreviation(),
            domainDto.getDescription(),
            domainDto.getAuthority()),
        output -> {
          ApiResponseBody body = CreateOutputMapper.map(output.getDomain());
          return RestApiResponse.created(URL_BASE_PATH, body);
        });
  }

  @PostMapping(value = "/{id}/createdomaintemplate")
  @Operation(summary = "Creates a domaintemplate from a domain")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "DomainTemplate created"),
    @ApiResponse(responseCode = "400", description = "Invalid version"),
    @ApiResponse(responseCode = "409", description = "Template with version already exists"),
    @ApiResponse(
        responseCode = "422",
        description = "Version is lower than current template version"),
  })
  public CompletableFuture<ResponseEntity<IdRef<DomainTemplate>>> createDomainTemplatefromDomain(
      Authentication auth,
      @Pattern(
              regexp = Patterns.UUID,
              message = "ID must be a valid UUID string following RFC 4122.")
          @PathVariable
          String id,
      @Valid @RequestBody CreateDomainTemplateFromDomainParameterDto createParameter) {
    Client client = getAuthenticatedClient(auth);
    if (createParameter == null) {
      throw new IllegalArgumentException("create parameter cannot be null");
    }
    CompletableFuture<IdRef<DomainTemplate>> completableFuture =
        useCaseInteractor.execute(
            createDomainTemplateFromDomainUseCase,
            new CreateDomainTemplateFromDomainUseCase.InputData(
                Key.uuidFrom(id),
                parseVersion(createParameter.getVersion()),
                client,
                (domainTemplateId) -> buildProfiles(createParameter, id, domainTemplateId)),
            out -> IdRef.from(out.getNewDomainTemplate(), referenceAssembler));
    return completableFuture.thenApply(result -> ResponseEntity.status(201).body(result));
  }

  private Map<String, ProfileDefinition> buildProfiles(
      CreateDomainTemplateFromDomainParameterDto createParameter,
      String domainId,
      Key<UUID> domainTemplateId) {
    Map<String, ProfileDefinition> profiles = new HashMap<>();

    createParameter
        .getProfiles()
        .forEach(
            (name, unitId) -> {
              try {
                UnitDumpDto dump =
                    useCaseInteractor
                        .execute(
                            getUnitDumpUseCase,
                            (Supplier<GetUnitDumpUseCase.InputData>)
                                () -> UnitDumpMapper.mapInput(unitId),
                            out -> UnitDumpMapper.mapOutput(out, entityToDtoTransformer))
                        .get();
                Set<AbstractElementDto> elements = dump.getElements();
                elements.forEach(e -> e.transferToDomain(domainId, domainTemplateId.uuidValue()));
                Set<AbstractRiskDto> risks = dump.getRisks();
                risks.forEach(e -> e.transferToDomain(domainId, domainTemplateId.uuidValue()));

                log.info(
                    "dump size, elements:{} risks:{}",
                    dump.getElements().size(),
                    dump.getRisks().size());
                profiles.put(name, ProfileDefinition.of(elements, risks));
              } catch (InterruptedException ex) {
                throw new InternalProccesingException("Internal error", ex);
              } catch (ExecutionException ex) {
                if (ex.getCause() instanceof RuntimeException)
                  throw (RuntimeException) ex.getCause();
              }
            });
    return profiles;
  }

  @Override
  @SuppressFBWarnings // ignore warning on call to method proxy factory
  protected String buildSearchUri(String id) {
    return linkTo(methodOn(DomainController.class).runSearch(ANY_AUTH, id)).withSelfRel().getHref();
  }

  @GetMapping(value = "/searches/{searchId}")
  @Operation(summary = "Finds domains for the search.")
  public @Valid Future<List<FullDomainDto>> runSearch(
      @Parameter(required = false, hidden = true) Authentication auth,
      @PathVariable String searchId) {
    // TODO: VEO-498 Implement Domain Search
    try {
      SearchQueryDto.decodeFromSearchId(searchId);
      return getDomains(auth);
    } catch (IOException e) {
      log.error("Could not decode search URL: {}", e.getLocalizedMessage());
      throw new IllegalArgumentException("Could not decode search URL.");
    }
  }

  @PutMapping(value = "/{id}/element-type-definitions/{type:[\\w]+}")
  @Operation(summary = "Updates an element type definition in a domain")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Element type definition updated")
  })
  public CompletableFuture<ResponseEntity<ApiResponseBody>> updateElementTypeDefinition(
      Authentication auth,
      @PathVariable String id,
      @PathVariable EntityType type,
      @RequestBody ElementTypeDefinitionDto elementTypeDefinitionDto) {
    Client client = getAuthenticatedClient(auth);
    return useCaseInteractor.execute(
        updateElementTypeDefinitionUseCase,
        new UpdateElementTypeDefinitionUseCase.InputData(
            client,
            Key.uuidFrom(id),
            type,
            dtoToEntityTransformer.mapElementTypeDefinition(
                type.getSingularTerm(), elementTypeDefinitionDto, null)),
        out -> ResponseEntity.noContent().build());
  }

  @PostMapping(value = "/{id}/elementtypedefinitions/{type:[\\w]+}/updatefromobjectschema")
  @Operation(
      summary =
          "Updates a domain with an entity schema. Deprecated, use PUT /domains/{id}/element-type-definitions/{type}",
      deprecated = true)
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Schema updated")})
  public CompletableFuture<ResponseEntity<ApiResponseBody>> updateDomainWithSchema(
      Authentication auth,
      @PathVariable String id,
      @PathVariable EntityType type,
      @RequestBody JsonNode schemaNode) {
    Client client = getAuthenticatedClient(auth);
    try {
      ElementTypeDefinition typeDefinition =
          objectSchemaParser.parseTypeDefinitionFromObjectSchema(type, schemaNode);
      return useCaseInteractor.execute(
          updateElementTypeDefinitionUseCase,
          new UpdateElementTypeDefinitionUseCase.InputData(
              client, Key.uuidFrom(id), type, typeDefinition),
          out -> ResponseEntity.noContent().build());
    } catch (JsonProcessingException e) {
      log.error("Cannot parse object schema: {}", e.getLocalizedMessage());
      throw new IllegalArgumentException("Cannot parse object schema.");
    }
  }

  @GetMapping(value = "/{id}/element-status-count")
  @Operation(summary = "Retrieve element counts grouped by subType and status")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Elements counted",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ElementStatusCounts.class))),
    @ApiResponse(responseCode = "404", description = "Domain not found")
  })
  public @Valid CompletableFuture<ResponseEntity<ElementStatusCounts>> getElementStatusCount(
      @Parameter(required = false, hidden = true) Authentication auth,
      @PathVariable String id,
      @UnitUuidParam @RequestParam(value = UNIT_PARAM, required = true) String unitId,
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
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Profile applied"),
    @ApiResponse(responseCode = "404", description = "Domain not found"),
    @ApiResponse(responseCode = "404", description = "Unit not found"),
  })
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
