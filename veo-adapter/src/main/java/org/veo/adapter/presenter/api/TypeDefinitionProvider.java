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
package org.veo.adapter.presenter.api;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.veo.adapter.presenter.api.common.ReferenceAssembler;
import org.veo.core.entity.EntityType;
import org.veo.core.usecase.UseCase;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
public class TypeDefinitionProvider {
  private final ReferenceAssembler referenceAssembler;

  public Map<String, TypeDefinition> getAll() {
    return EntityType.ELEMENT_TYPES.stream()
        .collect(Collectors.toMap(EntityType::getSingularTerm, this::buildDefinition));
  }

  private TypeDefinition buildDefinition(EntityType type) {
    return new TypeDefinition(
        referenceAssembler.resourcesReferenceOf(type.getType()),
        referenceAssembler.searchesReferenceOf(type.getType()),
        referenceAssembler.schemaReferenceOf(type.getSingularTerm()));
  }

  @Data
  @AllArgsConstructor
  public static class OutputData implements UseCase.OutputData {
    List<TypeDefinition> types;
  }
}
