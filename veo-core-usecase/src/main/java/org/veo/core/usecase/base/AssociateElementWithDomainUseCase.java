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

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AssociateElementWithDomainUseCase
    implements TransactionalUseCase<
        AssociateElementWithDomainUseCase.InputData, AssociateElementWithDomainUseCase.OutputData> {
  private final GenericElementRepository elementRepository;
  private final DomainRepository domainRepository;

  @Override
  public boolean isReadOnly() {
    return false;
  }

  public OutputData execute(InputData input) {
    var domain = domainRepository.getById(input.domainId, input.authenticatedClient.getId());
    var element = fetchElement(input);
    element.checkSameClient(input.authenticatedClient); // client boundary safety net
    element.associateWithDomain(domain, input.subType, input.status);
    element.setUpdatedAt(Instant.now());
    DomainSensitiveElementValidator.validate(element);
    // re-fetch the element to make sure it is returned with updated versioning information and
    // transaction listeners are called
    return new OutputData(fetchElement(input), domain);
  }

  private Element fetchElement(InputData input) {
    return elementRepository.getById(input.elementId, input.elementType, input.authenticatedClient);
  }

  @Valid
  public record InputData(
      Client authenticatedClient,
      Class<? extends Element> elementType,
      UUID elementId,
      UUID domainId,
      String subType,
      String status)
      implements UseCase.InputData {}

  @Valid
  public record OutputData(@Valid Element element, @Valid Domain domain)
      implements UseCase.OutputData {}
}
