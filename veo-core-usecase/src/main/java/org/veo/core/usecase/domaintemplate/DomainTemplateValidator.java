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
package org.veo.core.usecase.domaintemplate;

import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class DomainTemplateValidator {
  public static void validateVersion(String version) {
    try {
      validateVersion(Version.valueOf(version));
    } catch (ParseException parseEx) {
      throw new IllegalArgumentException(
          "Version %s does not conform to Semantic Versioning 2.0.0".formatted(version), parseEx);
    }
  }

  public static void validateVersion(Version version) {
    if (!version.getPreReleaseVersion().isEmpty() || !version.getBuildMetadata().isEmpty()) {
      throw new IllegalArgumentException(
          "Pre-release & metadata labels are not supported for domain template versions");
    }
  }
}
