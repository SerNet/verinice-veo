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
package org.veo.core.entity.type;

import java.time.Duration;
import java.util.Comparator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.veo.core.entity.definitions.attribute.DurationAttributeDefinition;

final class DurationStringType extends SimpleType {
  static final DurationStringType INSTANCE = new DurationStringType();

  private DurationStringType() {
    super(String.class);
  }

  @Override
  public <T> Comparator<T> getComparator() {
    return Comparator.comparing(d -> DurationAttributeDefinition.parse((String) d));
  }

  @Override
  public boolean includes(VeoType other) {
    return other instanceof DurationStringType;
  }

  @Override
  public String toHumanReadable() {
    return "DurationString";
  }

  @Override
  public String toString() {
    return toHumanReadable();
  }

  @Override
  public String format(Object value, Locale locale) {
    return split(DurationAttributeDefinition.parse((String) value))
        .map(
            e ->
                "%s %s"
                    .formatted(
                        e.amount,
                        ResourceBundle.getBundle("messages", locale).getString(e.unitKey())))
        .collect(Collectors.joining(", "));
  }

  private Stream<UnitComponent> split(Duration d) {
    if (d.isZero()) {
      return Stream.of(new UnitComponent("seconds", 0));
    }
    return Stream.of(
            new UnitComponent("days", d.toDays()),
            new UnitComponent("hours", d.toHoursPart()),
            new UnitComponent("minutes", d.toMinutesPart()),
            new UnitComponent("seconds", d.toSecondsPart()))
        .filter(e -> e.amount > 0);
  }

  private record UnitComponent(String unitKey, long amount) {}
}
