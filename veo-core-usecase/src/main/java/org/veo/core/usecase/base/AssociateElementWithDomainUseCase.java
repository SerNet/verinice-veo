/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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
package org.veo.core.usecase.base;

import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
public class AssociateElementWithDomainUseCase
    implements TransactionalUseCase<
        AssociateElementWithDomainUseCase.InputData, AssociateElementWithDomainUseCase.OutputData> {
  private final RepositoryProvider repositoryProvider;
  private final DomainRepository domainRepository;

  @Override
  public boolean isReadOnly() {
    return false;
  }

  public OutputData execute(InputData input) {
    var domain = domainRepository.getById(input.domainId, input.authenticatedClient.getId());
    var element =
        (Element)
            repositoryProvider
                .getElementRepositoryFor(input.elementType)
                .getById(input.elementId, input.authenticatedClient.getId());
    element.checkSameClient(input.authenticatedClient); // client boundary safety net
    element.associateWithDomain(domain, input.subType, input.status);
    DomainSensitiveElementValidator.validate(element);
    return new OutputData(element, domain);
  }

  @Valid
  @Getter
  @AllArgsConstructor
  public static class InputData implements UseCase.InputData {
    Client authenticatedClient;
    Class<? extends Element> elementType;
    Key<UUID> elementId;
    Key<UUID> domainId;
    String subType;
    String status;
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    @Valid Element element;
    @Valid Domain domain;
  }
}
