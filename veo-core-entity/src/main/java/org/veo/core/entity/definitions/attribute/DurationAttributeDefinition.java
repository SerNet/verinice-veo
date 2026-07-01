/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2026  Jochen Kemnade
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
package org.veo.core.entity.definitions.attribute;

import java.time.Duration;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.veo.core.entity.ValidationError;
import org.veo.core.entity.type.VeoType;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(
    description =
        "An attribute that represents a duration, coded as an ISO 8601 duration string, e.g. P3D or PT1H")
public final class DurationAttributeDefinition extends AttributeDefinition {
  public static final String TYPE = "duration";

  private static final Pattern ISO_STRUCTURE_PATTERN = Pattern.compile("^P([^T]*)(?:T(.*))?$");
  public static final List<ValidationError> ERROR_NO_ISO_DURATION =
      List.of(ValidationError.localized("error_no_iso_duration"));

  @Override
  public List<ValidationError> getErrors(Object value) {
    if (!(value instanceof String str)) {
      return List.of(ValidationError.localized("error_no_string"));
    }
    try {
      parse(str);
      return List.of();

    } catch (DateTimeParseException | IllegalArgumentException ex) {
      return ERROR_NO_ISO_DURATION;
    }
  }

  @NonNull
  public static Duration parse(String value) {
    Matcher matcher = ISO_STRUCTURE_PATTERN.matcher(value);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid ISO duration structure");
    }

    String periodPart = matcher.group(1);
    String durationPart = matcher.group(2);

    if (periodPart.isEmpty() && durationPart == null) {
      throw new IllegalArgumentException("Duration is empty");
    }
    var duration = durationPart != null ? Duration.parse("PT" + durationPart) : Duration.ZERO;
    if (!periodPart.isEmpty()) {
      var p = Period.parse("P" + periodPart);
      duration =
          duration
              .plus(ChronoUnit.DAYS.getDuration().multipliedBy(p.getDays()))
              .plus(ChronoUnit.MONTHS.getDuration().multipliedBy(p.getMonths()))
              .plus(ChronoUnit.YEARS.getDuration().multipliedBy(p.getYears()));
    }
    return duration;
  }

  @Override
  public VeoType getValueType() {
    return VeoType.durationString().orNothing();
  }
}
