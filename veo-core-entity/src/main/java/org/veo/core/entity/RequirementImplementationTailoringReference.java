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

  void setCost(Integer cost);

  void setImplementationDate(@Nullable LocalDate implementationDate);

  @Nullable
  T getImplementedBy();

  void setImplementedBy(@Nullable T implementedBy);

  @Nullable
  T getDocument();

  void setDocument(@Nullable T document);

  @Nullable
  T getLastRevisionBy();

  void setLastRevisionBy(@Nullable T lastRevisionBy);

  void setLastRevisionDate(@Nullable LocalDate implementationDate);

  @Nullable
  T getNextRevisionBy();

  void setNextRevisionBy(@Nullable T nextRevisionBy);

  void setNextRevisionDate(@Nullable LocalDate implementationDate);

  @Nullable
  T getAssessmentBy();

  void setAssessmentBy(@Nullable T assessmentBy);

  void setAssessmentDate(@Nullable LocalDate assessmentDate);

  @Override
  default ITypedSymbolicId<T, TNamespace> getResponsibleRef() {
    return Optional.ofNullable(getResponsible()).map(TypedSymbolicId::from).orElse(null);
  }

  @Override
  default ITypedSymbolicId<T, TNamespace> getImplementedByRef() {
    return Optional.ofNullable(getImplementedBy()).map(TypedSymbolicId::from).orElse(null);
  }

  @Override
  default ITypedSymbolicId<T, TNamespace> getDocumentRef() {
    return Optional.ofNullable(getDocument()).map(TypedSymbolicId::from).orElse(null);
  }

  @Override
  default ITypedSymbolicId<T, TNamespace> getLastRevisionByRef() {
    return Optional.ofNullable(getLastRevisionBy()).map(TypedSymbolicId::from).orElse(null);
  }

  @Override
  default ITypedSymbolicId<T, TNamespace> getNextRevisionByRef() {
    return Optional.ofNullable(getNextRevisionBy()).map(TypedSymbolicId::from).orElse(null);
  }

  @Override
  default ITypedSymbolicId<T, TNamespace> getAssessmentByRef() {
    return Optional.ofNullable(getAssessmentBy()).map(TypedSymbolicId::from).orElse(null);
  }
}
