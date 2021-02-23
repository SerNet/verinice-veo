/*******************************************************************************
 * Copyright (c) 2020 Alexander Koderman.
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.veo.rest.security;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuthAwareImpl implements AuditorAware<String> {

    @Override
    public @Nonnull Optional<String> getCurrentAuditor() {
        var currentUser = Optional.ofNullable(SecurityContextHolder.getContext()
                                                                   .getAuthentication())
                                  .map(Authentication::getPrincipal)
                                  .map(ApplicationUser::authenticatedUser)
                                  .map(ApplicationUser::getUsername);
        log.debug("Current auditor determined from SecurityContext as {}",
                  currentUser.orElse("-MISSING-"));
        return currentUser;
    }
}
