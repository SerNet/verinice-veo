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
package org.veo.adapter.presenter.api.dto;

import org.veo.core.entity.Document;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Schema(
    title = "Document",
    description =
        "Represents a document describing another element or providing additional information, such as guidelines, policies or documentation - this DTO represents a document from the viewpoint of a domain and contains both basic and domain-specific properties.")
public abstract class AbstractDocumentInDomainDto
    extends AbstractCompositeElementInDomainDto<Document> {

  @Override
  @Schema(example = "User guideline")
  public String getName() {
    return super.getName();
  }

  @Override
  @Schema(description = "Short human-readable identifier (not unique)", example = "UG")
  public String getAbbreviation() {
    return super.getAbbreviation();
  }

  @Override
  @Schema(example = "Data security guidelines for all IT users at Data Inc.")
  public String getDescription() {
    return super.getDescription();
  }

  @Override
  @Schema(description = "Unique human-readable identifier", example = "DOC-16")
  public String getDesignator() {
    return super.getDesignator();
  }

  @Override
  public Class<Document> getModelInterface() {
    return Document.class;
  }
}
