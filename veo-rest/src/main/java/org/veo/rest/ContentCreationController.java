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
import static org.veo.core.entity.DomainBase.INSPECTION_ID_MAX_LENGTH;
import static org.veo.rest.ControllerConstants.DEFAULT_CACHE_CONTROL;
import static org.veo.rest.ControllerConstants.UNIT_PARAM;
import static org.veo.rest.ControllerConstants.UUID_DESCRIPTION;
import static org.veo.rest.ControllerConstants.UUID_EXAMPLE;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.dto.DomainMetadataDto;
import org.veo.adapter.presenter.api.dto.ElementTypeDefinitionDto;
import org.veo.adapter.presenter.api.dto.create.CreateDomainDto;
import org.veo.adapter.presenter.api.dto.create.CreateProfileDto;
import org.veo.adapter.presenter.api.io.mapper.CreateOutputMapper;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.adapter.service.ObjectSchemaParser;
import org.veo.adapter.service.domaintemplate.dto.CreateDomainTemplateFromDomainParameterDto;
import org.veo.adapter.service.domaintemplate.dto.ExportDomainTemplateDto;
import org.veo.adapter.service.domaintemplate.dto.ExportProfileDto;
import org.veo.core.entity.ControlImplementationConfigurationDto;
import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.IncarnationConfiguration;
import org.veo.core.entity.Profile;
import org.veo.core.entity.decision.Decision;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.domainmigration.DomainMigrationStep;
import org.veo.core.entity.inspection.Inspection;
import org.veo.core.entity.riskdefinition.RiskDefinition;
import org.veo.core.entity.riskdefinition.RiskDefinitionChange;
import org.veo.core.usecase.UseCase.EntityId;
import org.veo.core.usecase.domain.CreateCatalogFromUnitUseCase;
import org.veo.core.usecase.domain.CreateDomainUseCase;
import org.veo.core.usecase.domain.CreateProfileFromUnitUseCase;
import org.veo.core.usecase.domain.DeleteDecisionUseCase;
import org.veo.core.usecase.domain.DeleteDomainUseCase;
import org.veo.core.usecase.domain.DeleteInspectionUseCase;
import org.veo.core.usecase.domain.DeleteProfileUseCase;
import org.veo.core.usecase.domain.DeleteRiskDefinitionUseCase;
import org.veo.core.usecase.domain.GetUpdateDefinitionUseCase;
import org.veo.core.usecase.domain.SaveControlImplementationConfigurationUseCase;
import org.veo.core.usecase.domain.SaveDecisionUseCase;
import org.veo.core.usecase.domain.SaveDomainMetadataUseCase;
import org.veo.core.usecase.domain.SaveInspectionUseCase;
import org.veo.core.usecase.domain.SaveRiskDefinitionUseCase;
import org.veo.core.usecase.domain.SaveUpdateDefinitionUseCase;
import org.veo.core.usecase.domain.UpdateElementTypeDefinitionUseCase;
import org.veo.core.usecase.domaintemplate.CreateDomainTemplateFromDomainUseCase;
import org.veo.core.usecase.domaintemplate.CreateDomainTemplateUseCase;
import org.veo.core.usecase.domaintemplate.CreateProfileInDomainTemplateUseCase;
import org.veo.core.usecase.domaintemplate.DeleteProfileInDomainTemplateUseCase;
import org.veo.core.usecase.domaintemplate.GetDomainTemplateUseCase;
import org.veo.core.usecase.profile.SaveIncarnationConfigurationUseCase;
import org.veo.rest.common.RestApiResponse;
import org.veo.service.EtagService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(ContentCreationController.URL_BASE_PATH)
@RequiredArgsConstructor
@ApiResponse(responseCode = "401", description = "Content creator role required")
public class ContentCreationController extends AbstractVeoController {
  private final ObjectSchemaParser objectSchemaParser;
  private final EntityToDtoTransformer entityToDtoTransformer;
  private final UpdateElementTypeDefinitionUseCase updateElementTypeDefinitionUseCase;
  private final SaveIncarnationConfigurationUseCase saveIncarnationConfigurationUseCase;
  private final SaveControlImplementationConfigurationUseCase
      saveControlImplementationConfigurationUseCase;
  private final SaveDecisionUseCase saveDecisionUseCase;
  private final SaveInspectionUseCase saveInspectionUseCase;
  private final SaveRiskDefinitionUseCase saveRiskDefinitionUseCase;
  private final DeleteDecisionUseCase deleteDecisionUseCase;
  private final DeleteInspectionUseCase deleteInspectionUseCase;
  private final DeleteRiskDefinitionUseCase deleteRiskDefinitionUseCase;
  private final CreateDomainTemplateFromDomainUseCase createDomainTemplateFromDomainUseCase;
  private final CreateCatalogFromUnitUseCase createCatalogForDomainUseCase;
  private final CreateProfileFromUnitUseCase createProfileFromUnitUseCase;
  private final DeleteProfileUseCase deleteProfileUseCase;
  private final DeleteDomainUseCase deleteDomainUseCase;
  private final GetDomainTemplateUseCase getDomainTemplateUseCase;
  private final CreateProfileInDomainTemplateUseCase createProfileInDomainTemplate;
  private final DeleteProfileInDomainTemplateUseCase deleteProfileInDomainTemplateUseCase;
  private final GetUpdateDefinitionUseCase getUpdateDefinitionUseCase;
  private final SaveUpdateDefinitionUseCase saveUpdateDefinitionUseCase;

  public static final String URL_BASE_PATH = "/content-creation";

  private final CreateDomainUseCase createDomainUseCase;
  private final CreateDomainTemplateUseCase createDomainTemplatesUseCase;
  private final EtagService etagService;
  private final SaveDomainMetadataUseCase saveDomainMetadataUseCase;

  @PostMapping("/domains")
  @Operation(summary = "Creates blank new domain")
  @ApiResponse(responseCode = "201", description = "Domain created")
  @ApiResponse(responseCode = "409", description = "Templates with name already exist")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> createDomain(
      @Valid @RequestBody CreateDomainDto domainDto) {
    return useCaseInteractor.execute(
        createDomainUseCase,
        new CreateDomainUseCase.InputData(
            domainDto.getName(), domainDto.getAuthority(), domainDto.getTranslations()),
        output -> {
          ApiResponseBody body = CreateOutputMapper.map(output.domain());
          return RestApiResponse.created(URL_BASE_PATH, body);
        });
  }

  @DeleteMapping("/domains/{id}")
  @Operation(summary = "Deletes a domain")
  @ApiResponse(responseCode = "204", description = "Domain deleted")
  @ApiResponse(responseCode = "404", description = "Domain not found")
  @ApiResponse(responseCode = "409", description = "Domain still in use")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteDomain(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID id) {
    return useCaseInteractor.execute(
        deleteDomainUseCase, new EntityId(id), output -> RestApiResponse.noContent());
  }

  @PutMapping(value = "/domains/{id}/element-type-definitions/{type}")
  @Operation(summary = "Updates an element type definition in a domain")
  @ApiResponse(responseCode = "204", description = "Element type definition updated")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> updateElementTypeDefinition(
      @PathVariable UUID id,
      @PathVariable ElementType type,
      @Valid @RequestBody ElementTypeDefinitionDto elementTypeDefinitionDto) {
    return useCaseInteractor.execute(
        updateElementTypeDefinitionUseCase,
        new UpdateElementTypeDefinitionUseCase.InputData(id, type, elementTypeDefinitionDto, false),
        out -> ResponseEntity.noContent().build());
  }

  @PostMapping(value = "/domains/{id}/element-type-definitions/{type}/object-schema")
  @Operation(
      summary =
          "Updates a domain with an entity schema. Deprecated, use PUT /domains/{id}/element-type-definitions/{type}",
      deprecated = true)
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Schema updated")})
  // TODO #3042: remove this when we remove support for JSON schema
  public CompletableFuture<ResponseEntity<ApiResponseBody>> updateDomainWithSchema(
      @PathVariable UUID id, @PathVariable ElementType type, @RequestBody JsonNode schemaNode) {
    try {
      ElementTypeDefinition typeDefinition =
          objectSchemaParser.parseTypeDefinitionFromObjectSchema(type, schemaNode);
      return useCaseInteractor.execute(
          updateElementTypeDefinitionUseCase,
          new UpdateElementTypeDefinitionUseCase.InputData(id, type, typeDefinition, true),
          out -> ResponseEntity.noContent().build());
    } catch (JsonProcessingException e) {
      log.error("Cannot parse object schema: {}", e.getLocalizedMessage());
      throw new IllegalArgumentException("Cannot parse object schema.");
    }
  }

  @PutMapping("/domains/{domainId}")
  @Operation(summary = "Update the domain metadata.")
  @ApiResponse(responseCode = "204", description = "Domain metadata updated")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> saveDomainMetadata(
      @Parameter(required = true, description = UUID_DESCRIPTION) @PathVariable UUID domainId,
      @RequestBody DomainMetadataDto domainMetadata) {
    return useCaseInteractor.execute(
        saveDomainMetadataUseCase,
        new SaveDomainMetadataUseCase.InputData(domainId, domainMetadata.getTranslations()),
        empty -> ResponseEntity.noContent().build());
  }

  @PutMapping("/domains/{domainId}/incarnation-configuration")
  @Operation(
      summary =
          "Update the incarnation config for this domain. This determines the default parameters when incarnating catalog items.")
  @ApiResponse(responseCode = "204", description = "Incarnation config updated")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> saveIncarnationConfiguration(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @RequestBody IncarnationConfiguration incarnationConfiguration) {
    return useCaseInteractor.execute(
        saveIncarnationConfigurationUseCase,
        new SaveIncarnationConfigurationUseCase.InputData(domainId, incarnationConfiguration),
        empty -> ResponseEntity.noContent().build());
  }

  @PutMapping("/domains/{domainId}/control-implementation-configuration")
  @Operation(summary = "Update domain-specific configuration related to control implementations.")
  @ApiResponse(responseCode = "204", description = "Control implementations config updated")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> saveControlImplementationConfiguration(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      // TODO #3860 use ControlImplementationConfiguration type again
      @RequestBody @Valid
          ControlImplementationConfigurationDto controlImplementationConfiguration) {
    return useCaseInteractor.execute(
        saveControlImplementationConfigurationUseCase,
        new SaveControlImplementationConfigurationUseCase.InputData(
            domainId, controlImplementationConfiguration),
        empty -> ResponseEntity.noContent().build());
  }

  @PutMapping("/domains/{domainId}/decisions/{decisionKey}")
  @Operation(summary = "Create or update decision with given key")
  @ApiResponse(responseCode = "201", description = "Decision created")
  @ApiResponse(responseCode = "200", description = "Decision updated")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> saveDecision(
      @Parameter(hidden = true) ServletWebRequest request,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Parameter(
              required = true,
              example = "dpiaMandatory",
              description = "Decision identifier - unique within this domain")
          @PathVariable
          String decisionKey,
      @RequestBody Decision decision) {
    return useCaseInteractor.execute(
        saveDecisionUseCase,
        new SaveDecisionUseCase.InputData(domainId, decisionKey, decision),
        out ->
            out.newDecision()
                ? RestApiResponse.created(request.getRequest().getRequestURI(), "Decision created")
                : RestApiResponse.ok("Decision updated"));
  }

  @DeleteMapping("/domains/{domainId}/decisions/{decisionKey}")
  @Operation(summary = "Delete decision with given key")
  @ApiResponse(responseCode = "204", description = "Decision deleted")
  @ApiResponse(responseCode = "404", description = "Domain or decision not found")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteDecision(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Parameter(
              required = true,
              example = "dpiaMandatory",
              description = "Decision identifier - unique within this domain")
          @PathVariable
          String decisionKey) {
    return useCaseInteractor.execute(
        deleteDecisionUseCase,
        new DeleteDecisionUseCase.InputData(domainId, decisionKey),
        out -> RestApiResponse.noContent());
  }

  @PutMapping("/domains/{domainId}/inspections/{inspectionId}")
  @Operation(summary = "Create or update inspection with given key")
  @ApiResponse(responseCode = "201", description = "Inspection created")
  @ApiResponse(responseCode = "200", description = "Inspection updated")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> saveDecision(
      @Parameter(hidden = true) ServletWebRequest request,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Parameter(
              required = true,
              example = "dpiaMandatory",
              description = "Inspection identifier - unique within this domain")
          @PathVariable
          @Size(min = 1, max = INSPECTION_ID_MAX_LENGTH)
          String inspectionId,
      @RequestBody Inspection inspection) {
    return useCaseInteractor.execute(
        saveInspectionUseCase,
        new SaveInspectionUseCase.InputData(domainId, inspectionId, inspection),
        out ->
            out.newInspection()
                ? RestApiResponse.created(
                    request.getRequest().getRequestURI(), "Inspection created")
                : RestApiResponse.ok("Inspection updated"));
  }

  @DeleteMapping("/domains/{domainId}/inspections/{inspectionId}")
  @Operation(summary = "Delete inspection with given key")
  @ApiResponse(responseCode = "204", description = "Inspection deleted")
  @ApiResponse(responseCode = "404", description = "Domain or inspection not found")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteInspection(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Parameter(
              required = true,
              example = "dpiaMandatory",
              description = "Inspection identifier - unique within this domain")
          @Size(min = 1, max = INSPECTION_ID_MAX_LENGTH)
          @PathVariable
          String inspectionId) {
    return useCaseInteractor.execute(
        deleteInspectionUseCase,
        new DeleteInspectionUseCase.InputData(domainId, inspectionId),
        out -> RestApiResponse.noContent());
  }

  @PutMapping("/domains/{domainId}/risk-definitions/{riskDefinitionId}")
  @Operation(summary = "Create or update a risk definition with given ID")
  @ApiResponse(responseCode = "200", description = "Risk definition updated")
  @ApiResponse(responseCode = "201", description = "Risk definition created")
  @ApiResponse(
      responseCode = "422",
      description = "Requested risk definition modification is not supported yet")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> saveRiskDefinition(
      @Parameter(hidden = true) ServletWebRequest request,
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
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
            domainId,
            riskDefinitionId,
            riskDefinition,
            Set.of(
                RiskDefinitionChange.CategoryListAdd.class,
                RiskDefinitionChange.CategoryListRemove.class,
                RiskDefinitionChange.ColorDiff.class,
                RiskDefinitionChange.ImpactLinks.class,
                RiskDefinitionChange.ImplementationStateListResize.class,
                RiskDefinitionChange.NewRiskDefinition.class,
                RiskDefinitionChange.RiskMatrixAdd.class,
                RiskDefinitionChange.RiskMatrixDiff.class,
                RiskDefinitionChange.RiskMatrixRemove.class,
                RiskDefinitionChange.RiskMatrixResize.class,
                RiskDefinitionChange.RiskValueListResize.class,
                RiskDefinitionChange.ImpactListResize.class,
                RiskDefinitionChange.ProbabilityListResize.class,
                RiskDefinitionChange.TranslationDiff.class)),
        out ->
            out.newRiskDefinition()
                ? RestApiResponse.created(
                    request.getRequest().getRequestURI(), "Risk definition created")
                : RestApiResponse.ok("Risk definition updated"));
  }

  @DeleteMapping("/domains/{domainId}/risk-definitions/{riskDefinitionKey}")
  @Operation(summary = "Delete risk definition with given key")
  @ApiResponse(responseCode = "204", description = "Risk definition deleted")
  @ApiResponse(responseCode = "404", description = "Domain or risk definition not found")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteRiskDefinition(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @Parameter(
              required = true,
              example = "DSRA",
              description = "Risk definition identifier - unique within this domain")
          @PathVariable
          String riskDefinitionKey) {
    return useCaseInteractor.execute(
        deleteRiskDefinitionUseCase,
        new DeleteRiskDefinitionUseCase.InputData(domainId, riskDefinitionKey),
        out -> RestApiResponse.noContent());
  }

  @PutMapping("/domains/{domainId}/catalog-items")
  @Operation(summary = "Creates a new catalog from a unit for a domain")
  @ApiResponse(responseCode = "204", description = "Catalog items created")
  @ApiResponse(responseCode = "404", description = "Domain or unit not found")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> createCatalogForDomain(
      @PathVariable UUID domainId,
      @Parameter(description = "The id of the unit containing the catalog elements.")
          @RequestParam(name = UNIT_PARAM)
          UUID unitId) {
    return useCaseInteractor.execute(
        createCatalogForDomainUseCase,
        new CreateCatalogFromUnitUseCase.InputData(domainId, unitId),
        out -> RestApiResponse.noContent());
  }

  @PostMapping(value = "/domains/{domainId}/profiles")
  @Operation(summary = "Creates a profile for a domain")
  @ApiResponse(responseCode = "201", description = "Profile created")
  public CompletableFuture<ResponseEntity<IdRef<Profile>>> createProfileForDomain(
      @PathVariable UUID domainId,
      @RequestParam(name = UNIT_PARAM, required = false) UUID unitId,
      @Valid @NotNull @RequestBody CreateProfileDto createParameter) {
    return useCaseInteractor
        .execute(
            createProfileFromUnitUseCase,
            new CreateProfileFromUnitUseCase.InputData(
                domainId,
                unitId,
                null,
                createParameter.getName(),
                createParameter.getDescription(),
                createParameter.getLanguage(),
                createParameter.getProductId()),
            out -> IdRef.from(out.profile(), referenceAssembler))
        .thenApply(result -> ResponseEntity.status(201).body(result));
  }

  @PutMapping(value = "/domains/{domainId}/profiles/{profileId}")
  @Operation(summary = "Update a profile for a domain")
  @ApiResponse(responseCode = "204", description = "Profile updated")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> updateProfileForDomain(
      @PathVariable UUID domainId,
      @PathVariable @NotNull UUID profileId,
      @Parameter(
              description =
                  "Pass a unit ID to overwrite all items in the profile with "
                      + "new profile items created from the elements in that unit. "
                      + "Omit unit ID to leave current profile items untouched.")
          @RequestParam(name = UNIT_PARAM, required = false)
          UUID unitId,
      @Valid @NotNull @RequestBody CreateProfileDto createParameter) {
    return useCaseInteractor.execute(
        createProfileFromUnitUseCase,
        new CreateProfileFromUnitUseCase.InputData(
            domainId,
            unitId,
            profileId,
            createParameter.getName(),
            createParameter.getDescription(),
            createParameter.getLanguage(),
            createParameter.getProductId()),
        out -> RestApiResponse.noContent());
  }

  @DeleteMapping(value = "/domains/{domainId}/profiles/{profileId}")
  @Operation(summary = "Delete a profile")
  @ApiResponse(responseCode = "204", description = "Profile deleted")
  @ApiResponse(responseCode = "404", description = "Domain or profile not found")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteProfile(
      @PathVariable UUID domainId, @PathVariable @NotNull UUID profileId) {
    return useCaseInteractor.execute(
        deleteProfileUseCase,
        new DeleteProfileUseCase.InputData(domainId, profileId),
        out -> RestApiResponse.noContent());
  }

  @PostMapping(value = "/domains/{id}/template")
  @Operation(summary = "Creates a domain template from a domain")
  @ApiResponse(responseCode = "201", description = "Domain template created")
  @ApiResponse(responseCode = "400", description = "Invalid version")
  @ApiResponse(responseCode = "409", description = "Template with version already exists")
  @ApiResponse(responseCode = "422", description = "Version is lower than current template version")
  public CompletableFuture<ResponseEntity<IdRef<DomainTemplate>>> createDomainTemplatefromDomain(
      @PathVariable UUID id,
      @Valid @RequestBody CreateDomainTemplateFromDomainParameterDto createParameter) {
    if (createParameter == null) {
      throw new IllegalArgumentException("create parameter cannot be null");
    }
    CompletableFuture<IdRef<DomainTemplate>> completableFuture =
        useCaseInteractor.execute(
            createDomainTemplateFromDomainUseCase,
            new CreateDomainTemplateFromDomainUseCase.InputData(
                id, parseVersion(createParameter.getVersion())),
            out -> IdRef.from(out.newDomainTemplate(), referenceAssembler));
    return completableFuture.thenApply(result -> ResponseEntity.status(201).body(result));
  }

  @PostMapping(value = "/domain-templates/{id}/profiles")
  @Operation(summary = "Import or update a profile in a domain template")
  @ApiResponse(responseCode = "201", description = "Profile created")
  public CompletableFuture<ResponseEntity<IdRef<Profile>>> createProfileForDomainTemplate(
      @PathVariable UUID id, @Valid @NotNull @RequestBody ExportProfileDto profileDto) {
    return useCaseInteractor
        .execute(
            createProfileInDomainTemplate,
            new CreateProfileInDomainTemplateUseCase.InputData(id, profileDto),
            out -> IdRef.from(out.profile(), referenceAssembler))
        .thenApply(result -> ResponseEntity.status(201).body(result));
  }

  @DeleteMapping(value = "/domain-templates/{id}/profiles/{profileId}")
  @Operation(summary = "Delete a profile in a domain template")
  @ApiResponse(responseCode = "204", description = "Profile deleted")
  public Future<ResponseEntity<Void>> deleteProfileInDomainTemplate(
      @PathVariable UUID id, @PathVariable UUID profileId) {
    return useCaseInteractor.execute(
        deleteProfileInDomainTemplateUseCase,
        new DeleteProfileInDomainTemplateUseCase.InputData(id, profileId),
        out -> ResponseEntity.noContent().build());
  }

  @GetMapping(value = "/domain-templates/{id}")
  @Operation(summary = "Loads a domain template")
  @ApiResponse(
      responseCode = "200",
      description = "Domain template loaded",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ExportDomainTemplateDto.class)))
  @ApiResponse(responseCode = "404", description = "Domain template not found")
  @ApiResponse(responseCode = "400", description = "Bad request")
  public @Valid Future<ResponseEntity<ExportDomainTemplateDto>> getDomainTemplate(
      @PathVariable UUID id) {
    return useCaseInteractor
        .execute(
            getDomainTemplateUseCase,
            new EntityId(id),
            output -> entityToDtoTransformer.transformDomainTemplate2Dto(output.domainTemplate()))
        .thenApply(
            domainDto -> ResponseEntity.ok().cacheControl(DEFAULT_CACHE_CONTROL).body(domainDto));
  }

  @PostMapping(value = "/domain-templates", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Creates domain template")
  @ApiResponse(responseCode = "201", description = "Domain template created")
  @ApiResponse(responseCode = "409", description = "Domain template with given ID already exists")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> createDomainTemplate(
      @Valid @NotNull @RequestBody ExportDomainTemplateDto domainTemplateDto) {
    return doCreateDomainTemplate(domainTemplateDto);
  }

  @PostMapping(value = "/domain-templates", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Creates domain template")
  @ApiResponse(responseCode = "201", description = "Domain template created")
  @ApiResponse(responseCode = "409", description = "Domain template with given ID already exists")
  @ApiResponse(
      responseCode = "400",
      description = "The content of the domain template is not valid")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> createDomainTemplate(
      @NotNull @RequestPart MultipartFile file) {
    ExportDomainTemplateDto domainTemplateDto = parse(file, ExportDomainTemplateDto.class);
    return doCreateDomainTemplate(domainTemplateDto);
  }

  @PutMapping("/domains/{domainId}/migrations")
  @Operation(summary = "Create or update the migration definition")
  @ApiResponse(responseCode = "200", description = "Migration definition updated")
  @ApiResponse(responseCode = "422", description = "Migration definition not consistent.")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> saveUpdateDefinition(
      @Parameter(required = true, example = UUID_EXAMPLE, description = UUID_DESCRIPTION)
          @PathVariable
          UUID domainId,
      @RequestBody @Valid @NotNull List<DomainMigrationStep> domainMigrationSteps) {
    return useCaseInteractor.execute(
        saveUpdateDefinitionUseCase,
        new SaveUpdateDefinitionUseCase.InputData(domainId, domainMigrationSteps),
        out -> RestApiResponse.ok("Migrations updated"));
  }

  @GetMapping(value = "/domains/{domainId}/migrations")
  @Operation(
      summary = "Retrieve the defined migrations.",
      description = "Returns the set of migrations.")
  @ApiResponse(
      responseCode = "200",
      content = {
        @Content(array = @ArraySchema(schema = @Schema(implementation = DomainMigrationStep.class)))
      })
  @ApiResponse(responseCode = "404", description = "Domain not found or has no domain template")
  public @Valid Future<ResponseEntity<List<DomainMigrationStep>>> getUpdateDefinitions(
      @PathVariable UUID domainId, WebRequest request) {
    if (getEtag(domainId).map(request::checkNotModified).orElse(false)) {
      return null;
    }

    return useCaseInteractor
        .execute(
            getUpdateDefinitionUseCase,
            new EntityId(domainId),
            GetUpdateDefinitionUseCase.OutputData::migrationSteps)
        .thenApply(
            updateDefinition ->
                ResponseEntity.ok().cacheControl(DEFAULT_CACHE_CONTROL).body(updateDefinition));
  }

  private Optional<String> getEtag(UUID id) {
    return etagService.getEtag(Domain.class, id);
  }

  private CompletableFuture<ResponseEntity<ApiResponseBody>> doCreateDomainTemplate(
      ExportDomainTemplateDto domainTemplateDto) {
    return useCaseInteractor.execute(
        createDomainTemplatesUseCase,
        new CreateDomainTemplateUseCase.InputData(domainTemplateDto),
        out -> {
          var body = CreateOutputMapper.map(out.domainTemplate());
          return RestApiResponse.created(URL_BASE_PATH, body);
        });
  }
}
