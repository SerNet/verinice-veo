/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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

import java.util.Optional;
import java.util.Set;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.specification.ClientBoundaryViolationException;
import org.veo.core.entity.specification.EntitySpecifications;

/**
 * A unit is high level group of elements defined by organizational structure. Units may contain
 * other units. For instance, a unit could be a division, a department or a project. Unit is a
 * component that defines ownership and primary responsibility. An organizational unit. Units may
 * have sub-units. Every element object is assigned to exactly one unit at all times. When the unit
 * is deleted, all its elements will be deleted as well. A unit defines object ownership. Small and
 * medium organizations may just have one unit. Large enterprises may have multiple units for
 * different subsidiaries. Service providers might have one unit for each client that is using the
 * software. A unit always belongs to exactly one client. This means that every element also
 * transitively belongs to exactly one client. Units cannot be moved between clients. The {@link
 * Scope} object is much more flexible and the preferred choice to group elements together for
 * business modeling purposes. Units should exclusively be used to model ownership and high-level
 * access restrictions.
 */
public interface Unit extends Versioned, Displayable, ClientOwned, Identifiable, Nameable {

  String SINGULAR_TERM = "unit";
  String PLURAL_TERM = "units";

  default Optional<Client> getOwningClient() {
    return Optional.of(getClient());
  }

  @NotNull
  Client getClient();

  void setClient(Client aClient);

  Unit getParent();

  void setParent(Unit aParent);

  /**
   * Add the given Domain to the collection of domains.
   *
   * @return true if added
   */
  boolean addToDomains(Domain aDomain);

  /**
   * Remove the given Domain from the collection domains.
   *
   * @return true if removed
   */
  boolean removeFromDomains(Domain aDomain);

  Set<Unit> getUnits();

  void setUnits(Set<Unit> units);

  Set<Domain> getDomains();

  void setDomains(Set<Domain> aDomains);

  /**
   * @throws ClientBoundaryViolationException if the passed client is not equal to the client in the
   *     unit
   */
  default void checkSameClient(Client client) {
    if (!(EntitySpecifications.hasSameClient(client).isSatisfiedBy(getClient()))) {
      throw new ClientBoundaryViolationException(this, client);
    }
  }

  @Override
  default Class<? extends Identifiable> getModelInterface() {
    return Unit.class;
  }

  default String getModelType() {
    return SINGULAR_TERM;
  }

  /**
   * Returns all domains to the collection of domains.
   *
   * @param domains The domains to add to this unit.
   * @return {@code true} if the domain collection changed as a result of this call
   */
  boolean addToDomains(@NotNull Set<Domain> domains);
}
