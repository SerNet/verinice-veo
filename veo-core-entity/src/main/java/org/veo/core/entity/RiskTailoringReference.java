/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler
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

import java.util.Map;
import java.util.Optional;

import org.veo.core.entity.ref.ITypedSymbolicId;
import org.veo.core.entity.ref.TypedSymbolicId;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.state.RiskTailoringReferenceState;

/**
 * This reference type describes an {@link AbstractRisk}. The owning template item describes the
 * {@link RiskAffected} and the target template item describes the risk's {@link Scenario}.
 */
public interface RiskTailoringReference<
        T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
    extends TailoringReference<T, TNamespace>, RiskTailoringReferenceState<T, TNamespace> {
  void setMitigation(T mitigation);

  T getMitigation();

  @Override
  default ITypedSymbolicId<T, TNamespace> getMitigationRef() {
    return Optional.ofNullable(getMitigation()).map(TypedSymbolicId::from).orElse(null);
  }

  void setRiskOwner(T riskOwner);

  T getRiskOwner();

  @Override
  default ITypedSymbolicId<T, TNamespace> getRiskOwnerRef() {
    return Optional.ofNullable(getRiskOwner()).map(TypedSymbolicId::from).orElse(null);
  }

  @Override
  Map<RiskDefinitionRef, RiskTailoringReferenceValues> getRiskDefinitions();

  void setRiskDefinitions(Map<RiskDefinitionRef, RiskTailoringReferenceValues> riskDefinitions);
}
