/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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
package org.veo.core.usecase.parameter;

import java.util.ArrayList;
import java.util.List;

import org.veo.core.entity.Identifiable;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.ref.ITypedSymbolicId;
import org.veo.core.entity.ref.TypedSymbolicId;
import org.veo.core.entity.state.TailoringReferenceParameterState;
import org.veo.core.entity.state.TemplateItemIncarnationDescriptionState;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
@AllArgsConstructor
/** Contains the element together with the relevant tailoringreferences. */
public class TemplateItemIncarnationDescription<
        T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
    implements TemplateItemIncarnationDescriptionState<T, TNamespace> {
  private T item;
  private List<TailoringReferenceParameter> references = new ArrayList<>();

  @Override
  public ITypedSymbolicId<T, TNamespace> getItemRef() {
    return TypedSymbolicId.from(item);
  }

  @Override
  public List<TailoringReferenceParameterState> getParameterStates() {
    return references.stream().map(TailoringReferenceParameterState.class::cast).toList();
  }
}
