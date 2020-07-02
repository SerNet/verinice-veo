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
package org.veo.persistence.entity.jpa;

import javax.persistence.Embeddable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;

/**
 * Domains are value objects. They do not have an ID. They may hold references
 * to other entities.
 */
@Value
@AllArgsConstructor
@With

@Embeddable
public class DomainValue {

    // Constructor for ModelMapper:
    DomainValue() {
        this.name = "";
        this.authority = "";
        this.version = "";
    }

    @NotNull
    @NotBlank(message = "The name of a domain must not be blank.")
    @Size(max = 255)
    String name;

    @NotNull
    @NotBlank(message = "The authority of a domain must not be blank.")
    @Size(max = 255)
    String authority;

    @NotNull
    @NotBlank(message = "The version of a domain must not be blank.")
    @Size(max = 255)
    String version;

}
