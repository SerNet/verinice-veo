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
package org.veo.core.entity;

import java.util.Optional;

import javax.annotation.Nullable;

import org.veo.core.entity.ref.ITypedSymbolicId;
import org.veo.core.entity.ref.TypedSymbolicId;
import org.veo.core.entity.state.ControlImplementationTailoringReferenceState;

public interface ControlImplementationTailoringReference<
        T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
    extends TailoringReference<T, TNamespace>,
        ControlImplementationTailoringReferenceState<T, TNamespace> {

  void setDescription(@Nullable String description);

  @Nullable
  T getResponsible();

  void setResponsible(@Nullable T responsible);

  @Override
  default ITypedSymbolicId<T, TNamespace> getResponsibleRef() {
    return Optional.ofNullable(getResponsible()).map(TypedSymbolicId::from).orElse(null);
  }
}
