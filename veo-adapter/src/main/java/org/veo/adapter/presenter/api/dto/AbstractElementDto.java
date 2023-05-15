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

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.veo.adapter.presenter.api.common.IdRef;
import org.veo.adapter.presenter.api.openapi.IdRefOwner;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;
import org.veo.core.entity.ElementOwner;
import org.veo.core.entity.ref.ITypedId;
import org.veo.core.entity.state.CustomAspectState;
import org.veo.core.entity.state.CustomLinkState;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.NonFinal;

/** Base transfer object for Elements. Contains common data for all Element DTOs. */
@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@SuppressWarnings("PMD.AbstractClassWithoutAnyMethod")
public abstract class AbstractElementDto extends AbstractVersionedSelfReferencingDto
    implements NameableDto {

  @Schema(
      description = "The name for the Element.",
      example = "Lock doors",
      requiredMode = REQUIRED)
  @ToString.Include
  private String name;

  @Schema(
      description = "Compact human-readable identifier that is unique within the client.",
      example = "A-155",
      accessMode = Schema.AccessMode.READ_ONLY)
  @ToString.Include
  private String designator;

  @Schema(description = "The abbreviation for the Element.", example = "Lock doors")
  private String abbreviation;

  @Schema(description = "The description for the Element.", example = "Lock doors")
  private String description;

  @NotNull(message = "An owner must be present.")
  @Schema(requiredMode = REQUIRED, implementation = IdRefOwner.class)
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

  public abstract void clearDomains();

  public abstract void transferToDomain(String sourceDomainId, String targetDomainId);

  @JsonIgnore
  public Set<? extends CustomAspectState> getCustomAspectStates() {
    return customAspects.entrySet().stream()
        .map(e -> new CustomAspectStateImpl(null, e.getKey(), e.getValue().getAttributes()))
        .collect(Collectors.toSet());
  }

  @JsonIgnore
  public Set<? extends CustomLinkState> getCustomLinkStates() {
    return links.entrySet().stream()
        .flatMap(
            e ->
                e.getValue().stream()
                    .map(
                        l ->
                            new CustomLinkStateImpl(
                                null, e.getKey(), l.getAttributes(), l.getTarget())))
        .collect(Collectors.toSet());
  }

  @Value
  @NonFinal
  static class CustomAspectStateImpl implements CustomAspectState {
    private final ITypedId<Domain> domain;
    private final String type;
    private final Map<String, Object> attributes;
  }

  @Value
  @EqualsAndHashCode(callSuper = true)
  static class CustomLinkStateImpl extends CustomAspectStateImpl implements CustomLinkState {

    private ITypedId<Element> target;

    public CustomLinkStateImpl(
        ITypedId<Domain> domainRef,
        String type,
        Map<String, Object> attributes,
        ITypedId<Element> target) {
      super(domainRef, type, attributes);
      this.target = target;
    }
  }
}
