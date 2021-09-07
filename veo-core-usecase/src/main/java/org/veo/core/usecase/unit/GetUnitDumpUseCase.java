/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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
package org.veo.core.usecase.unit;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.veo.core.entity.Account;
import org.veo.core.entity.Element;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.specification.MissingAdminPrivilegesException;
import org.veo.core.repository.PagingConfiguration;
import org.veo.core.repository.RepositoryProvider;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetUnitDumpUseCase implements
        TransactionalUseCase<GetUnitDumpUseCase.InputData, GetUnitDumpUseCase.OutputData> {
    private final RepositoryProvider repositoryProvider;
    private final UnitRepository unitRepository;

    @Override
    public OutputData execute(InputData input) {
        if (!input.account.isAdmin()) {
            throw new MissingAdminPrivilegesException();
        }
        var unit = unitRepository.findById(input.unitId)
                                 .orElseThrow(() -> new NotFoundException(
                                         String.format("Unit %s does not exist.", input.unitId)));
        return new OutputData(unit, getElements(unit));
    }

    private Set<Element> getElements(Unit unit) {
        return EntityType.ELEMENT_TYPE_CLASSES.stream()
                                              .map(repositoryProvider::getElementRepositoryFor)
                                              .map(repo -> repo.query(unit.getClient()))
                                              .map(query -> query.whereUnitIn(Set.of(unit)))
                                              .flatMap(query -> query.execute(PagingConfiguration.UNPAGED)
                                                                     .getResultPage()
                                                                     .stream())
                                              .collect(Collectors.toSet());
    }

    @Data
    @AllArgsConstructor
    public static class InputData implements UseCase.InputData {
        private Account account;
        private Key<UUID> unitId;
    }

    @Data
    @AllArgsConstructor
    public static class OutputData implements UseCase.OutputData {
        private Unit unit;
        private Set<Element> elements;
    }
}
