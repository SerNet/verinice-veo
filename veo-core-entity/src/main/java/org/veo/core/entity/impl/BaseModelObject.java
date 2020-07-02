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
package org.veo.core.entity.impl;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.time.Instant;
import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import javax.validation.constraints.PositiveOrZero;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.veo.core.entity.Key;
import org.veo.core.entity.ModelObject;

/**
 * @author urszeidler
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public abstract class BaseModelObject implements ModelObject {

    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    // @formatter:off
    /**
     * The version number starts a 0 for a new object and is increased whenever the entity is
     * edited by the user and saved.
     *
     * DRAFTs will have their version number increased. Whenever a DRAFT becomes the STORED_CURRENT version,
     * the state of the previous version will be set to STORED_ARCHIVED (and may be moved to a separate
     * database table that contains only archived data).
     *
     * When a draft is discarded, it will simply be deleted and the STORED_CURRENT remains unchanged.
     * Then the object is finally deleted it will simply be marked as such:
     *
     *                                 discard draft
     *                                ┌──<────────────┐
     *   ┌────────┐   save   ┌────────┴─────┐      ┌──┴──┐     save(*)  ┌──────────────┐ del  ┌──────────────┐(**)
     *   │CREATING├─────────>┤STORED_CURRENT├─────>┤DRAFT├─────────────>┤STORED_CURRENT├─────>┤STORED_DELETED│
     *   │   v0   │          │      v1      │ edit │  v2 ├─┐            │     v2       │      │     v2       │
     *   └────────┘          └──────────────┘      └───┬─┘ │            └──────────────┘      └──────────────┘
     *                                                 ▲   │
     *                                                 └───┘
     *                                             overwrite draft
     *                                               (autosave)
     *
     * (*)  When the DRAFT is saved, v1 will have its state set to STORED_ARCHIVED and its "validUntil" timestamp
     *      set to now().
     * (**) A deleted version will keep the version number but have its state set to STORED_DELETED
     *      and the "validUntil" field set to the timestamp of the delete operation. It will represent
     *      the last known state of the object.
     */
    // @formatter:on
    @PositiveOrZero
    @ToString.Include
    protected long version;

    @NotNull
    @EqualsAndHashCode.Include
    @ToString.Include
    protected Key<UUID> id = Key.newUuid();

    protected @NotNull Lifecycle state = Lifecycle.CREATING;

    @PastOrPresent(message = "The start of the entity's validity must be in the past.")
    @NotNull(message = "The start of the entity's validity must be in the past.")
    protected Instant validFrom = Instant.now();

    @PastOrPresent(message = "The end of the entity's validity must be be set in the past or set to 'null' if it is currently still valid.")
    protected Instant validUntil;

    private boolean ghost;

    public BaseModelObject() {
        super();
    }

    public BaseModelObject(@NotNull Key<UUID> id) {
        super();
        this.id = id;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        changeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    @Override
    public void firePropertyChangeEvent(PropertyChangeEvent event) {
        changeSupport.firePropertyChange(event);
    }

    @Override
    public abstract String getModelType();

    @Override
    public Lifecycle getState() {
        return state;
    }

    @Override
    public void setState(Lifecycle state) {
        this.state = state;
    }

    @Override
    public Instant getValidFrom() {
        return validFrom;
    }

    @Override
    public void setValidFrom(Instant validFrom) {
        this.validFrom = validFrom;
    }

    @Override
    public Instant getValidUntil() {
        return validUntil;
    }

    @Override
    public void setValidUntil(Instant validUntil) {
        this.validUntil = validUntil;
    }

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public void setVersion(long version) {
        this.version = version;
    }

    @Override
    public Key<UUID> getId() {
        return id;
    }

    public void setId(Key<UUID> id) {
        this.id = id;
    }

    @Override
    public boolean isGhost() {
        return ghost;
    }

    @Override
    public void setGhost(boolean isGhost) {
        this.ghost = isGhost;

    }
}
