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
package org.veo;

import java.io.Serializable;
import java.util.regex.Pattern;

import com.diffplug.spotless.FormatterStep;

/**
 * Formats spock blocks with:
 *
 * <ul>
 *   <li>one empty line before each block label (except at the beginning of the function)
 *   <li>no empty lines after block labels
 * </ul>
 */
public class SpockBlockFormatterStep implements Serializable {
  private static final long serialVersionUID = 1L;

  private static final String NAME = "spockLinebreak";

  private static final Pattern SPOCK_BLOCK_PATTERN =
      Pattern.compile(
          "\\n*(^\\s+(given|when|then|and|expect|where):(\\s*['\"].*['\"])?$)\\n*",
          Pattern.MULTILINE);
  private static final Pattern EMPTY_LINES_AT_BEGINNING_PATTERN =
      Pattern.compile("([\\[{,]\\n)\\n+(^\\s+(given|when|expect):)", Pattern.MULTILINE);

  /** Creates a FormatterStep which formats spock blocks. */
  public static FormatterStep create() {
    return FormatterStep.create(NAME, new SpockBlockFormatterStep(), step -> step::format);
  }

  /** Formats the given string. */
  public String format(String raw) {
    var s = SPOCK_BLOCK_PATTERN.matcher(raw).replaceAll("\n\n$1\n");
    return EMPTY_LINES_AT_BEGINNING_PATTERN.matcher(s).replaceAll("$1$2");
  }
}
