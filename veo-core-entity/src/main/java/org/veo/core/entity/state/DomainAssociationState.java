/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jochen Kemnade
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
package org.veo.core.entity.state;

import java.util.Set;

import org.veo.core.entity.Domain;
import org.veo.core.entity.ref.ITypedId;

import lombok.Value;
import lombok.experimental.NonFinal;

public interface DomainAssociationState {
  ITypedId<Domain> getDomain();

  String getSubType();

  String getStatus();

  Set<CustomAspectState> getCustomAspectStates();

  Set<CustomLinkState> getCustomLinkStates();

  @Value
  @NonFinal
  class DomainAssociationStateImpl implements DomainAssociationState {
    ITypedId<Domain> domain;
    String subType;
    String status;
    Set<CustomAspectState> customAspectStates;
    Set<CustomLinkState> customLinkStates;
  }
}
