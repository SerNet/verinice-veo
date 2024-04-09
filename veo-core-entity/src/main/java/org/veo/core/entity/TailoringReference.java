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
package org.veo.core.entity;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.ref.ITypedId;
import org.veo.core.entity.ref.TypedId;
import org.veo.core.entity.state.TailoringReferenceState;

/**
 * TailoringReference Refers another catalog item in this catalog which are connected and need to be
 * applied also. Like a set of controls connected to a scenario. The following constrains applies to
 * the tailoring refs: 1. The reference catalogItem always point to a catalogitem in the same
 * catalog. 2.1. All references defined by the element need to refer to an element of a catalogitem
 * in the same catalog. 2.2. For each such reference a coresponding tailref of type LINK must exist,
 * pointing to the catalogItem which holds the refered element.
 */
public interface TailoringReference<T extends TemplateItem<T>>
    extends TailoringReferenceState<T>, TemplateItemReference<T> {
  String SINGULAR_TERM = "tailoringreference";
  String PLURAL_TERM = "tailoringreferences";

  @Override
  default ITypedId<T> getTargetRef() {
    return TypedId.from(getTarget());
  }

  @NotNull
  TailoringReferenceType getReferenceType();

  void setReferenceType(TailoringReferenceType aReferenceType);

  default boolean isCopyRef() {
    return getReferenceType() == TailoringReferenceType.COPY
        || getReferenceType() == TailoringReferenceType.COPY_ALWAYS;
  }

  default boolean isParameterRef() {
    return !List.of(
            TailoringReferenceType.COPY,
            TailoringReferenceType.COPY_ALWAYS,
            TailoringReferenceType.OMIT)
        .contains(getReferenceType());
  }

  @Override
  default Class<TailoringReference> getModelInterface() {
    return TailoringReference.class;
  }

  @Override
  default String getModelType() {
    return SINGULAR_TERM;
  }

  void remove();
}
