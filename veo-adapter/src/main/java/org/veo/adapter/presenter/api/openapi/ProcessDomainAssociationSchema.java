/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Urs Zeidler
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
package org.veo.adapter.presenter.api.openapi;

import java.util.Map;
import java.util.Set;

import org.veo.core.entity.aspects.SubTypeAspect;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Details about this element's association with domains. Domain ID is key, association object is value.")
public interface ProcessDomainAssociationSchema {
    @Schema(name = "riskValues", implementation = ProcessRiskValueSchema.class)
    Map<String, Set<ProcessRiskValueSchema>> getRiskValues();

    @Schema(minLength = 1, maxLength = SubTypeAspect.SUB_TYPE_MAX_LENGTH)
    String getSubType();

    @Schema(minLength = 1, maxLength = SubTypeAspect.STATUS_MAX_LENGTH)
    String getStatus();

}
