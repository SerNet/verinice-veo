/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler
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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.core.entity.Client;
import org.veo.core.entity.UserConfiguration;
import org.veo.core.usecase.userconfiguration.DeleteUserConfigurationUseCase;
import org.veo.core.usecase.userconfiguration.GetAllUserConfigurationKeysUseCase;
import org.veo.core.usecase.userconfiguration.GetUserConfigurationUseCase;
import org.veo.core.usecase.userconfiguration.SaveUserConfigurationUseCase;
import org.veo.rest.common.RestApiResponse;
import org.veo.rest.security.ApplicationUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(UserConfigurationController.URL_BASE_PATH)
@RequiredArgsConstructor
public class UserConfigurationController extends AbstractVeoController {

  public static final String URL_BASE_PATH = "/" + UserConfiguration.PLURAL_TERM;
  private final GetAllUserConfigurationKeysUseCase getAllUserConfigurationKeysUseCase;
  private final GetUserConfigurationUseCase getUserConfigurationUseCase;
  private final SaveUserConfigurationUseCase saveUserConfigurationUseCase;
  private final DeleteUserConfigurationUseCase deleteUserConfigurationUseCase;

  @GetMapping()
  @Operation(summary = "Loads the user configuration keys")
  @ApiResponse(
      responseCode = "200",
      description = "Configuration template loaded",
      content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
  @ApiResponse(responseCode = "404", description = "User configuration not found")
  @ApiResponse(responseCode = "400", description = "Bad request")
  public @Valid Future<Set<String>> getUserConfiguration(
      @Parameter(required = true, hidden = true) ApplicationUser applicationUser) {
    Client authenticatedClient = getClient(applicationUser);
    return useCaseInteractor.execute(
        getAllUserConfigurationKeysUseCase,
        new GetAllUserConfigurationKeysUseCase.InputData(
            authenticatedClient.getId(), applicationUser.getUsername()),
        GetAllUserConfigurationKeysUseCase.OutputData::keys);
  }

  @GetMapping(value = "/{appId}")
  @Operation(summary = "Loads the user configuration")
  @ApiResponse(
      responseCode = "200",
      description = "Configuration template loaded",
      content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
  @ApiResponse(responseCode = "404", description = "User configuration not found")
  @ApiResponse(responseCode = "400", description = "Bad request")
  public @Valid Future<Map<String, Object>> getUserConfiguration(
      @Parameter(required = true, hidden = true) ApplicationUser applicationUser,
      @Parameter(required = true, description = "Id of the api-client") @PathVariable
          String appId) {
    Client authenticatedClient = getClient(applicationUser);
    return useCaseInteractor.execute(
        getUserConfigurationUseCase,
        new GetUserConfigurationUseCase.InputData(
            authenticatedClient.getId(), applicationUser.getUsername(), appId),
        GetUserConfigurationUseCase.OutputData::configuration);
  }

  @PutMapping("/{appId}")
  @Operation(summary = "creates or updates a user configuration")
  @ApiResponse(responseCode = "200", description = "Configuration updated")
  @ApiResponse(responseCode = "201", description = "Configuration created")
  @ApiResponse(responseCode = "413", description = "Exceeds the configuration size limit.")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> updateUserConfiguration(
      @Parameter(required = true, hidden = true) ApplicationUser applicationUser,
      @Parameter(required = true, description = "Id of the api-client") @PathVariable String appId,
      @Valid @RequestBody Map<String, Object> userConfiguration) {
    Client authenticatedClient = getClient(applicationUser);
    return useCaseInteractor.execute(
        saveUserConfigurationUseCase,
        new SaveUserConfigurationUseCase.InputData(
            authenticatedClient, applicationUser.getUsername(), appId, userConfiguration),
        output ->
            output.created()
                ? RestApiResponse.created(output.applicationId(), "configuration created")
                : RestApiResponse.ok("configuration updated"));
  }

  @DeleteMapping("/{appId}")
  @Operation(summary = "Deletes a user configuration")
  @ApiResponse(responseCode = "204", description = "Configuration deleted")
  @ApiResponse(responseCode = "404", description = "Configuration not found")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteUserConfiguration(
      @Parameter(required = true, hidden = true) ApplicationUser applicationUser,
      @Parameter(required = true, description = "Id of the api-client") @PathVariable
          String appId) {
    Client authenticatedClient = getClient(applicationUser);
    return useCaseInteractor.execute(
        deleteUserConfigurationUseCase,
        new DeleteUserConfigurationUseCase.InputData(
            authenticatedClient.getId(), applicationUser.getUsername(), appId),
        output -> ResponseEntity.noContent().build());
  }
}
