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

import java.util.List;
import java.util.concurrent.Future;

import jakarta.validation.Valid;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import org.veo.adapter.presenter.api.dto.SystemMessageDto;
import org.veo.core.entity.SystemMessage;
import org.veo.core.entity.specification.NotAllowedException;
import org.veo.core.usecase.message.GetAllSystemMessageUseCase;
import org.veo.core.usecase.message.GetSystemMessageUseCase;
import org.veo.rest.security.ApplicationUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(MessageController.URL_BASE_PATH)
@RequiredArgsConstructor
public class MessageController extends AbstractVeoController {
  public static final String URL_BASE_PATH = "/" + SystemMessage.PLURAL_TERM;

  private final GetSystemMessageUseCase getMessage;
  private final GetAllSystemMessageUseCase getAllMessages;

  @Value("${veo.api-keys.system-messages}")
  private final String systemMessagesApiKey;

  @GetMapping()
  @Operation(summary = "Loads system messages")
  @ApiResponse(
      responseCode = "200",
      description = "Messages loaded",
      content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
  @ApiResponse(responseCode = "404", description = "System messages not found")
  @ApiResponse(responseCode = "400", description = "Bad request")
  @SecurityRequirements({
    @SecurityRequirement(name = RestApplication.SECURITY_SCHEME_OAUTH),
    @SecurityRequirement(name = RestApplication.SECURITY_SCHEME_APIKEY)
  })
  public @Valid Future<List<SystemMessageDto>> getSystemMessages(
      WebRequest request, @Parameter(hidden = true) ApplicationUser applicationUser) {
    if (applicationUser == null) {
      String apiKey = request.getHeader(RestApplication.HEADER_NAME_APIKEY);
      if (!systemMessagesApiKey.equals(apiKey)) {
        throw new NotAllowedException("Invalid API key");
      }
    }
    return useCaseInteractor.execute(
        getAllMessages,
        new GetAllSystemMessageUseCase.InputData(),
        o -> o.messages().stream().map(SystemMessageDto::of).toList());
  }

  @GetMapping(value = "/{id}")
  @Operation(summary = "Loads a system message")
  @ApiResponse(
      responseCode = "200",
      description = "Message loaded",
      content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
  @ApiResponse(responseCode = "404", description = "System message not found")
  @ApiResponse(responseCode = "400", description = "Bad request")
  public @Valid Future<SystemMessageDto> getSystemMessage(
      @Nonnull @Parameter(required = true, description = "system message id") @PathVariable
          Long id) {
    return useCaseInteractor.execute(
        getMessage,
        new GetSystemMessageUseCase.InputData(id),
        o -> SystemMessageDto.of(o.message()));
  }
}
