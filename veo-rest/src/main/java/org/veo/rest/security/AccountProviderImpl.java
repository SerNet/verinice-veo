/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Jonas Jordan
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
package org.veo.rest.security;

import java.util.Optional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import org.veo.core.entity.Account;
import org.veo.core.entity.AccountProvider;
import org.veo.core.repository.ClientReadOnlyRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccountProviderImpl implements AccountProvider {

  private final ClientReadOnlyRepository clientRepository;

  @Override
  public Account getCurrentUserAccount() {
    var user =
        ApplicationUser.authenticatedUser(
            SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    var client =
        Optional.ofNullable(user.getClientId()).flatMap(clientRepository::findById).orElse(null);

    return new AccountImpl(user.isAdmin(), client);
  }
}
