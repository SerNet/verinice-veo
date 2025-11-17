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
package org.veo.adapter.service.domaintemplate.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.veo.adapter.presenter.api.common.SymIdRef;
import org.veo.adapter.presenter.api.dto.TailoringReferenceDto;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.TemplateItem;
import org.veo.core.entity.compliance.ImplementationStatus;
import org.veo.core.entity.ref.ITypedSymbolicId;
import org.veo.core.entity.state.RequirementImplementationTailoringReferenceState;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/** Represents a {@link org.veo.core.entity.RequirementImplementationTailoringReference} */
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequirementImplementationTailoringReferenceDto<
        T extends TemplateItem<T, TNamespace>, TNamespace extends Identifiable>
    extends TailoringReferenceDto<T, TNamespace>
    implements RequirementImplementationTailoringReferenceState<T, TNamespace> {
  private ImplementationStatus status;
  private SymIdRef<T, TNamespace> responsible;
  private String implementationStatement;
  private LocalDate implementationUntil;

  private Integer cost;

  private LocalDate implementationDate;

  private SymIdRef<T, TNamespace> implementedBy;
  private SymIdRef<T, TNamespace> document;
  private LocalDate lastRevisionDate;
  private SymIdRef<T, TNamespace> lastRevisionBy;
  private LocalDate nextRevisionDate;
  private SymIdRef<T, TNamespace> nextRevisionBy;

  @Override
  @JsonIgnore
  public ITypedSymbolicId<T, TNamespace> getResponsibleRef() {
    return responsible;
  }

  @Override
  @JsonIgnore
  public ITypedSymbolicId<T, TNamespace> getImplementedByRef() {
    return implementedBy;
  }

  @Override
  @JsonIgnore
  public ITypedSymbolicId<T, TNamespace> getDocumentRef() {
    return document;
  }

  @Override
  @JsonIgnore
  public ITypedSymbolicId<T, TNamespace> getLastRevisionByRef() {
    return lastRevisionBy;
  }

  @Override
  @JsonIgnore
  public ITypedSymbolicId<T, TNamespace> getNextRevisionByRef() {
    return nextRevisionBy;
  }
}
