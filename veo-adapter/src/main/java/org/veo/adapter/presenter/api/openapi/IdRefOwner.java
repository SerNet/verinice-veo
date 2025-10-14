/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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

/** Swagger documentation for the reference 'owner' of 'Asset': */
@Schema(name = "OwnerReference", description = "A reference to the unit containing this entity.")
public interface IdRefOwner extends IIdRef {

  // TODO: update reference doc for OwnerReference

  @Schema(
      description = "A friendly human readable title of the referenced unit.",
      example = "My Unit",
      accessMode = Schema.AccessMode.READ_ONLY)
  @Override
  String getDisplayName();

  @Schema(
      requiredMode = REQUIRED,
      description = "The resource URL of the referenced unit.",
      example = "http://<api.veo.example>/veo/units/<00000000-0000-0000-0000-000000000000>",
      format = "uri")
  @Override
  String getTargetUri();
}
