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

import org.veo.core.entity.state.SystemMessageState;

public interface SystemMessage extends Entity, SystemMessageState {
  String SINGULAR_TERM = "message";
  String PLURAL_TERM = "messages";

  public enum MessageLevel {
    INFO,
    WARNING,
    URGENT
  }

  public void setId(Long id);

  public void setMessage(TranslatedText message);

  public void setPublication(Instant publication);

  public void setEffective(Instant effective);

  public void setLevel(MessageLevel level);

  @Override
  default Class<? extends Entity> getModelInterface() {
    return UserConfiguration.class;
  }

  @Override
  default String getModelType() {
    return SINGULAR_TERM;
  }
}
