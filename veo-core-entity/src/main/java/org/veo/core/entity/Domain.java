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
package org.veo.core.entity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Value;
import lombok.With;

/**
 * A domain is a field of expertise that defines attributes and behaviour
 * for other entities. The domain is an abstract term and has no identity of
 * its own. 
 * 
 * It is modeled as a value object. It can be referenced by other entities. 
 * It can also be referenced by clients. This determines all domains that
 * are relevant for the model of this client.
 * 
 * A domain may contain references to aspects or attributes
 *
 */

@Value
@With
public class Domain {

    // -> link to RiskDefinition
    
    @NotNull
    @NotBlank(message="The name of a domain must not be blank.")
    @Size(max=255)
    private final String name;
    
    @NotNull
    @NotBlank(message="The authority of a domain must not be blank.")
    @Size(max=255)
    private final String authority;
    
    @NotNull
    @NotBlank(message="The version of a domain must not be blank.")
    @Size(max=255)
    private final String version;
    
}
