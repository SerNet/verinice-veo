/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jochen Kemnade.
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
package org.veo.core

import javax.persistence.SequenceGenerator

import org.veo.persistence.entity.jpa.StoredEventData

import net.ttddyy.dsproxy.QueryCount
import net.ttddyy.dsproxy.QueryCountHolder

abstract class AbstractPerformaceITSpec extends VeoSpringSpec {

    /**
     * to create a predictable number of select statements, we need to make sure
     * that the number of queries to the seq_events sequence is always the same.
     * Therefore, we insert dummy events until the highest ID is a multiple of the
     * allocationSize of the @SequenceGenerator.
     *
     * @see {@link org.veo.persistence.entity.jpa.StoredEventData#id}
     * @see {@link javax.persistence.SequenceGenerator#allocationSize()}
     */
    def setup() {
        executeInTransaction {
            long highestId = eventStoreDataRepository.save(new StoredEventData()).id
            int allocationSize = StoredEventData.class.getDeclaredField('id').getAnnotation(SequenceGenerator).allocationSize()
            long nextMultipleOfAllocationSize = (long) Math.ceil(highestId/allocationSize)*allocationSize
            if (nextMultipleOfAllocationSize != highestId) {
                int numberOfItemsToSave = nextMultipleOfAllocationSize - highestId
                def fillUpItems = (1..numberOfItemsToSave).collect{ new StoredEventData() }
                eventStoreDataRepository.saveAll(fillUpItems)
            }
        }
    }

    QueryCount trackQueryCounts(Closure cl) {
        QueryCountHolder.clear()
        executeInTransaction {
            cl.call()
        }
        QueryCountHolder.grandTotal
    }
}
