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

import java.util.Collections;
import java.util.Map;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.dto.TailoringReferenceDto;
import org.veo.core.entity.TemplateItem;

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
public class RiskTailoringReferenceDto<T extends TemplateItem<T>> extends TailoringReferenceDto<T> {
  private IdRef<T> mitigation;
  private IdRef<T> riskOwner;

  @Schema(description = "The properties of the risk element.", example = " name: 'value'")
  private Map<String, Object> riskAspects = Collections.emptyMap();
}
