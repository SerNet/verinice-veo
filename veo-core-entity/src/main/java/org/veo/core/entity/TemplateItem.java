/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler
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
package org.veo.core.entity;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.exception.UnprocessableDataException;

public interface TemplateItem<T extends TemplateItem<T>> extends Nameable, Identifiable, Versioned {

  @Deprecated // TODO #2301 remove
  String getNamespace();

  @Deprecated // TODO #2301 remove
  void setNamespace(String aNamespace);

  @NotNull
  String getElementType();

  void setElementType(String aType);

  String getSubType();

  void setSubType(String subType);

  String getStatus();

  void setStatus(String status);

  Map<String, Map<String, Object>> getCustomAspects();

  void setCustomAspects(Map<String, Map<String, Object>> container);

  Domain requireDomainMembership();

  Element incarnate(Unit owner);

  /** All the tailoring references for this template item. */
  Set<TailoringReference<T>> getTailoringReferences();

  default Class<? extends Element> getElementInterface() {
    return (Class<? extends Element>) EntityType.getBySingularTerm(getElementType()).getType();
  }

  static void checkValidElementType(String type) {
    if (EntityType.ELEMENT_TYPES.stream().noneMatch(et -> et.getSingularTerm().equals(type))) {
      throw new UnprocessableDataException(
          "The given elementType '"
              + type
              + "' is not a valid template type. Valid types are: "
              + EntityType.ELEMENT_TYPES.stream()
                  .map(et -> et.getSingularTerm())
                  .collect(Collectors.joining(", ")));
    }
  }
}
