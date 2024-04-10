/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Urs Zeidler.
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
package org.veo.adapter.service.domaintemplate.dto;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.veo.adapter.presenter.api.common.SymIdRef;
import org.veo.adapter.presenter.api.dto.TailoringReferenceDto;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.RiskTailoringReferenceValues;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.ref.ITypedSymbolicId;
import org.veo.core.entity.risk.RiskDefinitionRef;
import org.veo.core.entity.state.RiskTailoringReferenceState;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/** Represents a {@link org.veo.core.entity.RiskTailoringReference} */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RiskTailoringReferenceDto<
        T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
    extends TailoringReferenceDto<T, TNamespace>
    implements RiskTailoringReferenceState<T, TNamespace> {
  private SymIdRef<T, TNamespace> mitigation;
  private SymIdRef<T, TNamespace> riskOwner;

  @Schema(description = "Keys are risk definition IDs, values hold risk values")
  private Map<RiskDefinitionRef, RiskTailoringReferenceValues> riskDefinitions = new HashMap<>();

  @Override
  @JsonIgnore
  public ITypedSymbolicId getMitigationRef() {
    return mitigation;
  }

  @Override
  @JsonIgnore
  public ITypedSymbolicId getRiskOwnerRef() {
    return riskOwner;
  }
}
