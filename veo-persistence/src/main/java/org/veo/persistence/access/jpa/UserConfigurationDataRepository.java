/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler
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
package org.veo.persistence.access.jpa;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.veo.persistence.entity.jpa.UserConfigurationData;

public interface UserConfigurationDataRepository
    extends JpaRepository<UserConfigurationData, String> {

  @Query(
      "select distinct e from #{#entityName} as e "
          + "where e.client.dbId=?1 and e.userName=?2 and e.applicationId=?3")
  Optional<UserConfigurationData> findUserConfiguration(
      String clientId, String username, String applicationId);

  @Query("select e from #{#entityName} as e where e.client.dbId=?1")
  Set<UserConfigurationData> findUserConfigurationsByClient(String clientId);

  @Query("select count(e) from #{#entityName} as e where e.client.dbId=?1 and e.userName=?2 ")
  int countUserConfigurations(String clientId, String username);

  @Query(
      "select e.applicationId from #{#entityName} as e where e.client.dbId=?1 and e.userName=?2 ")
  Set<String> findAllKeysByUser(String clientId, String userName);
}
