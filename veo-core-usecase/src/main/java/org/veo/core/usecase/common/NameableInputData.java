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
package org.veo.core.usecase.common;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.veo.core.entity.Key;
import org.veo.core.usecase.UseCase;

@Data
@AllArgsConstructor
@NoArgsConstructor
/**
 * Common record class to p[ass nameable attributes from a DTO into a use case.
 */
public class NameableInputData implements UseCase.InputData {
    private Optional<Key<UUID>> id;

    private long version;
    private Instant validFrom;
    private Instant validUntil;
    private String name;
    private String abbreviation;
    private String description;
}
