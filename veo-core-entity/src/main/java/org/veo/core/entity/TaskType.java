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
package org.veo.core.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.veo.core.entity.compliance.RequirementImplementation;

public enum TaskType {
  @JsonProperty(RequirementImplementation.SINGULAR_TERM)
  REQUIREMENT_IMPLEMENTATION,
  @JsonProperty(RequirementImplementation.SINGULAR_TERM + "-revision")
  REQUIREMENT_IMPLEMENTATION_REVISION,
  @JsonProperty("inspection-finding")
  INSPECTION_FINDING,
}
