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

import java.util.Map;
import java.util.Set;

import org.veo.core.entity.Domain;
import org.veo.core.entity.ref.ITypedId;
import org.veo.core.entity.risk.ProcessRiskValues;

import lombok.EqualsAndHashCode;
import lombok.Value;

public interface ProcessDomainAssociationState extends DomainAssociationState {

  Map<String, ? extends ProcessRiskValues> getRiskValues();

  @Value
  @EqualsAndHashCode(callSuper = true)
  class ProcessDomainAssociationStateImpl extends DomainAssociationStateImpl
      implements ProcessDomainAssociationState {

    Map<String, ? extends ProcessRiskValues> riskValues;

    public ProcessDomainAssociationStateImpl(
        ITypedId<Domain> domain,
        String subType,
        String status,
        Map<String, ? extends ProcessRiskValues> riskValues,
        Set<CustomAspectState> customAspects,
        Set<CustomLinkState> customLinks) {
      super(domain, subType, status, customAspects, customLinks);
      this.riskValues = riskValues;
    }
  }
}
