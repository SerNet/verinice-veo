/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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
package org.veo.core.entity.state;

import java.util.Locale;
import java.util.Map;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.definitions.ControlImplementationDefinition;
import org.veo.core.entity.definitions.CustomAspectDefinition;
import org.veo.core.entity.definitions.LinkDefinition;
import org.veo.core.entity.definitions.SubTypeDefinition;

public interface ElementTypeDefinitionState {

  @NotNull
  Map<String, SubTypeDefinition> getSubTypes();

  @NotNull
  Map<String, CustomAspectDefinition> getCustomAspects();

  @NotNull
  Map<String, LinkDefinition> getLinks();

  @NotNull
  Map<Locale, Map<String, String>> getTranslations();

  ControlImplementationDefinition getControlImplementationDefinition();
}
