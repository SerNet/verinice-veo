/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Alexander Koderman
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
package org.veo.persistence.entity.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.veo.core.entity.risk.RiskDefinitionRef;

/** JPA converter mapping {@link RiskDefinitionRef} values to varchar columns. */
@Converter(autoApply = true)
class RiskDefinitionRefConverter implements AttributeConverter<RiskDefinitionRef, String> {

  @Override
  public String convertToDatabaseColumn(RiskDefinitionRef attribute) {
    return attribute == null ? null : attribute.getIdRef();
  }

  @Override
  public RiskDefinitionRef convertToEntityAttribute(String dbData) {
    return dbData == null
        ? null
        : RiskReferenceFactoryImpl.getInstance().createRiskDefinitionRef(dbData);
  }
}
