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
package org.veo.adapter.presenter.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.veo.core.entity.TailoringReferenceType;
import org.veo.core.entity.TailoringReferenceTyped;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** Base class for all TailoringReferenceDto. */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@SuppressWarnings("PMD.AbstractClassWithoutAnyMethod")
@JsonIgnoreProperties(value = "linkTailoringReferences")
public abstract class AbstractProfileTailoringReferenceDto extends AbstractProfileReferenceDto
    implements TailoringReferenceTyped {

  private TailoringReferenceType referenceType;
}
