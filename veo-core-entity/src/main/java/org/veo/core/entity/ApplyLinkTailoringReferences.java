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

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.veo.core.entity.condition.VeoExpression;
import org.veo.core.entity.exception.UnprocessableDataException;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@Valid
public final class ApplyLinkTailoringReferences extends ActionStep {

  @NotNull private VeoExpression incarnations;

  private IncarnationConfiguration config;
  private String linkType;

  @JsonIgnore
  public Set<Element> getControls(Element element, Domain domain) {
    var targets = incarnations.getValue(element, domain);
    if (targets instanceof Collection<?> elements) {
      return elements.stream()
          .map(
              o -> {
                if (o instanceof Element e) return e;
                throw new UnprocessableDataException(
                    "Cannot reapply catalog item on %s, expected an element"
                        .formatted(o.getClass()));
              })
          .collect(Collectors.toSet());
    }
    ;
    throw new UnprocessableDataException(
        "Cannot reapply catalog items for %s, expected a collection of elements"
            .formatted(targets.getClass()));
  }

  @Override
  void selfValidate(Domain domain, String elementType) {
    incarnations.selfValidate(domain, elementType);
  }
}
