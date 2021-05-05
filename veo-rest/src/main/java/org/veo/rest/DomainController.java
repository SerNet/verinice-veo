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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.veo.adapter.presenter.api.dto.SearchQueryDto;
import org.veo.adapter.presenter.api.dto.full.FullDomainDto;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCaseInteractor;
import org.veo.core.usecase.common.ETag;
import org.veo.core.usecase.domain.GetDomainUseCase;
import org.veo.core.usecase.domain.GetDomainsUseCase;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;

/**
 * REST service which provides methods to manage domains.
 * <p>
 * Uses async calls with {@code CompletableFuture} to parallelize long running
 * operations (i.e. network calls to the database or to other HTTP services).
 *
 * @see <a href=
 *      "https://spring.io/guides/gs/async-method">https://spring.io/guides/gs/async-method/</a>
 */
@RestController
@RequestMapping(DomainController.URL_BASE_PATH)
@Slf4j
public class DomainController extends AbstractEntityController {

    public static final String URL_BASE_PATH = "/" + Domain.PLURAL_TERM;

    private final UseCaseInteractor useCaseInteractor;
    private final GetDomainUseCase getDomainUseCase;
    private final GetDomainsUseCase getDomainsUseCase;

    public DomainController(UseCaseInteractor useCaseInteractor, GetDomainUseCase getDomainUseCase,
            GetDomainsUseCase getDomainsUseCase) {
        this.useCaseInteractor = useCaseInteractor;
        this.getDomainUseCase = getDomainUseCase;
        this.getDomainsUseCase = getDomainsUseCase;
    }

    @GetMapping
    @Operation(summary = "Loads all domains")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Domains loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            array = @ArraySchema(schema = @Schema(implementation = FullDomainDto.class)))) })

    public @Valid CompletableFuture<List<FullDomainDto>> getDomains(
            @Parameter(required = false, hidden = true) Authentication auth) {

        Client client = null;
        try {
            client = getAuthenticatedClient(auth);
        } catch (NoSuchElementException e) {
            return CompletableFuture.supplyAsync(Collections::emptyList);
        }

        final GetDomainsUseCase.InputData inputData = new GetDomainsUseCase.InputData(client);
        return useCaseInteractor.execute(getDomainsUseCase, inputData, output -> {
            return output.getObjects()
                         .stream()
                         .map(u -> entityToDtoTransformer.transformDomain2Dto(u))
                         .collect(Collectors.toList());
        });

    }

    @Async
    @GetMapping(value = "/{id}")
    @Operation(summary = "Loads a domain")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Domain loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = FullDomainDto.class))),
            @ApiResponse(responseCode = "404", description = "Domain not found") })
    public @Valid CompletableFuture<ResponseEntity<FullDomainDto>> getDomain(
            @Parameter(required = false, hidden = true) Authentication auth,
            @PathVariable String id) {
        Client client = getAuthenticatedClient(auth);
        CompletableFuture<FullDomainDto> domainFuture = useCaseInteractor.execute(getDomainUseCase,
                                                                                  new UseCase.IdAndClient(
                                                                                          Key.uuidFrom(id),
                                                                                          client),
                                                                                  output -> entityToDtoTransformer.transformDomain2Dto(output.getDomain()));
        return domainFuture.thenApply(domainDto -> ResponseEntity.ok()
                                                                 .eTag(ETag.from(domainDto.getId(),
                                                                                 domainDto.getVersion()))
                                                                 .body(domainDto));
    }

    @Override
    @SuppressFBWarnings // ignore warning on call to method proxy factory
    protected String buildSearchUri(String id) {
        return linkTo(methodOn(DomainController.class).runSearch(ANY_AUTH, id)).withSelfRel()
                                                                               .getHref();
    }

    @GetMapping(value = "/searches/{searchId}")
    @Operation(summary = "Finds domains for the search.")
    public @Valid CompletableFuture<List<FullDomainDto>> runSearch(
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
}