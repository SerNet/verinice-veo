/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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
package org.veo.rest.schemas.controller;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

import org.veo.adapter.presenter.api.dto.TranslationsDto;
import org.veo.core.Translations;
import org.veo.core.entity.Client;
import org.veo.core.entity.Key;
import org.veo.core.repository.ClientRepository;
import org.veo.core.service.EntitySchemaService;
import org.veo.rest.VeoMessage;
import org.veo.rest.schemas.resource.TranslationsResource;
import org.veo.rest.security.ApplicationUser;

import lombok.RequiredArgsConstructor;

/** REST service which provides methods to UI translations in JSON format. */
@Component
@RequiredArgsConstructor
public class TranslationController implements TranslationsResource {

  private final EntitySchemaService schemaService;

  private final ClientRepository clientRepository;

  private final MessageSource messageSource;

  @Override
  public CompletableFuture<ResponseEntity<TranslationsDto>> getSchema(
      Authentication auth, @RequestParam(value = "languages") Set<String> languages) {
    ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
    Client client = getClient(user.getClientId());
    return CompletableFuture.supplyAsync(
        () -> {
          Translations t10n = schemaService.findTranslations(client, languages);
          // TODO VEO-526 evaluate languages parameter
          Set.of("de", "en")
              .forEach(
                  lang -> {
                    for (VeoMessage veoMessage : VeoMessage.values()) {
                      t10n.add(
                          lang,
                          veoMessage.getMessageKey(),
                          messageSource.getMessage(
                              veoMessage.getMessageKey(), null, Locale.forLanguageTag(lang)));
                    }
                  });
          return ResponseEntity.ok().body((TranslationsDto) t10n);
        });
  }

  protected Client getClient(String clientId) {
    Key<UUID> id = Key.uuidFrom(clientId);
    return clientRepository.findByIdFetchTranslations(id).orElseThrow();
  }
}
