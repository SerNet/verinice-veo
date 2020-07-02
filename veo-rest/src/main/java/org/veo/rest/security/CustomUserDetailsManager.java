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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Component;

/**
 * In-memory implementation of a custom user details service for testing.
 *
 * It returns ApplicationUser objects which hold information about the Client-ID
 * in addition to credentials, enabled-status etc.
 *
 * In production, this should use a user repository or even better a separate
 * authorization (micro)service.
 */
@Component
public class CustomUserDetailsManager extends InMemoryUserDetailsManager {

    // This would be a repository or service in production:
    private Map<String, ApplicationUser> users = new HashMap<String, ApplicationUser>();

    public CustomUserDetailsManager(Collection<ApplicationUser> appUsers) {
        super(appUsers.stream()
                      .map(u -> (UserDetails) u)
                      .collect(Collectors.toList()));
        appUsers.stream()
                .forEach(this::storeUser);

    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        ApplicationUser user = users.get(username);
        if (user == null)
            throw new UsernameNotFoundException(username);
        return user;
    }

    private void storeUser(ApplicationUser user) {
        users.put(user.getUsername(), user);
    }
}