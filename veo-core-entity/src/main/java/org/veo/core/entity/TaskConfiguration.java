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

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.veo.core.entity.exception.NotFoundException;
import org.veo.core.entity.exception.UnprocessableDataException;
import org.veo.core.entity.inspection.Inspection;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.NonNull;

@Schema(description = "Defines how tasks are managed in the domain")
public record TaskConfiguration(
    @Schema(
            description =
                "Determines how tasks are generated for specific inspections - keys are inspection IDs.")
        @NotNull
        @NonNull
        Map<String, InspectionTaskTemplate> inspectionTaskTemplates) {

  public TaskConfiguration() {
    this(new HashMap<>());
  }

  // enforce immutability
  public TaskConfiguration {
    inspectionTaskTemplates = Map.copyOf(inspectionTaskTemplates);
  }

  public void selfValidate(DomainBase domain) {
    inspectionTaskTemplates.forEach((inspectionId, taskTemplate) -> {
      try {
        var inspection = domain.getInspection(inspectionId);
        taskTemplate.selfValidate(inspection, domain);
      } catch (NotFoundException | UnprocessableDataException | IllegalArgumentException ex) {
        throw new UnprocessableDataException(
            "Invalid task template for inspection '%s': %s"
                .formatted(inspectionId, ex.getMessage()));
      }
    });
  }

  public record InspectionTaskTemplate(
      @Schema(
              description = "Brief task description",
              example = "{\"en\":\"Revise potential impact.\"}")
          TranslatedText summary,
      @Size(min = 1, max = Constraints.DEFAULT_STRING_MAX_LENGTH)
          @Schema(
              description =
                  "A custom link ID - the responsibility link type must target persons and is used to determine the person responsible for an inspection task.")
          String responsibilityLink) {
    void selfValidate(Inspection inspection, DomainBase domain) {
      if (responsibilityLink != null) {
        ElementType elementType = inspection.getElementType();
        if (elementType == null) {
          throw new UnprocessableDataException(
              "A responsibility link can only be defined, if the inspection targets a specific element type.");
        }
        var link = domain.getLinkDefinition(elementType, responsibilityLink);
        if (link.getTargetType() != ElementType.PERSON) {
          throw new UnprocessableDataException(
              "Link type '%s' cannot be used for task responsibilities, because it targets %s instead of persons"
                  .formatted(responsibilityLink, link.getTargetType().getPluralTerm()));
        }
      }
    }
  }
}
