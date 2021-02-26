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

import org.springframework.data.domain.AuditorAware;

import org.veo.persistence.CurrentUserProvider;

/**
 * Strict {@link CurrentUserProvider} implementation that throws an exception
 * when there is no user.
 */
public class CurrentUserProviderImpl implements CurrentUserProvider {

    private final AuditorAware<String> auditorAware;

    public CurrentUserProviderImpl(AuditorAware<String> auditorAware) {
        this.auditorAware = auditorAware;
    }

    @Override
    public String getUsername() {
        return auditorAware.getCurrentAuditor()
                           .orElseThrow();
    }
}
