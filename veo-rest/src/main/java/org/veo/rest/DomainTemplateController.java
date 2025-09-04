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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.dto.DomainTemplateMetadataDto;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.domain.CreateDomainFromTemplateUseCase;
import org.veo.core.usecase.domain.GetClientIdsWhereDomainTemplateNotAppliedUseCase;
import org.veo.core.usecase.domaintemplate.FindDomainTemplatesUseCase;

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
 * REST service which provides methods to manage domain templates.
 *
 * <p>Uses async calls with {@code CompletableFuture} to parallelize long-running operations (i.e.
 * network calls to the database or to other HTTP services).
 *
 * @see <a href=
 *     "https://spring.io/guides/gs/async-method">https://spring.io/guides/gs/async-method/</a>
 */
@RestController
@RequestMapping(DomainTemplateController.URL_BASE_PATH)
@RequiredArgsConstructor
@Slf4j
public class DomainTemplateController extends AbstractEntityController {

  public static final String URL_BASE_PATH = "/" + DomainTemplate.PLURAL_TERM;

  private final GetClientIdsWhereDomainTemplateNotAppliedUseCase
      getClientIdsWhereDomainTemplateNotAppliedUseCase;
  private final CreateDomainFromTemplateUseCase createDomainFromTemplateUseCase;
  private final FindDomainTemplatesUseCase findDomainTemplatesUseCase;

  @GetMapping
  @Operation(summary = "Loads all domain templates (metadata only)")
  @ApiResponse(
      responseCode = "200",
      description = "Domain templates loaded",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array =
                  @ArraySchema(schema = @Schema(implementation = DomainTemplateMetadataDto.class))))
  public @Valid Future<ResponseEntity<List<DomainTemplateMetadataDto>>> getDomainTemplates() {
    return useCaseInteractor.execute(
        findDomainTemplatesUseCase,
        UseCase.EmptyInput.INSTANCE,
        out ->
            ResponseEntity.ok()
                .body(
                    out.getDomainTemplates().stream()
                        .map(entityToDtoTransformer::transformDomainTemplateMetadata2Dto)
                        .toList()));
  }

  @PostMapping(value = "/{id}/createdomains")
  @Operation(
      summary = "Creates domains from a domain template",
      description =
          "Creates domains from a domain template in all, or a set of clients, with a domain connected to a previous domain template of that kind.",
      parameters = {
        @Parameter(
            name = "restrictToClientsWithExistingDomain",
            description =
                "When set to false, a domain will be created for all clients, regardless of their domains."),
        @Parameter(
            name = "clientids",
            description = "A set of client ids to create the domain template for"),
        @Parameter(name = "id", description = "The id of the domain template")
      })
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Domain(s) created")})
  public CompletableFuture<ResponseEntity<ApiResponseBody>> createDomainFromTemplate(
      @PathVariable UUID id,
      @RequestParam(
              value = "restrictToClientsWithExistingDomain",
              required = false,
              defaultValue = "true")
          boolean restrictToClientsWithExistingDomain,
      @RequestParam(value = "clientids", required = false) List<UUID> inputClientIds) {
    return (inputClientIds != null
            ? CompletableFuture.completedFuture(new HashSet<>(inputClientIds))
            : useCaseInteractor.execute(
                getClientIdsWhereDomainTemplateNotAppliedUseCase,
                new GetClientIdsWhereDomainTemplateNotAppliedUseCase.InputData(
                    id, restrictToClientsWithExistingDomain),
                GetClientIdsWhereDomainTemplateNotAppliedUseCase.OutputData::clientIds))
        .thenCompose(clientIds -> createDomains(id, clientIds))
        .thenApply(v -> ResponseEntity.noContent().build());
  }

  private CompletableFuture<Class<Void>> createDomains(UUID domainTemplateId, Set<UUID> clientIds) {
    log.info("Creating domain from template {} in {} clients", domainTemplateId, clientIds.size());
    var future = CompletableFuture.completedFuture(void.class);
    var i = new AtomicInteger();
    for (var clientId : clientIds) {
      future =
          future
              .thenCompose(
                  nothing ->
                      useCaseInteractor.execute(
                          createDomainFromTemplateUseCase,
                          new CreateDomainFromTemplateUseCase.InputData(
                              domainTemplateId, clientId, true),
                          out -> void.class))
              .thenApply(
                  nothing -> {
                    log.info("{} / {} domains created", i.incrementAndGet(), clientIds.size());
                    return void.class;
                  });
    }
    return future;
  }
}
