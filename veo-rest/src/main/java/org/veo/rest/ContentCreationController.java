/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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

import static org.veo.adapter.presenter.api.io.mapper.VersionMapper.parseVersion;
import static org.veo.rest.ControllerConstants.UUID_DESCRIPTION;
import static org.veo.rest.ControllerConstants.UUID_EXAMPLE;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import org.veo.adapter.presenter.api.Patterns;
import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.dto.AbstractElementDto;
import org.veo.adapter.presenter.api.dto.AbstractRiskDto;
import org.veo.adapter.presenter.api.dto.ElementTypeDefinitionDto;
import org.veo.adapter.presenter.api.dto.UnitDumpDto;
import org.veo.adapter.presenter.api.dto.create.CreateDomainDto;
import org.veo.adapter.presenter.api.io.mapper.CreateOutputMapper;
import org.veo.adapter.presenter.api.io.mapper.UnitDumpMapper;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityTransformer;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.adapter.service.ObjectSchemaParser;
import org.veo.adapter.service.domaintemplate.dto.CreateDomainTemplateFromDomainParameterDto;
import org.veo.core.entity.Client;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.Key;
import org.veo.core.entity.decision.Decision;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.profile.ProfileDefinition;
import org.veo.core.entity.riskdefinition.RiskDefinition;
import org.veo.core.usecase.UseCase.IdAndClient;
import org.veo.core.usecase.domain.CreateDomainUseCase;
import org.veo.core.usecase.domain.DeleteDecisionUseCase;
import org.veo.core.usecase.domain.DeleteDomainUseCase;
import org.veo.core.usecase.domain.DeleteRiskDefinitionUseCase;
import org.veo.core.usecase.domain.SaveDecisionUseCase;
import org.veo.core.usecase.domain.SaveRiskDefinitionUseCase;
import org.veo.core.usecase.domain.UpdateElementTypeDefinitionUseCase;
import org.veo.core.usecase.domaintemplate.CreateDomainTemplateFromDomainUseCase;
import org.veo.core.usecase.unit.GetUnitDumpUseCase;
import org.veo.rest.common.RestApiResponse;
import org.veo.rest.security.ApplicationUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(ContentCreationController.URL_BASE_PATH)
@RequiredArgsConstructor
@SecurityRequirement(name = RestApplication.SECURITY_SCHEME_OAUTH)
public class ContentCreationController extends AbstractVeoController {
  private final ObjectSchemaParser objectSchemaParser;
  private final DtoToEntityTransformer dtoToEntityTransformer;
  private final GetUnitDumpUseCase getUnitDumpUseCase;
  private final EntityToDtoTransformer entityToDtoTransformer;
  private final UpdateElementTypeDefinitionUseCase updateElementTypeDefinitionUseCase;
  private final SaveDecisionUseCase saveDecisionUseCase;
  private final SaveRiskDefinitionUseCase saveRiskDefinitionUseCase;
  private final DeleteDecisionUseCase deleteDecisionUseCase;
  private final DeleteRiskDefinitionUseCase deleteRiskDefinitionUseCase;
  private final CreateDomainTemplateFromDomainUseCase createDomainTemplateFromDomainUseCase;
  private final DeleteDomainUseCase deleteDomainUseCase;
  public static final String URL_BASE_PATH = "/content-creation";

  private final CreateDomainUseCase createDomainUseCase;

  @PostMapping("/domains")
  @Operation(summary = "Creates blank new domain")
  @ApiResponse(responseCode = "201", description = "Domain created")
  @ApiResponse(responseCode = "409", description = "Templates with name already exist")
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

  @DeleteMapping("/domains/{id}")
  @Operation(summary = "Deletes a domain")
  @ApiResponse(responseCode = "204", description = "Domain deleted")
  @ApiResponse(responseCode = "404", description = "Domain not found")
  @ApiResponse(responseCode = "409", description = "Domain still in use")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteDomain(
      @Parameter(hidden = true) Authentication auth,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String id) {
    return useCaseInteractor.execute(
        deleteDomainUseCase,
        new IdAndClient(Key.uuidFrom(id), getAuthenticatedClient(auth)),
        output -> RestApiResponse.noContent());
  }

  @PutMapping(value = "/domains/{id}/element-type-definitions/{type}")
  @Operation(summary = "Updates an element type definition in a domain")
  @ApiResponse(responseCode = "204", description = "Element type definition updated")
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

  @PostMapping(value = "/domains/{id}/element-type-definitions/{type}/object-schema")
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

  @PutMapping("/domains/{domainId}/decisions/{decisionKey}")
  @Operation(summary = "Create or update decision with given key")
  @ApiResponse(responseCode = "201", description = "Decision created")
  @ApiResponse(responseCode = "200", description = "Decision updated")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> saveDecision(
      @Parameter(hidden = true) ApplicationUser user,
      @Parameter(hidden = true) ServletWebRequest request,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String domainId,
      @Parameter(
              required = true,
              example = "dpiaMandatory",
              description = "Decision identifier - unique within this domain")
          @PathVariable
          String decisionKey,
      @RequestBody Decision decision) {
    return useCaseInteractor.execute(
        saveDecisionUseCase,
        new SaveDecisionUseCase.InputData(
            Key.uuidFrom(user.getClientId()), Key.uuidFrom(domainId), decisionKey, decision),
        out ->
            out.isNewDecision()
                ? RestApiResponse.created(request.getRequest().getRequestURI(), "Decision created")
                : RestApiResponse.ok("Decision updated"));
  }

  @DeleteMapping("/domains/{domainId}/decisions/{decisionKey}")
  @Operation(summary = "Delete decision with given key")
  @ApiResponse(responseCode = "204", description = "Decision deleted")
  @ApiResponse(responseCode = "404", description = "Decision not found")
  @ApiResponse(responseCode = "404", description = "Domain not found")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteDecision(
      @Parameter(hidden = true) ApplicationUser user,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String domainId,
      @Parameter(
              required = true,
              example = "dpiaMandatory",
              description = "Decision identifier - unique within this domain")
          @PathVariable
          String decisionKey) {
    return useCaseInteractor.execute(
        deleteDecisionUseCase,
        new DeleteDecisionUseCase.InputData(
            Key.uuidFrom(user.getClientId()), Key.uuidFrom(domainId), decisionKey),
        out -> RestApiResponse.noContent());
  }

  @PutMapping("/domains/{domainId}/risk-definitions/{riskDefinitionId}")
  @Operation(summary = "Create a risk definition with given ID")
  @ApiResponse(responseCode = "201", description = "Risk definition created")
  @ApiResponse(
      responseCode = "409",
      description = "Risk definition already exists and update is not implemented yet")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> saveRiskDefinition(
      @Parameter(hidden = true) ApplicationUser user,
      @Parameter(hidden = true) ServletWebRequest request,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String domainId,
      @Parameter(
              required = true,
              example = "DSRA",
              description = "Risk definition identifier - unique within this domain")
          @PathVariable
          String riskDefinitionId,
      @RequestBody @Valid @NotNull RiskDefinition riskDefinition) {
    riskDefinition.setId(riskDefinitionId);
    return useCaseInteractor.execute(
        saveRiskDefinitionUseCase,
        new SaveRiskDefinitionUseCase.InputData(
            Key.uuidFrom(user.getClientId()),
            Key.uuidFrom(domainId),
            riskDefinitionId,
            riskDefinition),
        out ->
            out.isNewRiskDefinition()
                ? RestApiResponse.created(
                    request.getRequest().getRequestURI(), "Risk definition created")
                : RestApiResponse.ok("Risk definition updated"));
  }

  @DeleteMapping("/domains/{domainId}/risk-definitions/{riskDefinitionKey}")
  @Operation(summary = "Delete risk definition with given key")
  @ApiResponse(responseCode = "204", description = "Risk definition deleted")
  @ApiResponse(responseCode = "404", description = "Risk definition not found")
  @ApiResponse(responseCode = "404", description = "Domain not found")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteRiskDefinition(
      @Parameter(hidden = true) ApplicationUser user,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          String domainId,
      @Parameter(
              required = true,
              example = "DSRA",
              description = "Risk definition identifier - unique within this domain")
          @PathVariable
          String riskDefinitionKey) {
    return useCaseInteractor.execute(
        deleteRiskDefinitionUseCase,
        new DeleteRiskDefinitionUseCase.InputData(
            Key.uuidFrom(user.getClientId()), Key.uuidFrom(domainId), riskDefinitionKey),
        out -> RestApiResponse.noContent());
  }

  @PostMapping(value = "/domains/{id}/template")
  @Operation(summary = "Creates a domaintemplate from a domain")
  @ApiResponse(responseCode = "201", description = "DomainTemplate created")
  @ApiResponse(responseCode = "400", description = "Invalid version")
  @ApiResponse(responseCode = "409", description = "Template with version already exists")
  @ApiResponse(responseCode = "422", description = "Version is lower than current template version")
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
                domainTemplateId -> buildProfiles(createParameter, id, domainTemplateId)),
            out -> IdRef.from(out.getNewDomainTemplate(), referenceAssembler));
    return completableFuture.thenApply(result -> ResponseEntity.status(201).body(result));
  }

  // TODO VEO-2010 this is all so hideous
  private Map<String, ProfileDefinition> buildProfiles(
      CreateDomainTemplateFromDomainParameterDto createParameter,
      String domainId,
      Key<UUID> domainTemplateId) {
    Map<String, ProfileDefinition> profiles = new HashMap<>();

    createParameter
        .getProfiles()
        .forEach(
            (name, creationParameters) -> {
              try {
                UnitDumpDto dump =
                    useCaseInteractor
                        .execute(
                            getUnitDumpUseCase,
                            (Supplier<GetUnitDumpUseCase.InputData>)
                                () ->
                                    UnitDumpMapper.mapInput(
                                        creationParameters.getUnitId(), domainId),
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
                profiles.put(
                    name,
                    new ProfileDefinition(
                        creationParameters.getName(),
                        creationParameters.getDescription(),
                        creationParameters.getLanguage(),
                        elements,
                        risks));
              } catch (InterruptedException ex) {
                throw new InternalProccesingException("Internal error", ex);
              } catch (ExecutionException ex) {
                if (ex.getCause() instanceof RuntimeException c) throw c;
              }
            });
    return profiles;
  }

  @InitBinder
  public void initBinder(WebDataBinder dataBinder) {
    dataBinder.registerCustomEditor(
        EntityType.class, new IgnoreCaseEnumConverter<>(EntityType.class));
  }
}
