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

import java.util.Date;
import java.util.Set;


import org.veo.adapter.presenter.api.common.DateTimeConversion;
import org.veo.adapter.presenter.api.dto.ProcessDto;
import org.veo.core.entity.DomainException;
import org.veo.core.entity.Key;
import org.veo.core.usecase.process.CreateProcessUseCase;
import org.veo.core.usecase.process.CreateProcessUseCase.InputData;

/**
 * Map between the request DTO received from a client and the input expected by
 * the data source. (This is not needed for simple input data.)
 * 
 * The request DTO is not mapped to a process entity in this case because a new process is 
 * created from the input values.
 * 
 * The newly created process is returned by the use case. 
 * @see CreateProcessOutputMapper
 *
 */
public final class CreateProcessInputMapper {
    
    public static CreateProcessUseCase.InputData map(ProcessDto dto, String timezoneId) throws DomainException {
        DateTimeConversion timeConversion = new DateTimeConversion(timezoneId);
        Date validFrom = timeConversion.getConvertedDate(dto.getValidFrom()); 
        Date validUntil = timeConversion.getConvertedDate(dto.getValidUntil());

        return new InputData(
                Key.uuidFrom(dto.getId()), 
                dto.getName(), 
                Key.uuidsFrom(Set.of(dto.getAssetIDs())),
                validFrom,
                validUntil
          );
    }
}
