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
import java.util.UUID;

import org.veo.core.entity.Process;
import org.veo.core.entity.ProcessRisk;
import org.veo.core.entity.Scenario;

/**
 * A repository for <code>Process</code> entities.
 *
 * <p>Implements basic CRUD operations from the superinterface and extends them with more specific
 * methods - i.e. queries based on particular fields.
 */
public interface ProcessRepository
    extends RiskAffectedRepository<Process, ProcessRisk>, CompositeElementRepository<Process> {

  /** Returns risks with initialized risk value aspects. */
  Set<Process> findRisksWithValue(Scenario scenario);

  Set<Process> findWithRisksAndScenarios(Set<UUID> ids);
}
