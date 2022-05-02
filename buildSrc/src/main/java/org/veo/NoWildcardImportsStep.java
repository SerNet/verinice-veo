/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jochen Kemnade.
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
package org.veo;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.diffplug.spotless.FormatterStep;

/** Ensures that files do not contain wildcard imports. */
public final class NoWildcardImportsStep implements Serializable {
  private static final long serialVersionUID = 1L;

  private static final String NAME = "noWildCardImpors";

  private static final Pattern WILDCARD_IMPORT =
      Pattern.compile("^import[^\n]+\\*;$", Pattern.MULTILINE);

  /** Creates a FormatterStep which forbids wildcard imports. */
  public static FormatterStep create() {
    return FormatterStep.create(
        NoWildcardImportsStep.NAME, new NoWildcardImportsStep(), step -> step::format);
  }

  /** Formats the given string. */
  public String format(String raw) {
    Matcher m = WILDCARD_IMPORT.matcher(raw);
    if (m.find()) {
      throw new AssertionError("Found wildcard import: " + m.group(0));
    }
    return raw;
  }
}
