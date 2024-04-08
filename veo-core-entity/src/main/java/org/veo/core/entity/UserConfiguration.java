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
package org.veo.core.entity;

import java.util.Map;

public interface UserConfiguration extends ClientOwned {
  String SINGULAR_TERM = "user-configuration";
  String PLURAL_TERM = "user-configurations";

  void setClient(Client client);

  void setConfiguration(Map<String, Object> configuration);

  void setApplicationId(String applicationId);

  void setUserName(String userName);

  Map<String, Object> getConfiguration();

  Client getClient();

  String getApplicationId();

  String getUserName();

  boolean isPersisted();

  @Override
  default Class<? extends Entity> getModelInterface() {
    return UserConfiguration.class;
  }

  @Override
  default String getModelType() {
    return SINGULAR_TERM;
  }
}
