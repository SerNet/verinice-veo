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

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.veo.adapter.presenter.api.common.SymIdRef;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.TailoringReferenceType;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.ref.ITypedSymbolicId;
import org.veo.core.entity.state.TailoringReferenceState;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Represents a {@link org.veo.core.entity.TailoringReference}. This base class can be used for many
 * {@link TailoringReferenceType}s, but some types require specific DTO subclasses that can hold
 * additional information.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@JsonIgnoreProperties("linkTailoringReferences")
public class TailoringReferenceDto<
        T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
    extends AbstractVersionedDto implements TailoringReferenceState<T, TNamespace> {

  @ToString.Include private UUID id;

  private TailoringReferenceType referenceType;

  private SymIdRef<T, TNamespace> target;

  @Override
  @JsonIgnore
  public ITypedSymbolicId<T, TNamespace> getTargetRef() {
    return target;
  }
}
