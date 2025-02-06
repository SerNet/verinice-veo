/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
package org.veo.core.entity.inspection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.validation.constraints.Size;

import javax.annotation.Nullable;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementType;
import org.veo.core.entity.TranslatedText;
import org.veo.core.entity.aspects.ElementDomainAssociation;
import org.veo.core.entity.condition.Condition;
import org.veo.core.entity.condition.VeoExpression;
import org.veo.core.entity.exception.NotFoundException;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Configurable check that can be performed on an {@link Element}. If all {@link Condition}s match
 * on the element, the inspection will yield a {@link Finding} with the configured description,
 * {@link Severity} & {@link Suggestion}s.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(
    description =
        "Dynamic check to be performed on elements. An inspection can find a problem with an element, direct the user's attention to the problem and suggest actions that would fix the problem. An inspection defines a condition and some suggestions. If the inspection is run on an element and the condition is true, the suggestions are presented to the user.")
public class Inspection {

  @NonNull Severity severity;
  TranslatedText description;

  @Nullable
  @Schema(
      description =
          "Element type (singular term) that this inspection applies to. If this is null, the inspection applies to all element types.")
  ElementType elementType;

  @Nullable
  @Size(min = 1, max = ElementDomainAssociation.SUB_TYPE_MAX_LENGTH)
  @Schema(
      description =
          "Element sub type that this inspection applies to. If this is null, the inspection applies to all element sub types.")
  String elementSubType;

  @NonNull VeoExpression condition;
  final List<Suggestion> suggestions = new ArrayList<>();

  public Optional<Finding> run(Element element, Domain domain) {
    if (elementType != null && !elementType.matches(element)) {
      return Optional.empty();
    }
    if (elementSubType != null
        && !elementSubType.equals(element.findSubType(domain).orElse(null))) {
      return Optional.empty();
    }
    if (Objects.equals(condition.getValue(element, domain), true)) {
      return Optional.of(new Finding(severity, description, suggestions));
    }
    return Optional.empty();
  }

  public Inspection suggestAddingPart(String subType) {
    suggestions.add(new AddPartSuggestion(subType));
    return this;
  }

  /**
   * @throws IllegalArgumentException If the condition is an invalid expression
   * @throws NotFoundException If this inspection's element type or subtype, or any domain contents
   *     referenced by its condition cannot be found
   */
  public void selfValidate(DomainBase domain) {
    var etd = domain.getElementTypeDefinition(getElementType());
    Optional.ofNullable(getElementSubType()).ifPresent(etd::getSubTypeDefinition);
    condition.selfValidate(domain, getElementType());
  }
}
