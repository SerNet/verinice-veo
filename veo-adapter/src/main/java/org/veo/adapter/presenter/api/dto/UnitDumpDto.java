/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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

import org.veo.adapter.presenter.api.dto.full.FullDomainDto;
import org.veo.adapter.presenter.api.dto.full.FullUnitDto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UnitDumpDto {
    private final FullUnitDto unit;
    private final Set<FullDomainDto> domains;
    private final Set<AbstractElementDto> elements;
    private final Set<AbstractRiskDto> risks;
}
