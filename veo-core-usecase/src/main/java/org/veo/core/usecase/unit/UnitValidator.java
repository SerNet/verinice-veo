/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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
package org.veo.core.usecase.unit;

import java.util.Set;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Unit;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.state.UnitState;
import org.veo.core.repository.GenericElementRepository;
import org.veo.core.repository.PagingConfiguration;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UnitValidator {
  private final GenericElementRepository genericElementRepository;

  /** Validates whether given stored unit may be replaced with given changed unit. */
  public void validateUpdate(UnitState changedunit, Unit storedUnit) {
    storedUnit.getDomains().stream()
        .filter(
            d ->
                changedunit.getDomains().stream()
                    .noneMatch(idAndType -> idAndType.getId().equals(d.getId())))
        .toList()
        .forEach(removedDomain -> validateDomainRemoval(storedUnit, removedDomain));
  }

  // TODO VEO-2136 consider removing the elements from the domain instead
  private void validateDomainRemoval(Unit storedUnit, Domain removedDomain) {
    var query = genericElementRepository.query(storedUnit.getClient());
    query.whereUnitIn(Set.of(storedUnit));
    query.whereDomainsContain(removedDomain);
    var associatedElementsPage =
        query.execute(
            new PagingConfiguration<>(10, 0, "name", PagingConfiguration.SortOrder.ASCENDING));
    if (associatedElementsPage.totalResults() > 0) {
      throw new UnprocessableDataException(
          "Cannot remove domain %s from unit. %s element(s) in the unit are associated with it, including: %s"
              .formatted(
                  removedDomain.getIdAsString(),
                  associatedElementsPage.totalResults(),
                  String.join(
                      ",",
                      associatedElementsPage.resultPage().stream()
                          .map(el -> "%s %s".formatted(el.getModelType(), el.getIdAsString()))
                          .toList())));
    }
  }
}
