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

import java.time.LocalDate;
import java.util.Optional;

import jakarta.validation.constraints.NotNull;

import javax.annotation.Nullable;

import org.veo.core.entity.compliance.ImplementationStatus;
import org.veo.core.entity.ref.ITypedSymbolicId;
import org.veo.core.entity.ref.TypedSymbolicId;
import org.veo.core.entity.state.RequirementImplementationTailoringReferenceState;

public interface RequirementImplementationTailoringReference<
        T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
    extends TailoringReference<T, TNamespace>,
        RequirementImplementationTailoringReferenceState<T, TNamespace> {

  void setStatus(@NotNull ImplementationStatus status);

  void setImplementationStatement(@Nullable String implementationStatement);

  void setImplementationUntil(@Nullable LocalDate implementationUntil);

  @Nullable
  T getResponsible();

  void setResponsible(@Nullable T responsible);

  @Override
  default ITypedSymbolicId<T, TNamespace> getResponsibleRef() {
    return Optional.ofNullable(getResponsible()).map(TypedSymbolicId::from).orElse(null);
  }
}
