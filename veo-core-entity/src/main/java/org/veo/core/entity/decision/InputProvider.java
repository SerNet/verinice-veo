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
package org.veo.core.entity.decision;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;

/**
 * Provides input value for a {@link RuleCondition} in a {@link Decision}. Takes
 * an element and extracts a value from the element in the context of a given
 * domain.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @Type(value = CustomAspectAttributeSizeProvider.class, name = "customAspectAttributeSize"),
        @Type(value = CustomAspectAttributeValueProvider.class,
              name = "customAspectAttributeValue"),
        @Type(value = MaxRiskProvider.class, name = "maxRisk"), })
public interface InputProvider {
    public Object getValue(Element element, Domain domain);
}
