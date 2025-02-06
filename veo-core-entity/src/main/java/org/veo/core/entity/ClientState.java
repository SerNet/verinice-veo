/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jonas Jordan
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

import java.util.Collections;
import java.util.Set;

import org.veo.core.entity.event.ClientEvent;

public enum ClientState {
  CREATED(Set.of(ClientEvent.ClientChangeType.ACTIVATION)),
  DELETED(Collections.emptySet()),
  DEACTIVATED(
      Set.of(ClientEvent.ClientChangeType.ACTIVATION, ClientEvent.ClientChangeType.DELETION)),
  ACTIVATED(
      Set.of(ClientEvent.ClientChangeType.MODIFICATION, ClientEvent.ClientChangeType.DEACTIVATION));

  ClientState(Set<ClientEvent.ClientChangeType> validChanges) {
    this.validChanges = validChanges;
  }

  private final Set<ClientEvent.ClientChangeType> validChanges;

  public boolean isValidChange(ClientEvent.ClientChangeType changeType) {
    return validChanges.contains(changeType);
  }

  public ClientState nextState(ClientEvent.ClientChangeType changeType) {
    return switch (changeType) {
      case CREATION -> ClientState.CREATED;
      case ACTIVATION, MODIFICATION -> ClientState.ACTIVATED;
      case DEACTIVATION -> ClientState.DEACTIVATED;
      case DELETION -> ClientState.DELETED;
    };
  }
}
