/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Alexander Koderman
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

import java.util.HashMap;
import java.util.Map;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.dto.full.RiskValuesDto;
import org.veo.core.entity.Domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(
    description =
        "References a domain and its available risk-definitions in a map of "
            + "risk-definition-ID to riskDefinition")
public class RiskDomainAssociationDto {
  IdRef<Domain> reference;
  Map<String, RiskValuesDto> riskDefinitions = new HashMap<>();

  public RiskDomainAssociationDto(IdRef<Domain> domainRef) {
    this.reference = domainRef;
  }

  public RiskDomainAssociationDto(
      IdRef<Domain> domainRef, Map<String, RiskValuesDto> riskDefinitions) {
    this.reference = domainRef;
    this.riskDefinitions = riskDefinitions;
  }
}
