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
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import org.veo.adapter.presenter.api.dto.TranslationsDto;
import org.veo.core.Translations;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.ElementType;
import org.veo.core.repository.ClientReadOnlyRepository;
import org.veo.core.service.EntitySchemaService;
import org.veo.rest.VeoMessage;
import org.veo.rest.common.ClientNotActiveException;
import org.veo.rest.schemas.resource.TranslationsResource;
import org.veo.rest.security.ApplicationUser;

import lombok.RequiredArgsConstructor;

/** REST service which provides methods to UI translations in JSON format. */
@Component
@RequiredArgsConstructor
public class TranslationController implements TranslationsResource {

  private final EntitySchemaService schemaService;

  private final ClientReadOnlyRepository clientRepository;

  private final MessageSource messageSource;

  @Override
  public CompletableFuture<ResponseEntity<TranslationsDto>> getSchema(
      Authentication auth, Set<String> languages, String domainId) {
    ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
    Client client = getClient(user.getClientId());
    var locales = languages.stream().map(Locale::forLanguageTag).collect(Collectors.toSet());

    return CompletableFuture.supplyAsync(
        () -> {
          Set<Domain> domains = client.getDomains();
          if (domainId != null) {
            domains =
                domains.stream()
                    .filter(it -> it.getIdAsString().equals(domainId))
                    .collect(Collectors.toSet());
            if (domains.isEmpty()) {
              return ResponseEntity.notFound().build();
            }
          }
          Translations t10n = schemaService.findTranslations(domains, locales);
          locales.forEach(
              loc -> {
                for (VeoMessage veoMessage : VeoMessage.values()) {
                  t10n.add(
                      loc,
                      veoMessage.getMessageKey(),
                      messageSource.getMessage(veoMessage.getMessageKey(), null, loc));
                }
                // Add alternative element type plural keys for convenience
                for (ElementType type : ElementType.values()) {
                  t10n.add(
                      loc,
                      type.getSingularTerm() + "_plural",
                      messageSource.getMessage(type.getPluralTerm(), null, loc));
                }
              });
          return ResponseEntity.ok().body((TranslationsDto) t10n);
        });
  }

  protected Client getClient(UUID clientId) {
    return clientRepository
        .findByIdFetchTranslations(clientId)
        .orElseThrow(() -> new ClientNotActiveException(clientId.toString()));
  }
}
