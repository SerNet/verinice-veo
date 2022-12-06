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
package org.veo.core.entity;

import java.util.Collections;
import java.util.Set;

import org.veo.core.entity.event.ClientEvent.ClientChangeType;

/**
 * A client is the root object of the organizational structure. Usually a client is a company or
 * other large closed organizational entity. The client could be used for high level authorization.
 */
public interface Client extends Identifiable, Versioned {

  String SINGULAR_TERM = "client";
  String PLURAL_TERM = "clients";

  String getName();

  void setName(String aName);

  /**
   * Add the given Domain to the collection domains.
   *
   * @return true if added
   */
  boolean addToDomains(Domain aDomain);

  /**
   * Remove the given Domain from the collection domains.
   *
   * @return true if removed
   */
  boolean removeFromDomains(Domain aDomain);

  Set<Domain> getDomains();

  void setDomains(Set<Domain> aDomains);

  @Override
  default Class<? extends Identifiable> getModelInterface() {
    return Client.class;
  }

  @Override
  default String getModelType() {
    return SINGULAR_TERM;
  }

  int getTotalUnits();

  void incrementTotalUnits();

  void decrementTotalUnits();

  ClientState getState();

  int getMaxUnits();

  void setMaxUnits(int newMaxunits);

  boolean updateState(ClientChangeType changeType);

  enum ClientState {
    CREATED(Set.of(ClientChangeType.ACTIVATION)),
    DELETED(Collections.emptySet()),
    DEACTIVATED(Set.of(ClientChangeType.ACTIVATION, ClientChangeType.DELETION)),
    ACTIVATED(Set.of(ClientChangeType.MODIFICATION, ClientChangeType.DEACTIVATION));

    private ClientState(Set<ClientChangeType> validChanges) {
      this.validChanges = validChanges;
    }

    private final Set<ClientChangeType> validChanges;

    public boolean isValidChange(ClientChangeType changeType) {
      return validChanges.contains(changeType);
    }

    public ClientState nextState(ClientChangeType changeType) {
      return switch (changeType) {
        case CREATION -> ClientState.CREATED;
        case ACTIVATION, MODIFICATION -> ClientState.ACTIVATED;
        case DEACTIVATION -> ClientState.DEACTIVATED;
        case DELETION -> ClientState.DELETED;
      };
    }
  }
}
