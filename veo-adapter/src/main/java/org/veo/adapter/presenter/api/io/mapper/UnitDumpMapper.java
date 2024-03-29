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
package org.veo.adapter.presenter.api.io.mapper;

import java.util.Set;
import java.util.stream.Collectors;

import org.veo.adapter.presenter.api.dto.AbstractRiskDto;
import org.veo.adapter.presenter.api.dto.UnitDumpDto;
import org.veo.adapter.presenter.api.dto.full.FullDomainDto;
import org.veo.adapter.presenter.api.response.transformer.EntityToDtoTransformer;
import org.veo.core.entity.Element;
import org.veo.core.entity.Key;
import org.veo.core.entity.RiskAffected;
import org.veo.core.usecase.unit.GetUnitDumpUseCase;

public class UnitDumpMapper {

  public static GetUnitDumpUseCase.InputData mapInput(String unitId) {
    return mapInput(unitId, null);
  }

  public static GetUnitDumpUseCase.InputData mapInput(String unitId, String domainId) {
    return new GetUnitDumpUseCase.InputData(Key.uuidFrom(unitId), Key.uuidFrom(domainId));
  }

  public static UnitDumpDto mapOutput(
      GetUnitDumpUseCase.OutputData useCaseOutput, EntityToDtoTransformer entityToDtoTransformer) {
    var elementDtos =
        useCaseOutput.getElements().stream()
            .map(entityToDtoTransformer::transform2Dto)
            .collect(Collectors.toSet());
    return new UnitDumpDto(
        entityToDtoTransformer.transformUnit2Dto(useCaseOutput.getUnit()),
        mapDomains(useCaseOutput, entityToDtoTransformer),
        elementDtos,
        getRisks(useCaseOutput.getElements(), entityToDtoTransformer));
  }

  private static Set<AbstractRiskDto> getRisks(
      Set<Element> elements, EntityToDtoTransformer transformer) {
    return elements.stream()
        .filter(RiskAffected.class::isInstance)
        .map(i -> (RiskAffected<?, ?>) i)
        .flatMap(a -> a.getRisks().stream())
        .map(transformer::transform2Dto)
        .collect(Collectors.toSet());
  }

  private static Set<FullDomainDto> mapDomains(
      GetUnitDumpUseCase.OutputData useCaseOutput, EntityToDtoTransformer entityToDtoTransformer) {
    return useCaseOutput.getUnit().getDomains().stream()
        .map(entityToDtoTransformer::transformDomain2Dto)
        .collect(Collectors.toSet());
  }
}
