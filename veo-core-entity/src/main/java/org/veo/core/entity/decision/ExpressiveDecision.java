/*
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
 */
package org.veo.core.entity.decision;

import java.util.Locale;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.condition.VeoExpression;
import org.veo.core.entity.event.ElementEvent;
import org.veo.core.entity.type.VeoType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Schema(description = "Decision for which the result is determined by an expression")
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExpressiveDecision extends Decision {
  @Schema(
      description =
          "Determines the result value of the decision - must yield a primitive value. Must not reference the containing decision (no circles allowed).")
  @NotNull
  private VeoExpression expression;

  @Override
  public DecisionResult evaluate(Element element, Domain domain) {
    return new DecisionResult(expression.getValue(element, domain));
  }

  @Override
  public String format(DecisionResult result, Locale locale, DomainBase domain) {
    return getResultType(domain).format(result.getValue(), locale);
  }

  @Override
  public boolean isAffectedByEvent(ElementEvent event, Domain domain) {
    return expression.isAffectedByEvent(event, domain);
  }

  @Override
  public void selfValidate(DomainBase domain) {
    expression.selfValidate(domain, getElementType());
    VeoType resultType = expression.getValueType(domain, getElementType());
    resultType.mustBeIncludedIn(
        VeoType.primitive(), "Expressive decisions must yield a primitive result");
  }

  @Override
  public VeoType getResultType(DomainBase domain) {
    return expression.getValueType(domain, getElementType());
  }
}
