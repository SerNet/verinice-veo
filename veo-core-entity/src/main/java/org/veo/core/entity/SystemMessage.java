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

import java.time.Instant;
import java.util.Set;

import org.veo.core.entity.state.SystemMessageState;

public interface SystemMessage extends Entity, SystemMessageState {
  String SINGULAR_TERM = "message";
  String PLURAL_TERM = "messages";

  enum MessageLevel {
    INFO,
    WARNING,
    URGENT
  }

  void setId(Long id);

  void setMessage(TranslatedText message);

  void setPublication(Instant publication);

  void setEffective(Instant effective);

  void setLevel(MessageLevel level);

  void setTags(Set<String> tags);

  @Override
  default Class<? extends Entity> getModelInterface() {
    return UserConfiguration.class;
  }

  @Override
  default String getModelType() {
    return SINGULAR_TERM;
  }
}
