/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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
package org.veo.core.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.veo.core.Translations;
import org.veo.core.entity.Domain;
import org.veo.core.entity.ElementType;

/**
 * Builds and returns an entity JSON-schema that matches the requestes entity type and domain(s).
 */
public interface EntitySchemaService {

  /**
   * Build and return an element DTO schema JSON as a simple string.
   *
   * @param type the entity type, i.e. 'process'
   * @param domains the domains to use, i.e. 'GDPR', 'ISO_27001'
   * @return a JSON schema document dynamically generated for the above parameters
   */
  @Deprecated
  String getSchema(ElementType type, Set<Domain> domains);

  /** Build domain-specific element DTO schema */
  String getSchema(ElementType elementType, Domain domain);

  /**
   * Returns a translations for the given language identifiers. If no translation is present for any
   * identifier, it will be ignored.
   */
  Translations findTranslations(Set<Domain> domains, Set<Locale> languages);

  /**
   * Filter the inputSchema to remove attributes that do not match the given user roles
   *
   * @param roles the role identifiers that the user is allowed to see
   * @param inputSchema the input schema from which certain attributes will be removed
   * @return the reduced JSON-schema document
   */
  String roleFilter(List<String> roles, String inputSchema);
}
