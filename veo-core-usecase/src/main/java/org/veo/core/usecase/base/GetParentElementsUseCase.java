/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2025  Aziz Khalledi
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

import org.veo.core.UserAccessRights;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.repository.ClientRepository;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.PagedResult;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.ParentElementQuery;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
public class GetParentElementsUseCase
    implements TransactionalUseCase<
        GetParentElementsUseCase.InputData, GetParentElementsUseCase.OutputData> {

  private final ClientRepository clientRepository;
  private final GenericElementRepository genericRepository;
  private final DomainRepository domainRepository;

  @Override
  public OutputData execute(InputData input, UserAccessRights userAccessRights) {
    var client = clientRepository.getById(userAccessRights.getClientId());
    var domain = domainRepository.getById(input.domainId, client.getId());

    var element =
        genericRepository.getById(input.elementId, input.elementType.getType(), userAccessRights);

    var pagedParents =
        genericRepository.queryParents(element, domain).execute(input.pagingConfiguration);

    return new OutputData(pagedParents);
  }

  @Valid
  @Value
  @Builder
  public static class InputData implements UseCase.InputData {
    UUID domainId;
    ElementType elementType;
    UUID elementId;
    PagingConfiguration<ParentElementQuery.SortCriterion> pagingConfiguration;
  }

  @Valid
  @Value
  public static class OutputData implements UseCase.OutputData {
    PagedResult<Element, ParentElementQuery.SortCriterion> parents;
  }
}
