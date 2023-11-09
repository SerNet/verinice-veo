/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jochen Kemnade.
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.repository.ClientRepository;
import org.veo.core.usecase.UseCaseInteractor;
import org.veo.rest.common.ClientNotActiveException;
import org.veo.rest.security.ApplicationUser;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractVeoController {

  @Autowired protected UseCaseInteractor useCaseInteractor;
  @Autowired protected ReferenceAssembler referenceAssembler;
  @Autowired protected ClientRepository clientRepository;
  @Autowired protected ObjectMapper jsonObjectMapper;
  @Autowired protected Validator validator;

  protected AbstractVeoController() {}

  @SuppressFBWarnings(
      value = "DM_DEFAULT_ENCODING",
      justification = "Charset UTF 8 is used when decoding the byte array")
  protected <T> T parse(MultipartFile file, Class<T> type) {
    try {
      String content = new String(file.getBytes(), StandardCharsets.UTF_8);
      T dto = jsonObjectMapper.readValue(content, type);
      validateDto(dto);
      return dto;
    } catch (JsonMappingException jsonMappingException) {
      throw new IllegalArgumentException(
          "Could not parse content of multipart file.", jsonMappingException);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not read multipart file.", e);
    }
  }

  private <T> void validateDto(T dto) {
    Set<ConstraintViolation<T>> violations = validator.validate(dto);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }

  protected Client getClient(String clientId) {
    Key<UUID> id = Key.uuidFrom(clientId);
    return clientRepository
        .findActiveById(id)
        .orElseThrow(() -> new ClientNotActiveException(clientId));
  }

  protected Client getAuthenticatedClient(Authentication auth) {
    ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
    return getClient(user);
  }

  protected Client getClient(ApplicationUser user) {
    return getClient(user.getClientId());
  }
}
