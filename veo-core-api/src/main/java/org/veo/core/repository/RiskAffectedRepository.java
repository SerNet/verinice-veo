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
package org.veo.core.repository;

import java.util.Set;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Control;
import org.veo.core.entity.Person;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scenario;

/**
 * A repository for <code>RiskAffected</code> entities like Asset and Process.
 *
 * <p>Implements basic CRUD operations from the superinterface and extends them with more specific
 * methods - i.e. queries based on particular fields.
 */
public interface RiskAffectedRepository<T extends RiskAffected<T, R>, R extends AbstractRisk<T, R>>
    extends ElementRepository<T> {
  /** Retrieves elements that have risks resulting from the given scenario. */
  Set<T> findByRisk(Scenario cause);

  /** Retrieves elements that have risks that are mitigated by the given control. */
  Set<T> findByRisk(Control mitigatedBy);

  /** Retrieves elements that have risks for which the given person is the risk owner. */
  Set<T> findByRisk(Person riskOwner);
}
