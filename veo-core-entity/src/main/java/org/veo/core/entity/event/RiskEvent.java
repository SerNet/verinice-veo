/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Alexander Koderman
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
package org.veo.core.entity.event;

import java.util.Set;

public interface RiskEvent extends DomainEvent {

  /** The kind of values that were changed. */
  enum ChangedValues {
    /** Values on the probability provider were changed. */
    PROBABILITY_VALUES_CHANGED,

    /** Values on the (categorized) impact provider were changed. */
    IMPACT_VALUES_CHANGED,

    /** Values on the (categorized) risk values aspect were changed. */
    RISK_VALUES_CHANGED,

    RISK_CREATED,

    RISK_DELETED
  }

  /** Returns the kind of values that were affected. */
  Set<ChangedValues> getChanges();
}
