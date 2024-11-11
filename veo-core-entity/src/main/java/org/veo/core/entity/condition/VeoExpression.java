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
package org.veo.core.entity.condition;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainBase;
import org.veo.core.entity.Element;
import org.veo.core.entity.event.ElementEvent;

/**
 * Provides input value for a {@link Condition}. Takes an element and extracts a value from the
 * element in the context of a given domain.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @Type(value = AndExpression.class, name = "and"),
  @Type(value = CurrentElementExpression.class, name = "currentElement"),
  @Type(value = ConstantExpression.class, name = "constant"),
  @Type(value = ContainsExpression.class, name = "contains"),
  @Type(value = CustomAspectAttributeSizeExpression.class, name = "customAspectAttributeSize"),
  @Type(value = CustomAspectAttributeValueExpression.class, name = "customAspectAttributeValue"),
  @Type(value = EqualsExpression.class, name = "equals"),
  @Type(value = DecisionResultValueExpression.class, name = "decisionResultValue"),
  @Type(value = ImplementedRequirementsExpression.class, name = "implementedRequirements"),
  @Type(value = LinkTargetsExpression.class, name = "linkTargets"),
  @Type(value = MapExpression.class, name = "map"),
  @Type(value = MaxRiskExpression.class, name = "maxRisk"),
  @Type(value = PartCountExpression.class, name = "partCount"),
  @Type(value = RemoveExpression.class, name = "remove"),
})
public interface VeoExpression {
  public Object getValue(Element element, Domain domain);

  /** Determines whether this provider may yield a different value after given event. */
  default boolean isAffectedByEvent(ElementEvent event, Domain domain) {
    return false;
  }

  /**
   * @throws RuntimeException If this provider is invalid for given domain & element type
   */
  default void selfValidate(DomainBase domain, String elementType) {}

  /**
   * @return type of the values that this provider will yield in given domain for given element type
   */
  Class<?> getValueType(DomainBase domain, String elementType);
}
