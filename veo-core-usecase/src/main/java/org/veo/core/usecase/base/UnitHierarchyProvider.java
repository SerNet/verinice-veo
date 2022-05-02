/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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
package org.veo.core.usecase.base;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.veo.core.entity.Key;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.repository.UnitRepository;

/** Provides data from within unit hierarchies. */
public class UnitHierarchyProvider {
  private final UnitRepository unitRepository;

  public UnitHierarchyProvider(UnitRepository unitRepository) {
    this.unitRepository = unitRepository;
  }

  /**
   * Recursively finds all descendant units of given unit.
   *
   * @param rootUnitId ID of the unit whose descendants should be found.
   * @return A flat collection containing the root unit and all its descendants.
   */
  public Set<Unit> findAllInRoot(Key<UUID> rootUnitId) {
    Unit root =
        unitRepository
            .findById(rootUnitId)
            .orElseThrow(() -> new NotFoundException("No Unit found with ID %s", rootUnitId));
    var units = new HashSet<Unit>();
    addUnitHierarchyToSet(root, units);
    return units;
  }

  private void addUnitHierarchyToSet(Unit root, Set<Unit> target) {
    target.add(root);
    unitRepository.findByParent(root).forEach(child -> addUnitHierarchyToSet(child, target));
  }
}
