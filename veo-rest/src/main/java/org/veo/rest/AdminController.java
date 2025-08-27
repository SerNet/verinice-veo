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

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import org.veo.adapter.presenter.api.common.ApiResponseBody;
import org.veo.adapter.presenter.api.dto.SystemMessageDto;
import org.veo.adapter.presenter.api.dto.UnitDumpDto;
import org.veo.adapter.presenter.api.io.mapper.UnitDumpMapper;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.core.entity.specification.NotAllowedException;
import org.veo.core.usecase.UseCase;
import org.veo.core.usecase.UseCaseInteractor;
import org.veo.core.usecase.client.DeleteClientUseCase;
import org.veo.core.usecase.domain.UpdateAllClientDomainsUseCase;
import org.veo.core.usecase.message.DeleteSystemMessageUseCase;
import org.veo.core.usecase.message.SaveSystemMessageUseCase;
import org.veo.core.usecase.unit.GetUnitCountUseCase;
import org.veo.core.usecase.unit.GetUnitDumpUseCase;
import org.veo.rest.common.RestApiResponse;
import org.veo.rest.security.ApplicationUser;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(AdminController.URL_BASE_PATH)
@SecurityScheme(
    name = AdminController.SECURITY_SCHEME_APIKEY,
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.HEADER,
    description = "API key authentication",
    paramName = AdminController.HEADER_NAME_APIKEY)
@RequiredArgsConstructor
public class AdminController {
  private final UseCaseInteractor useCaseInteractor;
  private final DeleteClientUseCase deleteClientUseCase;
  private final GetUnitDumpUseCase getUnitDumpUseCase;
  private final UpdateAllClientDomainsUseCase updateAllClientDomainsUseCase;
  private final EntityToDtoTransformer entityToDtoTransformer;
  private final AsyncTaskExecutor taskExecutor;
  private final SaveSystemMessageUseCase saveSystemMessageUseCase;
  private final DeleteSystemMessageUseCase deleteSystemMessageUseCase;
  private final GetUnitCountUseCase getUnitCountUseCase;

  public static final String SECURITY_SCHEME_APIKEY = "ApiKeyAuth";
  public static final String HEADER_NAME_APIKEY = "X-API-KEY";

  public static final String URL_BASE_PATH = "/admin";

  @Value("${veo.api-keys.unit-count}")
  private final String unitCountApiKey;

  @PostMapping("/messages")
  @Operation(summary = "Create a message.")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> saveSystemMessage(
      @RequestBody @Valid @NotNull SystemMessageDto systemMessage) {
    return useCaseInteractor.execute(
        saveSystemMessageUseCase,
        new SaveSystemMessageUseCase.InputData(null, systemMessage),
        out ->
            RestApiResponse.created(
                "/messages",
                new ApiResponseBody(
                    true,
                    Optional.of(out.id().toString()),
                    "SystemMessage created successfully.")));
  }

  @PutMapping("/messages/{messageId}")
  @Operation(summary = "Update a message.")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> saveSystemMessage(
      @PathVariable long messageId, @RequestBody @Valid @NotNull SystemMessageDto systemMessage) {
    return useCaseInteractor.execute(
        saveSystemMessageUseCase,
        new SaveSystemMessageUseCase.InputData(messageId, systemMessage),
        out -> RestApiResponse.ok("SystemMessage updated."));
  }

  @DeleteMapping("/messages/{messageId}")
  @Operation(summary = "Deletes the message with the id.)")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteSystemMessage(
      @PathVariable long messageId) {
    return useCaseInteractor.execute(
        deleteSystemMessageUseCase,
        new DeleteSystemMessageUseCase.InputData(messageId),
        out -> ResponseEntity.noContent().build());
  }

  @DeleteMapping("/client/{clientId}")
  @Operation(
      summary =
          "Deletes client and all associated data (including units, domains, elements & risks)")
  public CompletableFuture<ResponseEntity<ApiResponseBody>> deleteClient(
      @PathVariable String clientId) {
    return useCaseInteractor.execute(
        deleteClientUseCase,
        new DeleteClientUseCase.InputData(UUID.fromString(clientId)),
        out -> ResponseEntity.noContent().build());
  }

  @GetMapping("/unit-dump/{unitId}")
  @Operation(summary = "Exports given unit, including unit metadata, domains, elements & risks")
  public CompletableFuture<UnitDumpDto> getUnitDump(@PathVariable UUID unitId) {
    return useCaseInteractor.execute(
        getUnitDumpUseCase,
        new GetUnitDumpUseCase.InputData(unitId, null),
        out -> UnitDumpMapper.mapOutput(out, entityToDtoTransformer));
  }

  @PostMapping("domain-templates/{id}/allclientsupdate")
  @Operation(
      summary = "Migrates all clients to the domain created from given domain template",
      description =
          "Runs as a background task. For each client, elements associated with a previous version of the domain are migrated to the given version and the old domain is deactivated.")
  @SuppressFBWarnings({"CRLF_INJECTION_LOGS"})
  public CompletableFuture<ResponseEntity<ApiResponseBody>> updateAllClientDomains(
      @Parameter(hidden = true) ApplicationUser user, @PathVariable UUID id) {
    log.info("Submit updateAllClientDomainsUseCase task for domainTemplate: {}", id);
    taskExecutor.execute(
        // TODO: VEO-1397 wrap this lambda to job/task, maybe submit the
        // task as event
        () -> {
          log.info("Start of updateAllClientDomainsUseCase task");
          updateAllClientDomainsUseCase.executeAndTransformResult(
              new UpdateAllClientDomainsUseCase.InputData(id), out -> null, user);
          log.info("end of updateAllClientDomainsUseCase task");
        });
    return CompletableFuture.completedFuture(ResponseEntity.noContent().build());
  }

  @GetMapping("/unit-count")
  @Operation(summary = "Returns the overall number of units")
  @SecurityRequirements(@SecurityRequirement(name = SECURITY_SCHEME_APIKEY))
  public CompletableFuture<Long> getUnitCount(WebRequest request) {
    String apiKey = request.getHeader(HEADER_NAME_APIKEY);
    if (!unitCountApiKey.equals(apiKey)) {
      throw new NotAllowedException("Invalid API key");
    }
    return useCaseInteractor.execute(
        getUnitCountUseCase,
        UseCase.EmptyInput.INSTANCE,
        GetUnitCountUseCase.OutputData::numberOfUnits);
  }
}
