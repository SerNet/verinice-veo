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
package org.veo.core.entity.profile;

import static lombok.AccessLevel.PACKAGE;
import static org.veo.core.entity.profile.ProfileDefinition.DEMO_UNIT;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import org.veo.core.entity.Constraints;
import org.veo.core.entity.Domain;
import org.veo.core.entity.exception.NotFoundException;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * References a profile on a domain using the profile's key (in the domain's map of profiles). A
 * profile key is only unique within a specific domain.
 */
@AllArgsConstructor(access = PACKAGE)
@EqualsAndHashCode
@Valid
@ToString
public class ProfileRef {
  public static final int MAX_ID_LENGTH = Constraints.DEFAULT_CONSTANT_MAX_LENGTH;

  @Getter
  @Size(max = MAX_ID_LENGTH)
  String keyRef;

  public ProfileRef(String profileKey, Domain domain) {
    this(profileKey);
    if (!domain.getProfiles().containsKey(profileKey)) {
      throw new NotFoundException(
          "Profile \"%s\" not found in domain %s", profileKey, domain.getIdAsString());
    }
  }

  // TODO VEO-1554 remove magical demo unit ref
  public static final ProfileRef DEMO_UNIT_REF = new ProfileRef(DEMO_UNIT);
}