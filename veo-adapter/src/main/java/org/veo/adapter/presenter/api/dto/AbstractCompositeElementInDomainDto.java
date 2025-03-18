/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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

import java.util.Collections;
import java.util.Set;

import org.veo.adapter.presenter.api.common.ElementInDomainIdRef;
import org.veo.core.entity.CompositeElement;
import org.veo.core.entity.EntityType;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.state.CompositeElementState;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public abstract class AbstractCompositeElementInDomainDto<T extends CompositeElement<T>>
    extends AbstractElementInDomainDto<T> implements CompositeElementState<T> {

  private Set<ElementInDomainIdRef<T>> parts = Collections.emptySet();

  @Schema(description = "Elements contained in this composite element")
  @Override
  public Set<ElementInDomainIdRef<T>> getParts() {
    return parts;
  }

  public void setParts(Set<ElementInDomainIdRef<T>> parts) {
    parts.forEach(
        p -> {
          if (!p.getType().equals(getModelInterface())) {
            throw new UnprocessableDataException(
                EntityType.getPluralTermByType(p.getType())
                    + " cannot be parts of "
                    + EntityType.getPluralTermByType(getModelInterface())
                    + ".");
          }
        });
    this.parts = parts;
  }
}
