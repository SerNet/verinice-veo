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

import static org.veo.rest.ControllerConstants.DISPLAY_NAME_PARAM;
import static org.veo.rest.ControllerConstants.DOMAIN_PARAM;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import org.veo.adapter.presenter.api.dto.full.FullCatalogDto;
import org.veo.adapter.presenter.api.dto.full.FullCatalogItemDto;
import org.veo.core.entity.Catalog;
import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCaseInteractor;
import org.veo.core.usecase.catalog.GetCatalogUseCase;
import org.veo.core.usecase.catalog.GetCatalogsUseCase;
import org.veo.core.usecase.catalogitem.GetCatalogItemUseCase;
import org.veo.core.usecase.catalogitem.GetCatalogItemsUseCase;
import org.veo.rest.annotations.UnitUuidParam;
import org.veo.rest.security.ApplicationUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * REST service which provides methods to manage catalogs.
 * <p>
 * Uses async calls with {@code CompletableFuture} to parallelize long running
 * operations (i.e. network calls to the database or to other HTTP services).
 *
 * @see <a href=
 *      "https://spring.io/guides/gs/async-method">https://spring.io/guides/gs/async-method/</a>
 */
@RestController
@RequestMapping(CatalogController.URL_BASE_PATH)
public class CatalogController extends AbstractEntityController {

    public static final String URL_BASE_PATH = "/" + Catalog.PLURAL_TERM;

    private final UseCaseInteractor useCaseInteractor;
    private final GetCatalogUseCase getCatalogUseCase;
    private final GetCatalogsUseCase getCatalogsUseCase;
    private final GetCatalogItemUseCase getCatalogItemUseCase;
    private final GetCatalogItemsUseCase getCatalogItemsUseCase;

    public CatalogController(UseCaseInteractor useCaseInteractor,
            GetCatalogUseCase getCatalogUseCase, GetCatalogsUseCase getCatalogsUseCase,
            GetCatalogItemUseCase getCatalogItemUseCase,
            GetCatalogItemsUseCase getCatalogItemsUseCase) {
        this.useCaseInteractor = useCaseInteractor;
        this.getCatalogUseCase = getCatalogUseCase;
        this.getCatalogsUseCase = getCatalogsUseCase;
        this.getCatalogItemUseCase = getCatalogItemUseCase;
        this.getCatalogItemsUseCase = getCatalogItemsUseCase;
    }

    @GetMapping(value = "/{id}")
    @Operation(summary = "Loads a catalog")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Catalog loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = FullCatalogDto.class))),
            @ApiResponse(responseCode = "404", description = "Catalog not found") })
    public @Valid CompletableFuture<ResponseEntity<FullCatalogDto>> getCatalog(
            @Parameter(required = false, hidden = true) Authentication auth,
            @PathVariable String id, WebRequest request) {
        Client client = getClientWithCatalogsAndItems(auth, false);
        if (getEtag(Catalog.class, id).map(request::checkNotModified)
                                      .orElse(false)) {
            return null;
        }
        CompletableFuture<FullCatalogDto> catalogFuture = useCaseInteractor.execute(getCatalogUseCase,
                                                                                    new UseCase.IdAndClient(
                                                                                            Key.uuidFrom(id),
                                                                                            client),
                                                                                    output -> entityToDtoTransformer.transformCatalog2Dto(output.getCatalog()));
        return catalogFuture.thenApply(catalogDto -> ResponseEntity.ok()
                                                                   .cacheControl(defaultCacheControl)
                                                                   .body(catalogDto));
    }

    @GetMapping
    @Operation(summary = "Loads all catalogs")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Catalogs loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            array = @ArraySchema(schema = @Schema(implementation = FullCatalogDto.class)))) })
    public @Valid CompletableFuture<List<FullCatalogDto>> getCatalogs(
            @Parameter(required = false, hidden = true) Authentication auth,
            @UnitUuidParam @RequestParam(value = DOMAIN_PARAM, required = false) String domainUuid,
            @RequestParam(value = DISPLAY_NAME_PARAM, required = false) String displayName) {
        Client client = null;
        try {
            client = getClientWithCatalogsAndItems(auth, false);
        } catch (NoSuchElementException e) {
            return CompletableFuture.supplyAsync(Collections::emptyList);
        }

        final GetCatalogsUseCase.InputData inputData = new GetCatalogsUseCase.InputData(
                Optional.ofNullable(domainUuid)
                        .map(Key::uuidFrom),
                client);

        return useCaseInteractor.execute(getCatalogsUseCase, inputData, output -> {
            return output.getCatalogs()
                         .stream()
                         .map(u -> entityToDtoTransformer.transformCatalog2Dto(u))
                         .collect(Collectors.toList());
        });
    }

    @GetMapping(value = "/{id}/items")
    @Operation(summary = "Loads all items of a catalog")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "CatalogItems loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            array = @ArraySchema(schema = @Schema(implementation = FullCatalogItemDto.class)))) })
    public @Valid CompletableFuture<ResponseEntity<List<FullCatalogItemDto>>> getCatalogItems(
            @Parameter(required = false, hidden = true) Authentication auth,
            @PathVariable String id,
            @UnitUuidParam @RequestParam(value = DOMAIN_PARAM, required = false) String domainUuid,
            WebRequest request) {
        Client client = null;
        try {
            client = getClientWithCatalogsAndItems(auth, true);
        } catch (NoSuchElementException e) {
            return CompletableFuture.supplyAsync(() -> ResponseEntity.ok(Collections.emptyList()));
        }
        if (getEtag(Catalog.class, id).map(request::checkNotModified)
                                      .orElse(false)) {
            return null;
        }

        final GetCatalogItemsUseCase.InputData inputData = new GetCatalogItemsUseCase.InputData(
                Optional.empty(), Key.uuidFrom(id), Optional.ofNullable(domainUuid)
                                                            .map(Key::uuidFrom),
                client);

        return useCaseInteractor.execute(getCatalogItemsUseCase, inputData, output -> {
            return ResponseEntity.ok()
                                 .cacheControl(defaultCacheControl)
                                 .body(output.getCatalogItems()
                                             .stream()
                                             .map(u -> entityToDtoTransformer.transformCatalogItem2Dto(u,
                                                                                                       true))
                                             .collect(Collectors.toList()));
        });
    }

    @GetMapping(value = "/{id}/items/{itemId}")
    @Operation(summary = "Loads a catalogitem")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "CatalogItem loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = FullCatalogItemDto.class))),
            @ApiResponse(responseCode = "404", description = "CatalogItem not found") })
    public @Valid CompletableFuture<ResponseEntity<FullCatalogItemDto>> getCatalogItem(
            @Parameter(required = false, hidden = true) Authentication auth,
            @PathVariable String id, @PathVariable String itemId,
            @UnitUuidParam @RequestParam(value = DOMAIN_PARAM, required = false) String domainUuid,
            WebRequest request) {
        Client client = getClientWithCatalogsAndItems(auth, true);
        if (getEtag(CatalogItem.class, itemId).map(request::checkNotModified)
                                              .orElse(false)) {
            return null;
        }

        CompletableFuture<FullCatalogItemDto> catalogitemFuture = useCaseInteractor.execute(getCatalogItemUseCase,
                                                                                            new GetCatalogItemUseCase.InputData(
                                                                                                    Key.uuidFrom(itemId),
                                                                                                    Key.uuidFrom(id),
                                                                                                    Optional.ofNullable(domainUuid)
                                                                                                            .map(Key::uuidFrom),
                                                                                                    client),
                                                                                            output -> entityToDtoTransformer.transformCatalogItem2Dto(output.getCatalogItem(),
                                                                                                                                                      false));
        return catalogitemFuture.thenApply(catalogitemDto -> ResponseEntity.ok()
                                                                           .cacheControl(defaultCacheControl)
                                                                           .body(catalogitemDto));
    }

    protected Client getClientWithCatalogsAndItems(Authentication auth,
            boolean loadCatalogItemsDetails) {
        ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
        Key<UUID> id = Key.uuidFrom(user.getClientId());
        Optional<Client> client = loadCatalogItemsDetails
                ? clientRepository.findByIdFetchCatalogsAndItemsAndTailoringReferences(id)
                : clientRepository.findByIdFetchCatalogsAndItems(id);
        return client.orElseThrow();
    }

    @Override
    protected String buildSearchUri(String id) {
        // TODO: VEO-500 Implement Catalog Search
        return null;
    }

    public @Valid CompletableFuture<List<FullCatalogDto>> runSearch(
            @Parameter(required = false, hidden = true) Authentication auth,
            @PathVariable String searchId) {
        return null;
    }

}