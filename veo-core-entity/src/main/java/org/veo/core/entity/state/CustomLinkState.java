/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Jochen Kemnade
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
package org.veo.core.entity.state;

import java.util.Map;

import org.veo.core.entity.Element;
import org.veo.core.entity.ref.ITypedId;

import lombok.EqualsAndHashCode;
import lombok.Value;

public interface CustomLinkState extends CustomAspectState {

  ITypedId<Element> getTarget();

  @Value
  @EqualsAndHashCode(callSuper = true)
  class CustomLinkStateImpl extends CustomAspectStateImpl implements CustomLinkState {

    private ITypedId<Element> target;

    public CustomLinkStateImpl(
        String type, Map<String, Object> attributes, ITypedId<Element> target) {
      super(type, attributes);
      this.target = target;
    }
  }
}
