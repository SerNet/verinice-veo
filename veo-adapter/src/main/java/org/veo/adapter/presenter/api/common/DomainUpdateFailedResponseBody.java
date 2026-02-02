/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2026  Jonas Jordan
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
package org.veo.adapter.presenter.api.common;

import java.util.List;
import java.util.stream.Collectors;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.Unit;
import org.veo.core.usecase.DomainUpdateFailedException;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(accessMode = Schema.AccessMode.READ_ONLY)
public class DomainUpdateFailedResponseBody extends ApiResponseBody {

  private List<UnitElementsDtoGroup> conflictedElementsByUnit;

  public DomainUpdateFailedResponseBody(
      DomainUpdateFailedException ex, ReferenceAssembler referenceAssembler) {
    super(
        false,
        "Domain update failed due to %s conflicted element(s)."
            .formatted(ex.getConflictedElements().size()));
    this.conflictedElementsByUnit =
        ex.getConflictedElements().stream()
            .collect(Collectors.groupingBy(Element::getOwner))
            .entrySet()
            .stream()
            .map(
                kv ->
                    UnitElementsDtoGroup.from(
                        kv.getKey(), kv.getValue(), referenceAssembler, ex.getOldDomain()))
            .toList();
  }

  public record UnitElementsDtoGroup(
      IdRef<Unit> unit, List<ElementInDomainIdRef<Element>> elements) {

    static UnitElementsDtoGroup from(
        Unit unit,
        List<Element> elements,
        ReferenceAssembler referenceAssembler,
        Domain oldDomain) {
      return new UnitElementsDtoGroup(
          IdRef.from(unit, referenceAssembler),
          elements.stream()
              .map(e -> ElementInDomainIdRef.from(e, oldDomain, referenceAssembler))
              .toList());
    }
  }
}
