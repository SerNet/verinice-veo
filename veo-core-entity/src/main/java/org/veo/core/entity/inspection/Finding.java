/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
package org.veo.core.entity.inspection;

import java.util.List;

import org.veo.core.entity.TranslatedText;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** A problem or observation yielded by an {@link Inspection}. */
@AllArgsConstructor
@Getter
@Schema(description = "A problem or observation yielded by an inspection")
public class Finding {
  @Schema(description = "Rating of how problematic this finding is")
  Severity severity;

  @Schema(description = "Key is ISO language key, value is human-readable description")
  TranslatedText description;

  @Schema(description = "Suggested actions that would fix this finding")
  List<Suggestion> suggestions;
}
