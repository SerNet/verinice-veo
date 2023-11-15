/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jonas Jordan
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

import org.veo.core.entity.inspection.Inspection;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    description =
        "Dynamic check to be performed on elements. An inspection can find a problem with an element, direct the user's attention to the problem and suggest actions that would fix the problem. An inspection defines a condition and some suggestions. If the inspection is run on an element and the condition is true, the suggestions are presented to the user.")
public abstract class InspectionSchema extends Inspection {
  public InspectionSchema() {
    super(null, null, null, null, null);
  }

  @Override
  @Schema(
      description =
          "Element type (singular term) that this inspection applies to. If this is null, the inspection applies to all element types.")
  public abstract String getElementType();

  @Override
  @Schema(
      description =
          "Element sub type that this inspection applies to. If this is null, the inspection applies to all element sub types.")
  public abstract String getElementSubType();
}
