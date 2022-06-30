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
package org.veo.adapter.presenter.api.dto;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.openapi.IdRefOwner;
import org.veo.core.entity.ElementOwner;
import org.veo.core.entity.Nameable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

/** Base transfer object for Elements. Contains common data for all Element DTOs. */
@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@SuppressWarnings("PMD.AbstractClassWithoutAnyMethod")
public abstract class AbstractElementDto extends AbstractVersionedSelfReferencingDto
    implements NameableDto {

  @NotNull(message = "A name must be present.")
  @Schema(description = "The name for the Element.", example = "Lock doors", required = true)
  @Size(max = Nameable.NAME_MAX_LENGTH)
  @ToString.Include
  private String name;

  @Schema(
      description = "Compact human-readable identifier that is unique within the client.",
      example = "A-155",
      accessMode = Schema.AccessMode.READ_ONLY)
  @ToString.Include
  private String designator;

  @Schema(description = "The abbreviation for the Element.", example = "Lock doors")
  @Size(max = Nameable.ABBREVIATION_MAX_LENGTH)
  private String abbreviation;

  @Schema(
      description = "The description for the Element.",
      example = "Lock doors",
      required = false)
  @Size(max = Nameable.DESCRIPTION_MAX_LENGTH)
  private String description;

  @NotNull(message = "An owner must be present.")
  @Schema(required = true, implementation = IdRefOwner.class)
  private IdRef<ElementOwner> owner;

  @Valid
  @Schema(description = "Custom relations which do not affect the behavior.", title = "CustomLink")
  private Map<String, List<CustomLinkDto>> links = Collections.emptyMap();

  @Valid
  @Schema(
      description = "Groups of customizable attributes - see '/schemas'",
      title = "CustomAspect")
  private Map<String, CustomAspectDto> customAspects = Collections.emptyMap();

  @Schema(description = "Entity type identifier", accessMode = Schema.AccessMode.READ_ONLY)
  private String type;

  public abstract void associateWithTargetDomain(String id);

  public abstract void clearDomains();
}
