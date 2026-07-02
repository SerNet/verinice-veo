/*
 * verinice.veo
 * Copyright (C) 2026  Aziz Khalledi.
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
 */
package org.veo.core.usecase.domain;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;

import org.veo.core.UserAccessRights;
import org.veo.core.entity.CustomAspect;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Unit;
import org.veo.core.entity.definitions.CustomAspectDefinition;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.definitions.attribute.AttributeDefinition;
import org.veo.core.repository.DomainRepository;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.UnitRepository;
import org.veo.core.usecase.TransactionalUseCase;
import org.veo.core.usecase.UseCase;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetAttributeValuesUseCase
    implements TransactionalUseCase<
        GetAttributeValuesUseCase.InputData, GetAttributeValuesUseCase.OutputData> {

  private final DomainRepository domainRepository;
  private final UnitRepository unitRepository;
  private final GenericElementRepository genericElementRepository;
  private final int maxResults;

  @Override
  public OutputData execute(InputData input, UserAccessRights userAccessRights) {
    String wantedType = input.attributeType;
    if (!AttributeDefinition.getValidTypes().contains(wantedType)) {
      throw new IllegalArgumentException("Unknown attribute type '%s'".formatted(wantedType));
    }

    Domain domain =
        domainRepository.getActiveByIdWithElementTypeDefinitionsAndRiskDefinitions(
            input.domainId, userAccessRights.getClientId());
    Unit unit = unitRepository.getById(input.unitId, userAccessRights);

    Map<String, Set<String>> caTypeToKeys = new HashMap<>();
    Map<String, Set<String>> linkTypeToKeys = new HashMap<>();
    for (ElementTypeDefinition etd : domain.getElementTypeDefinitions()) {
      collectKeysByType(etd.getCustomAspects(), wantedType, caTypeToKeys);
      collectKeysByType(etd.getLinks(), wantedType, linkTypeToKeys);
    }

    Set<Object> values = new HashSet<>();
    for (CustomAspect ca :
        genericElementRepository.findCustomAspects(
            unit.getId(), domain.getId(), caTypeToKeys.keySet())) {
      collectValues(values, caTypeToKeys.get(ca.getType()), ca.getAttributes());
    }
    for (CustomLink link :
        genericElementRepository.findCustomLinks(
            unit.getId(), domain.getId(), linkTypeToKeys.keySet())) {
      collectValues(values, linkTypeToKeys.get(link.getType()), link.getAttributes());
    }

    // Sort is by the values string representation (descending).
    List<Object> sorted =
        values.stream().sorted(Comparator.comparing(String::valueOf).reversed()).toList();
    boolean truncated = sorted.size() > maxResults;
    if (truncated) {
      sorted = sorted.subList(0, maxResults);
    }
    return new OutputData(sorted, truncated);
  }

  private static void collectKeysByType(
      Map<String, ? extends CustomAspectDefinition> definitions,
      String wantedType,
      Map<String, Set<String>> typeToKeys) {
    definitions.forEach(
        (type, def) ->
            def.getAttributeDefinitions()
                .forEach(
                    (key, attrDef) -> {
                      if (wantedType.equals(attrDef.getType())) {
                        typeToKeys.computeIfAbsent(type, k -> new HashSet<>()).add(key);
                      }
                    }));
  }

  private static void collectValues(
      Set<Object> values, Set<String> keys, Map<String, Object> attributes) {
    if (keys == null) {
      return;
    }
    for (String key : keys) {
      Object value = attributes.get(key);
      if (value != null) {
        values.add(value);
      }
    }
  }

  @Valid
  public record InputData(UUID domainId, UUID unitId, String attributeType)
      implements UseCase.InputData {}

  @Valid
  public record OutputData(List<Object> values, boolean truncated) implements UseCase.OutputData {}
}
