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
package org.veo.core.entity.definitions;

import java.util.Map;
import java.util.Optional;

public interface ElementTypeDefinition {
  String getElementType();

  Map<String, SubTypeDefinition> getSubTypes();

  void setSubTypes(Map<String, SubTypeDefinition> definitions);

  Map<String, CustomAspectDefinition> getCustomAspects();

  void setCustomAspects(Map<String, CustomAspectDefinition> definitions);

  Map<String, LinkDefinition> getLinks();

  default Optional<LinkDefinition> findLink(String type) {
    return Optional.ofNullable(getLinks().get(type));
  }

  void setLinks(Map<String, LinkDefinition> definitions);

  Map<String, Map<String, String>> getTranslations();

  void setTranslations(Map<String, Map<String, String>> translations);
}
