/*******************************************************************************
 * Copyright (c) 2019 Daniel Murygin.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.rest;

import static org.veo.rest.ControllerConstants.PARENT_PARAM;
import static org.veo.rest.ControllerConstants.UUID_PARAM;
import static org.veo.rest.ControllerConstants.UUID_REGEX;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.io.mapper.CreateAssetOutputMapper;
import org.veo.adapter.presenter.api.request.CreateAssetDto;
import org.veo.adapter.presenter.api.response.AssetDto;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityContext;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoContext;
import org.veo.core.entity.Asset;
import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.usecase.asset.CreateAssetUseCase;
import org.veo.core.usecase.asset.GetAssetUseCase;
import org.veo.core.usecase.asset.GetAssetsUseCase;
import org.veo.core.usecase.asset.UpdateAssetUseCase;
import org.veo.core.usecase.base.DeleteEntityUseCase;
import org.veo.core.usecase.base.ModifyEntityUseCase;
import org.veo.core.usecase.base.ModifyEntityUseCase.InputData;
import org.veo.rest.annotations.ParameterUuid;
import org.veo.rest.annotations.ParameterUuidParent;
import org.veo.rest.common.RestApiResponse;
import org.veo.rest.interactor.UseCaseInteractorImpl;
import org.veo.rest.security.ApplicationUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * REST service which provides methods to manage assets.
 */
@RestController
@RequestMapping(AssetController.URL_BASE_PATH)
public class AssetController extends AbstractEntityController {

    public AssetController(UseCaseInteractorImpl useCaseInteractor, GetAssetUseCase getAssetUseCase,
            GetAssetsUseCase getAssetsUseCase, CreateAssetUseCase createAssetUseCase,
            UpdateAssetUseCase updateAssetUseCase, DeleteEntityUseCase deleteEntityUseCase) {
        this.useCaseInteractor = useCaseInteractor;
        this.getAssetUseCase = getAssetUseCase;
        this.getAssetsUseCase = getAssetsUseCase;
        this.createAssetUseCase = createAssetUseCase;
        this.updateAssetUseCase = updateAssetUseCase;
        this.deleteEntityUseCase = deleteEntityUseCase;
    }

    public static final String URL_BASE_PATH = "/assets";

    private final UseCaseInteractorImpl useCaseInteractor;
    private final CreateAssetUseCase<ResponseEntity<ApiResponseBody>> createAssetUseCase;
    private final UpdateAssetUseCase<AssetDto> updateAssetUseCase;
    private final GetAssetUseCase<AssetDto> getAssetUseCase;
    private final GetAssetsUseCase<List<AssetDto>> getAssetsUseCase;
    private final DeleteEntityUseCase deleteEntityUseCase;

    @GetMapping
    @Operation(summary = "Loads all assets")
    public @Valid CompletableFuture<List<AssetDto>> getAssets(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuidParent @RequestParam(value = PARENT_PARAM,
                                               required = false) String parentUuid) {
        EntityToDtoContext tcontext = EntityToDtoContext.getCompleteTransformationContext();
        return useCaseInteractor.execute(getAssetsUseCase, new GetAssetsUseCase.InputData(
                getAuthenticatedClient(auth), Optional.ofNullable(parentUuid)), output -> {
                    return output.getEntities()
                                 .stream()
                                 .map(u -> AssetDto.from(u, tcontext))
                                 .collect(Collectors.toList());
                });
    }

    @GetMapping(value = "/{id}")
    @Operation(summary = "Loads an asset")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                         description = "Asset loaded",
                         content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = AssetDto.class))),
            @ApiResponse(responseCode = "404", description = "Asset not found") })
    public @Valid CompletableFuture<AssetDto> getAsset(
            @Parameter(required = false, hidden = true) Authentication auth,
            @PathVariable String id) {
        ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
        Client client = getClient(user.getClientId());
        return useCaseInteractor.execute(getAssetUseCase,
                                         new GetAssetUseCase.InputData(Key.uuidFrom(id), client),
                                         output -> {
                                             EntityToDtoContext tcontext = EntityToDtoContext.getCompleteTransformationContext();
                                             return AssetDto.from(output.getAsset(), tcontext);
                                         });
    }

    @PostMapping()
    @Operation(summary = "Creates an asset")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Asset created") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> createAsset(
            @Parameter(required = false, hidden = true) Authentication auth,
            @Valid @NotNull @RequestBody CreateAssetDto dto) {
        ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
        return useCaseInteractor.execute(createAssetUseCase,
                                         new Supplier<CreateAssetUseCase.InputData>() {

                                             @Override
                                             public org.veo.core.usecase.asset.CreateAssetUseCase.InputData get() {
                                                 Client client = getClient(user.getClientId());
                                                 DtoToEntityContext tcontext = configureDtoContext(client,
                                                                                                   dto.getReferences());
                                                 return new CreateAssetUseCase.InputData(
                                                         dto.toAsset(tcontext), client);
                                             }
                                         }, output -> {
                                             ApiResponseBody body = CreateAssetOutputMapper.map(output.getAsset());
                                             return RestApiResponse.created(URL_BASE_PATH, body);
                                         });
    }

    @PutMapping(value = "/{id}")
    @Operation(summary = "Updates an asset")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Asset updated"),
            @ApiResponse(responseCode = "404", description = "Asset not found") })
    public CompletableFuture<AssetDto> updateAsset(
            @Parameter(required = false, hidden = true) Authentication auth,
            @PathVariable String id, @Valid @NotNull @RequestBody AssetDto assetDto) {
        ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
        return useCaseInteractor.execute(updateAssetUseCase,
                                         new Supplier<ModifyEntityUseCase.InputData<Asset>>() {

                                             @Override
                                             public InputData<Asset> get() {
                                                 Client client = getClient(user.getClientId());
                                                 DtoToEntityContext tcontext = configureDtoContext(client,
                                                                                                   assetDto.getReferences());
                                                 return new ModifyEntityUseCase.InputData<Asset>(
                                                         assetDto.toAsset(tcontext), client);
                                             }
                                         }

                                         , output -> {
                                             return AssetDto.from(output.getEntity(),
                                                                  EntityToDtoContext.getCompleteTransformationContext());
                                         });
    }

    @DeleteMapping(value = "/{" + UUID_PARAM + ":" + UUID_REGEX + "}")
    @Operation(summary = "Deletes an asset")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Asset deleted"),
            @ApiResponse(responseCode = "404", description = "Asset not found") })
    public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteAsset(
            @Parameter(required = false, hidden = true) Authentication auth,
            @ParameterUuid @PathVariable(UUID_PARAM) String uuid) {
        ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
        Client client = getClient(user.getClientId());
        return useCaseInteractor.execute(deleteEntityUseCase,
                                         new DeleteEntityUseCase.InputData(Asset.class,
                                                 Key.uuidFrom(uuid), client),
                                         output -> ResponseEntity.ok()
                                                                 .build());
    }

}
