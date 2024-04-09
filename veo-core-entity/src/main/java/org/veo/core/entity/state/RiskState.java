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
package org.veo.core.entity.state;

import java.util.Set;

import javax.annotation.Nullable;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Person;
import org.veo.core.entity.RiskAffected;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.ref.ITypedId;
import org.veo.core.entity.risk.RiskValues;

public interface RiskState<
    TRisk extends AbstractRisk<TNamespace, TRisk>,
    TNamespace extends RiskAffected<TNamespace, TRisk>> {
  ITypedId<TNamespace> getOwnerRef();

  ITypedId<Scenario> getScenarioRef();

  Set<ITypedId<Domain>> getDomainRefs();

  @Nullable
  ITypedId<Control> getMitigationRef();

  @Nullable
  ITypedId<Person> getRiskOwnerRef();

  Set<RiskValues> getRiskValues();
}
