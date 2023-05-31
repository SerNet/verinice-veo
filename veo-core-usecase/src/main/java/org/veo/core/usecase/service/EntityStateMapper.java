/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jochen Kemnade
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
package org.veo.core.usecase.service;

import java.util.Optional;

import org.veo.core.entity.Nameable;
import org.veo.core.entity.Unit;
import org.veo.core.entity.state.UnitState;

public final class EntityStateMapper {

  public void mapState(UnitState source, Unit target, IdRefResolver idRefResolver) {
    mapNameableProperties(source, target);

    target.setDomains(idRefResolver.resolve(source.getDomains()));
    target.setParent(
        Optional.ofNullable(source.getParent()).map(idRefResolver::resolve).orElse(null));
  }

  private static void mapNameableProperties(Nameable source, Nameable target) {
    target.setName(source.getName());
    target.setAbbreviation(source.getAbbreviation());
    target.setDescription(source.getDescription());
  }
}
