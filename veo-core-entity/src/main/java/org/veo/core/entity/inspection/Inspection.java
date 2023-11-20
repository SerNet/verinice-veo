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
import org.veo.core.entity.Element;
import org.veo.core.entity.TranslatedText;
import org.veo.core.entity.aspects.SubTypeAspect;
import org.veo.core.entity.condition.Condition;
import org.veo.core.entity.condition.VeoExpression;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Configurable check that can be performed on an {@link Element}. If all {@link Condition}s match
 * on the element, the inspection will yield a {@link Finding} with the configured description,
 * {@link Severity} & {@link Suggestion}s.
 */
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class Inspection {

  @NonNull Severity severity;
  final TranslatedText description;

  @Nullable
  @Size(min = 1, max = 32)
  String elementType;

  @Nullable
  @Size(min = 1, max = SubTypeAspect.SUB_TYPE_MAX_LENGTH)
  String elementSubType;

  @NonNull VeoExpression condition;
  final List<Suggestion> suggestions = new ArrayList<>();

  public Optional<Finding> run(Element element, Domain domain) {
    if (elementType != null && !elementType.equals(element.getModelType())) {
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
}
