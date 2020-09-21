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
package org.veo.adapter.presenter.api.common;

import org.veo.core.entity.Client;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.util.ModelSwitch;

public class ToDisplayNameSwitch extends ModelSwitch<String> {

    @Override
    public String caseNameable(Nameable object) {
        return object.getDisplayName();
    }

    @Override
    public String caseClient(Client object) {
        return object.getName();
    }
}
