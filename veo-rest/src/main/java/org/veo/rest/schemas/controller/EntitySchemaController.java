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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Key;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.DomainRepository;
import org.veo.core.service.EntitySchemaService;
import org.veo.rest.schemas.resource.EntitySchemaResource;
import org.veo.rest.security.ApplicationUser;

import lombok.RequiredArgsConstructor;

/**
 * REST service which provides methods to query schemas for business entities. The schemas that are
 * delivered will be generated according to: - the entity, i.e. "Process" - the domain, i.e. "GDPR"
 * - the user's granted authorities (to be determined - currently all properties are returned
 * regardless of the user)
 */
@Component
@RequiredArgsConstructor
public class EntitySchemaController implements EntitySchemaResource {

  private final EntitySchemaService schemaService;
  private final DomainRepository domainRepository;

  @Override
  public Future<ResponseEntity<String>> getSchema(
      Authentication auth,
      @PathVariable String type,
      @RequestParam(value = "domains") List<String> domainIDs) {
    return CompletableFuture.supplyAsync(
        () -> {
          ApplicationUser user = ApplicationUser.authenticatedUser(auth.getPrincipal());
          List<String> userRoles =
              user.getAuthorities().stream()
                  .map(grant -> grant.getAuthority())
                  .collect(Collectors.toList());
          // TODO define schema-roles for users
          // TODO use valid 'domain' class

          var clientDomainsById =
              domainRepository.findAllByClient(Key.uuidFrom(user.getClientId())).stream()
                  .collect(Collectors.toMap(Domain::getIdAsString, Function.identity()));
          Set<Domain> domains = new HashSet<>(domainIDs.size());
          for (String domainId : domainIDs) {
            Domain domain = clientDomainsById.get(domainId);
            if (domain == null) {
              throw new NotFoundException("Domain %s not found", domainId);
            }
            domains.add(domain);
          }

          String schema =
              schemaService.roleFilter(userRoles, schemaService.findSchema(type, domains));
          return ResponseEntity.ok().body(schema);
        });
  }
}
