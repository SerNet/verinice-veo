/*******************************************************************************
 * Copyright (c) 2019 Alexander Koderman.
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
package org.veo.adapter.presenter.api.process;

import java.util.UUID;

import org.veo.core.entity.Key;
import org.veo.core.entity.process.Process;

/**
 * Returns the id of the newly created process as output.
 *
 * @author akoderman
 *
 */
public final class CreateProcessOutputMapper {

    public static Key<UUID> map(Process process) {
        // TODO: move conversion to getprocess() - not needed here because only ID is
        // returned
        // DateTimeConversion timeConversion = new DateTimeConversion(timezoneId);
        // ProcessDto dto = ProcessDto.from(process);
        // dto.setValidFrom(timeConversion.formatDate(process.getValidFrom()));
        // dto.setValidUntil(timeConversion.formatDate(process.getValidUntil()));
        return process.getId();
    }
}
