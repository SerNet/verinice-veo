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
package org.veo.core.entity.decision;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.NotNull;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.condition.VeoExpression;
import org.veo.core.entity.event.ElementEvent;

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
  public boolean isAffectedByEvent(ElementEvent event, Domain domain) {
    return expression.isAffectedByEvent(event, domain);
  }

  @Override
  public void selfValidate(DomainBase domain) {
    expression.selfValidate(domain, getElementType());
    Class<?> resultType = expression.getValueType(domain, getElementType());
    if (resultType != null
        && !List.of(Boolean.class, Integer.class, BigDecimal.class, String.class)
            .contains(resultType)) {
      throw new IllegalArgumentException(
          "Expressive decisions must yield a primitive result type, but given expression yields %s."
              .formatted(resultType.getSimpleName()));
    }
  }

  @Override
  public Class<?> getResultType(DomainBase domain) {
    return expression.getValueType(domain, getElementType());
  }
}
