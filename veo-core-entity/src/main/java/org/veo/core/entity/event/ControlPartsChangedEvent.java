/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Alexander Koderman
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

import static java.util.Collections.unmodifiableSet;

import java.util.Set;
import java.util.UUID;

import org.veo.core.entity.Control;
import org.veo.core.entity.Element;

import lombok.Value;

@Value
public class ControlPartsChangedEvent implements ElementEvent {

  Control entity;
  UUID clientId;

  Set<Control> oldRecursiveParts;

  public ControlPartsChangedEvent(Control entity, Set<Control> oldRecursiveParts) {
    this.entity = entity;
    this.clientId = entity.getOwningClient().orElseThrow().getId();
    this.oldRecursiveParts = unmodifiableSet(oldRecursiveParts);
  }

  @Override
  public UUID getEntityId() {
    return entity.getId();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Class<? extends Element> getEntityType() {
    return entity.getModelInterface();
  }

  @Override
  public Object getSource() {
    return entity;
  }

  public Set<Element> getNewRecursiveParts() {
    return unmodifiableSet(entity.getPartsRecursively());
  }
}
