/*******************************************************************************
 * Copyright (c) 2020 Urs Zeidler.
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
package org.veo.core.entity;

import java.time.Instant;

/**
 * A versioned entity with a sequential version number, lifecycle state and
 * validity time frame.
 */
public interface Versioned {
    /**
     * Lifecycle state of an entity. When a lifecycle state changes, the version
     * number needs to be increased in most cases.
     *
     * The possible states are defined as follows:
     *
     * <ul>
     * <li>CREATING: a newly created entity that has not yet been persisted into the
     * repository</li>
     * <li>STORED_CURRENT: a persisted entity in its currently valid version that
     * can be changed</li>
     * <li>STORED_ARCHIVED: a persisted entity in an older version that can no
     * longer be changed</li>
     * <li>STORED_DRAFT: a persisted entity based on the current entity that is
     * currently being edited and can be persisted into the repository. A draft must
     * have a higher version number than the object with STORED_CURRENT status.</li>
     * <li>STORED_DELETED: a entity that is marked as deleted and can no longer be
     * edited</li>
     * <li>DELETING: an entity that is in the process of being deleted from the
     * repository.</li>
     * </ul>
     */

    public enum Lifecycle {
        CREATING, STORED_CURRENT, STORED_ARCHIVED, STORED_DRAFT, STORED_DELETED, DELETING
    }

    /**
     * Map the type to a String.
     *
     * @see ModelPackage
     * @return
     */
    String getModelType();

    /**
     * @see Lifecycle
     */
    Lifecycle getState();

    /**
     * @see Lifecycle
     */
    void setState(Lifecycle state);

    Instant getValidFrom();

    void setValidFrom(Instant validFrom);

    Instant getValidUntil();

    void setValidUntil(Instant validUntil);

    // @formatter:off
    /**
     * The version number starts a 0 for a new object and is increased whenever
     * the entity is edited by the user and saved.
     *
     * DRAFTs will have their version number increased. Whenever a DRAFT becomes
     * the STORED_CURRENT version, the state of the previous version will be set
     * to STORED_ARCHIVED (and may be moved to a separate database table that
     * contains only archived data).
     *
     * When a draft is discarded, it will simply be deleted and the
     * STORED_CURRENT remains unchanged. Then the object is finally deleted it
     * will simply be marked as such:
     *
     * discard draft ┌──<────────────┐ ┌────────┐ save ┌────────┴─────┐ ┌──┴──┐
     * save(*) ┌──────────────┐ del ┌──────────────┐(**)
     * │CREATING├─────────>┤STORED_CURRENT├─────>┤DRAFT├─────────────>┤STORED_CURRENT├─────>┤STORED_DELETED│
     * │ v0 │ │ v1 │ edit │ v2 ├─┐ │ v2 │ │ v2 │ └────────┘ └──────────────┘
     * └───┬─┘ │ └──────────────┘ └──────────────┘ ▲ │ └───┘ overwrite draft
     * (autosave)
     *
     * (*) When the DRAFT is saved, v1 will have its state set to
     * STORED_ARCHIVED and its "validUntil" timestamp set to now(). (**) A
     * deleted version will keep the version number but have its state set to
     * STORED_DELETED and the "validUntil" field set to the timestamp of the
     * delete operation. It will represent the last known state of the object.
     */
    // @formatter:on
    long getVersion();

    void setVersion(long version);
}