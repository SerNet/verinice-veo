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
 *
 * Contributors:
 *     Alexander Koderman <ak@sernet.de> - initial API and implementation
 ******************************************************************************/
package org.veo.persistence.entity.jpa;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;

@Data

@Entity
@IdClass(SimpleKey.class)
public class ClientData {

    @NotNull
    @EmbeddedId
    private SimpleKey uuid;

    @Column(name = "name")
    @NotBlank(message="The name of a client must not be blank.")
    private String name;
    
   
    /*
     *  Domains are value objects, not entities.
     *  However, they are not embedded here, but referenced to a join table. 
     */
    @ElementCollection(targetClass = DomainValue.class)
    @JoinTable(name = "domains") 
    @JoinColumn(name = "client_uuid", referencedColumnName = "uuid")
    @Size(min=1, max=1000000, message="A client must be working with at least one domain.")
    private Set<DomainValue> domains;

}
