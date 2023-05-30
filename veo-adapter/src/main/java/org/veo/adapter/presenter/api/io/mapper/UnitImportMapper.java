/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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
package org.veo.adapter.presenter.api.io.mapper;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import org.veo.adapter.IdRefResolvingFactory;
import org.veo.adapter.presenter.api.dto.UnitDumpDto;
import org.veo.adapter.presenter.api.response.transformer.DomainAssociationTransformer;
import org.veo.adapter.presenter.api.response.transformer.DtoToEntityTransformer;
import org.veo.core.entity.Client;
import org.veo.core.entity.Domain;
import org.veo.core.entity.transform.EntityFactory;
import org.veo.core.entity.transform.IdentifiableFactory;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.usecase.service.DbIdRefResolver;
import org.veo.core.usecase.unit.UnitImportUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UnitImportMapper {
  private final IdentifiableFactory identifiableFactory;
  private final RepositoryProvider repositoryProvider;
  private final DomainAssociationTransformer domainAssociationTransformer;
  private final EntityFactory entityFactory;

  public UnitImportUseCase.InputData mapInput(UnitDumpDto dto, Client client) {
    // VEO-839 use new mapping style and handle resolving biz in use case
    var resolvingFactory = createResolvingFactory(dto, client);
    var transformer =
        new DtoToEntityTransformer(entityFactory, resolvingFactory, domainAssociationTransformer);
    var unit = transformer.transformDto2Unit(dto.getUnit(), resolvingFactory);
    unit.setClient(client);
    var elements =
        dto.getElements().stream()
            .map(elementDto -> transformer.transformDto2Element(elementDto, resolvingFactory))
            .collect(Collectors.toSet());
    dto.getRisks()
        .forEach(
            r -> {
              transformer.transformDto2Risk(r, resolvingFactory);
            });
    return new UnitImportUseCase.InputData(client, unit, elements);
  }

  private IdRefResolvingFactory createResolvingFactory(UnitDumpDto dto, Client client) {
    var resolvingFactory = new IdRefResolvingFactory(identifiableFactory);
    // Resolve domains using the DB (all other references must be resolved locally).
    var dbResolver = new DbIdRefResolver(repositoryProvider, client);
    dto.getDomains().stream()
        .map(d -> dbResolver.resolve(d.getId(), Domain.class))
        .forEach(resolvingFactory::register);

    return resolvingFactory;
  }
}
