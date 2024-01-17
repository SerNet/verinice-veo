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
package org.veo.adapter.presenter.api.dto;

import java.util.Set;

import org.veo.core.entity.RiskAffected;

/**
 * Represents {@link RiskAffected} elements with RIs in addition to CIs. Embedding RIs may not
 * always be desirable, because RI lists might get very long and add a lot of data to the requests.
 */
public interface RiskAffectedDtoWithRIs<T extends RiskAffected<T, ?>> extends RiskAffectedDto<T> {
  Set<RequirementImplementationDto> getRequirementImplementations();

  void setRequirementImplementations(Set<RequirementImplementationDto> requirementImplementations);
}
