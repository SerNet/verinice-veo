/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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
package org.veo.persistence;

import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/** Provides current user or a fallback value. */
@Component
@Slf4j
@Profile("!resttest")
public class LenientCurrentUserProviderImpl implements CurrentUserProvider {

  private final AuditorAware<String> auditorAware;

  public LenientCurrentUserProviderImpl(AuditorAware<String> auditorAware) {
    this.auditorAware = auditorAware;
  }

  @Override
  public String getUsername() {
    return auditorAware
        .getCurrentAuditor()
        .orElse(
            "MISSING - You're bypassing the controller and "
                + "accessing repositories directly, aren't you?");
  }
}
