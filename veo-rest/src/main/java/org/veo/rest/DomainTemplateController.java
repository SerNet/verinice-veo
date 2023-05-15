/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade.
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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.dto.DomainTemplateMetadataDto;
import org.veo.adapter.presenter.api.dto.full.FullDomainDto;
import org.veo.adapter.presenter.api.io.mapper.CreateDomainTemplateInputMapper;
import org.veo.adapter.presenter.api.io.mapper.CreateOutputMapper;
import org.veo.adapter.presenter.api.response.transformer.DomainAssociationTransformer;
import org.veo.adapter.service.domaintemplate.dto.TransformDomainTemplateDto;
import org.veo.core.entity.Client;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Key;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.entity.transform.IdentifiableFactory;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.domain.CreateDomainFromTemplateUseCase;
import org.veo.core.usecase.domaintemplate.CreateDomainTemplateUseCase;
import org.veo.core.usecase.domaintemplate.FindDomainTemplatesUseCase;
import org.veo.core.usecase.domaintemplate.GetDomainTemplateUseCase;
import org.veo.core.usecase.service.EntityStateMapper;
import org.veo.rest.common.RestApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

/**
 * REST service which provides methods to manage domain templates.
 *
 * <p>Uses async calls with {@code CompletableFuture} to parallelize long running operations (i.e.
 * network calls to the database or to other HTTP services).
 *
 * @see <a href=
 *     "https://spring.io/guides/gs/async-method">https://spring.io/guides/gs/async-method/</a>
 */
@RestController
@RequestMapping(DomainTemplateController.URL_BASE_PATH)
@RequiredArgsConstructor
@SecurityRequirement(name = RestApplication.SECURITY_SCHEME_OAUTH)
public class DomainTemplateController extends AbstractEntityController {

  public static final String URL_BASE_PATH = "/" + DomainTemplate.PLURAL_TERM;

  private final CreateDomainFromTemplateUseCase createDomainFromTemplateUseCase;
  private final CreateDomainTemplateUseCase createDomainTemplatesUseCase;
  private final EntityFactory entityFactory;
  private final IdentifiableFactory identifiableFactory;
  private final DomainAssociationTransformer domainAssociationTransformer;
  private final EntityStateMapper entityStateMapper;
  private final FindDomainTemplatesUseCase findDomainTemplatesUseCase;
  private final GetDomainTemplateUseCase getDomainTemplateUseCase;

  @GetMapping
  @Operation(summary = "Loads all domain templates (metadata only)")
  @ApiResponse(
      responseCode = "200",
      description = "DomainTemplates loaded",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = DomainTemplateMetadataDto.class)))
  public @Valid Future<ResponseEntity<List<DomainTemplateMetadataDto>>> getDomainTemplates() {
    return useCaseInteractor.execute(
        findDomainTemplatesUseCase,
        UseCase.EmptyInput.INSTANCE,
        out ->
            ResponseEntity.ok()
                .body(
                    out.getGetDomainTemplates().stream()
                        .map(entityToDtoTransformer::transformDomainTemplateMetadata2Dto)
                        .toList()));
  }

  @PostMapping(value = "/{id}/createdomains")
  @Operation(summary = "Creates domains from a domain template")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Domain(s) created")})
  public CompletableFuture<ResponseEntity<ApiResponseBody>> createDomainFromTemplate(
      Authentication auth,
      @PathVariable String id,
      @RequestParam(value = "clientids", required = false) List<String> clientIds) {
    return useCaseInteractor.execute(
        createDomainFromTemplateUseCase,
        new CreateDomainFromTemplateUseCase.InputData(id, Optional.ofNullable(clientIds)),
        out -> ResponseEntity.noContent().build());
  }

  @PostMapping()
  @Operation(summary = "Creates domain template")
  @ApiResponse(responseCode = "201", description = "Domain template created")
  @ApiResponse(responseCode = "409", description = "Domain template with given ID already exists")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> createDomainTemplate(
      @Valid @NotNull @RequestBody TransformDomainTemplateDto domainTemplateDto) {
    var input =
        CreateDomainTemplateInputMapper.map(
            domainTemplateDto,
            identifiableFactory,
            entityFactory,
            domainAssociationTransformer,
            entityStateMapper);
    return useCaseInteractor.execute(
        createDomainTemplatesUseCase,
        input,
        out -> {
          var body = CreateOutputMapper.map(out.getDomainTemplate());
          return RestApiResponse.created(URL_BASE_PATH, body);
        });
  }

  @GetMapping(value = "/{id}")
  @Operation(summary = "Loads a domaintemplate")
  @ApiResponse(
      responseCode = "200",
      description = "DomainTemplate loaded",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = FullDomainDto.class)))
  @ApiResponse(responseCode = "404", description = "DomainTemplate not found")
  public @Valid Future<ResponseEntity<TransformDomainTemplateDto>> getDomainTemplate(
      @Parameter(hidden = true) Authentication auth, @PathVariable String id, WebRequest request) {
    // TODO: VEO-1549 implement getDomainTemplate, hint: use getDomainTemplate(id, client,
    // toDtoFunction);
    return CompletableFuture.failedFuture(new UnsupportedOperationException("not implemented"));
  }

  @GetMapping(value = "/{id}/export")
  @Operation(summary = "export a domaintemplate")
  @ApiResponse(
      responseCode = "200",
      description = "DomainTemplate exported",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = TransformDomainTemplateDto.class)))
  @ApiResponse(responseCode = "404", description = "DomainTemplate not found")
  public @Valid Future<ResponseEntity<TransformDomainTemplateDto>> exportDomainTemplate(
      @Parameter(hidden = true) Authentication auth, @PathVariable String id, WebRequest request) {
    Client client = getAuthenticatedClient(auth);
    Function<GetDomainTemplateUseCase.OutputData, TransformDomainTemplateDto> toDtoFunction =
        output -> entityToDtoTransformer.transformDomainTemplate2Dto(output.getDomainTemplate());
    return getDomainTemplate(id, client, toDtoFunction);
  }

  private Future<ResponseEntity<TransformDomainTemplateDto>> getDomainTemplate(
      String id,
      Client client,
      Function<GetDomainTemplateUseCase.OutputData, TransformDomainTemplateDto> toDtoFunction) {
    var domainFuture =
        useCaseInteractor.execute(
            getDomainTemplateUseCase,
            new UseCase.IdAndClient(Key.uuidFrom(id), client),
            toDtoFunction);
    return domainFuture.thenApply(
        domainDto -> ResponseEntity.ok().cacheControl(defaultCacheControl).body(domainDto));
  }

  @Override
  protected String buildSearchUri(String searchId) {
    // TODO: VEO-499 Implement DomainTemplate Search
    return null;
  }
}
