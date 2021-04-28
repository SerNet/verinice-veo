/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan.
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
package org.veo.persistence.access;

import java.math.BigInteger;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.veo.core.entity.Key;
import org.veo.core.entity.ModelObjectType;
import org.veo.core.repository.DesignatorSequenceRepository;

import lombok.AllArgsConstructor;

@Repository
@AllArgsConstructor
public class DesignatorSequenceRepositoryImpl implements DesignatorSequenceRepository {
    private final EntityManager em;

    /**
     * Creates all required designator sequences for a new client. This must be
     * called before fetching any sequence vales for that client.
     */
    public void createSequences(Key<UUID> clientId) {
        ModelObjectType.TYPE_DESIGNATORS.forEach(typeDesignator -> {
            em.createNativeQuery("CREATE SEQUENCE IF NOT EXISTS "
                    + getSequenceName(clientId, typeDesignator))
              .executeUpdate();
        });
    }

    /**
     * @return The next sequential designator number for given client & entity type,
     *         starting with 1.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long getNext(Key<UUID> clientId, String typeDesignator) {
        var next = (BigInteger) em.createNativeQuery("SELECT nextval('"
                + getSequenceName(clientId, typeDesignator) + "')")
                                  .getSingleResult();
        return next.longValue();
    }

    /**
     * Removes all designator sequences for given client from DB. This should be
     * performed as a cleanup when the client is removed.
     */
    public void deleteSequences(Key<UUID> clientId) {
        ModelObjectType.TYPE_DESIGNATORS.forEach(typeDesignator -> {
            em.createNativeQuery("DROP SEQUENCE IF EXISTS "
                    + getSequenceName(clientId, typeDesignator))
              .executeUpdate();
        });
    }

    private String getSequenceName(Key<UUID> clientId, String typeDesignator) {
        return "designator_" + clientId.uuidValue()
                                       .replace("-", "")
                + "_" + typeDesignator;
    }
}
