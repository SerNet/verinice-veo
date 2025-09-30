/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import org.veo.adapter.presenter.api.common.IIdRef;

import io.swagger.v3.oas.annotations.media.Schema;

public interface IdRefTailoringReferenceParameterReferencedElement extends IIdRef {
  @Schema(
      description = "A friendly human readable title of the element which will be linked.",
      example = "CTL_1",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Override
  String getDisplayName();

  @Schema(
      requiredMode = REQUIRED,
      description = "The resource URL of the referenced element.",
      example =
          "http://<api.veo.example>/veo/domains/<00000000-0000-0000-0000-000000000000>/catalog-items/<00000000-0000-0000-0000-000000000000>",
      format = "uri")
  @Override
  String getTargetUri();

  @Schema(accessMode = Schema.AccessMode.READ_ONLY)
  String getSearchesUri();

  @Schema(accessMode = Schema.AccessMode.READ_ONLY)
  String getResourcesUri();
}
