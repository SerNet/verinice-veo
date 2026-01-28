/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2026  Jonas Jordan
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
package org.veo.core.usecase.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import jakarta.validation.Valid;

import com.github.zafarkhaja.semver.Version;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.UpdatableDomain;
import org.veo.core.entity.state.DomainBaseState;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetDomainUpdatesUseCase
    implements TransactionalUseCase<UseCase.EmptyInput, GetDomainUpdatesUseCase.OutputData> {
  private final DomainRepository domainRepository;
  private final DomainTemplateRepository domainTemplateRepository;

  @Override
  public OutputData execute(EmptyInput input, UserAccessRights userAccessRights) {
    List<Domain> domains =
        domainRepository.findAllActiveByClient(userAccessRights.getClientId()).stream().toList();
    var templates =
        domainTemplateRepository.findDomainTemplates(
            domains.stream().map(Nameable::getName).toList());
    var updates =
        domains.stream()
            .map(
                domain -> {
                  var domainVersion = Version.parse(domain.getTemplateVersion());
                  var allUpdates =
                      templates.stream()
                          .filter(t -> t.getName().equals(domain.getName()))
                          .filter(
                              t ->
                                  Version.parse(t.getTemplateVersion()).isHigherThan(domainVersion))
                          .sorted(Comparator.comparing(DomainBaseState::getTemplateVersion))
                          .toList();
                  var possibleUpdates =
                      allUpdates.stream()
                          .filter(
                              dt ->
                                  Version.parse(dt.getTemplateVersion()).majorVersion()
                                      <= domainVersion.majorVersion() + 1)
                          .toList();
                  if (!allUpdates.isEmpty()) {
                    return new UpdatableDomain(
                        domain,
                        allUpdates,
                        possibleUpdates,
                        allUpdates.getLast(),
                        !possibleUpdates.isEmpty() ? possibleUpdates.getLast() : null);
                  }
                  return null;
                })
            .filter(Objects::nonNull)
            .toList();
    return new OutputData(updates);
  }

  @Valid
  public record OutputData(@Valid List<UpdatableDomain> updatableDomains)
      implements UseCase.OutputData {}
}
