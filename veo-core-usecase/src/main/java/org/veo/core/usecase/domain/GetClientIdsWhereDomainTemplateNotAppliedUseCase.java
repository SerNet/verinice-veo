/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade.
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

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.veo.core.entity.AccountProvider;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.specification.MissingAdminPrivilegesException;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.DomainTemplateRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class GetClientIdsWhereDomainTemplateNotAppliedUseCase
    implements TransactionalUseCase<
        GetClientIdsWhereDomainTemplateNotAppliedUseCase.InputData,
        GetClientIdsWhereDomainTemplateNotAppliedUseCase.OutputData> {

  private final AccountProvider accountProvider;
  private final ClientRepository clientRepository;
  private final DomainTemplateRepository domainTemplatetRepository;

  @Override
  public OutputData execute(InputData input) {
    if (!accountProvider.getCurrentUserAccount().isAdmin()) {
      throw new MissingAdminPrivilegesException();
    }
    if (input.restrictToClientsWithExistingDomain) {
      DomainTemplate domainTemplate =
          domainTemplatetRepository.findById(input.domainTemplateId).orElseThrow();
      return new OutputData(
          clientRepository
              .findAllActiveWhereDomainTemplateNotAppliedAndWithDomainTemplateOfName(
                  input.domainTemplateId, domainTemplate.getName())
              .stream()
              .map(Identifiable::getId)
              .collect(Collectors.toSet()));
    }

    return new OutputData(
        clientRepository.findAllActiveWhereDomainTemplateNotApplied(input.domainTemplateId).stream()
            .map(Identifiable::getId)
            .collect(Collectors.toSet()));
  }

  @Valid
  public record InputData(UUID domainTemplateId, boolean restrictToClientsWithExistingDomain)
      implements UseCase.InputData {}

  @Valid
  public record OutputData(@Valid Set<UUID> clientIds) implements UseCase.OutputData {}
}
