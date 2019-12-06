/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.core.entity.process;

import java.util.Set;
import java.util.UUID;

import org.veo.core.entity.IRepository;
import org.veo.core.entity.Key;
import org.veo.core.entity.asset.Asset;

/**
 * A repository for <code>Process</code> entities.
 *
 */
public interface IProcessRepository extends IRepository<Process, Key<UUID>> {

    /**
     * Retrieve processes for which the given person is responsible.
     *
     * @param personId
     * @return
     */
    public Set<Process> getProcessByResponsiblePerson(Key<UUID> personId);

    /**
     * Find processes that hold a reference to the specified asset.
     *
     * @param asset
     * @return
     */
    public Set<Process> findProcessesContainingAsset(Asset asset);
}
