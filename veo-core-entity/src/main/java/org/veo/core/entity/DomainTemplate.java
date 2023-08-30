/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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

/**
 * DomainTemplate The domaintemplare are managed by the system itself. The uuid is a named uui
 * generated as following: https://v.de/veo/domain-templates/DOMAIN-NAME/VERSION DOMAIN-NAME:
 * authority-name VERSION: version
 */
public interface DomainTemplate extends DomainBase {
  String SINGULAR_TERM = "domain-template";
  String PLURAL_TERM = "domain-templates";

  @Override
  default Class<? extends Identifiable> getModelInterface() {
    return DomainTemplate.class;
  }

  @Override
  default String getModelType() {
    return SINGULAR_TERM;
  }
}
