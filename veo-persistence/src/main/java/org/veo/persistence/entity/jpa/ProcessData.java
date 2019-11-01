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

import java.time.Instant;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.veo.core.entity.process.Process;

@Entity(name="process")
@Table(name="processes")
public class ProcessData extends EntityLayerSupertypeData {

 
    private String name;

    /* not modelled from asset's side - will be replaced by Aspect
    @ManyToMany
    * @JoinTable(name = "process_asset", 
        joinColumns = { @JoinColumn(name="processId", referencedColumnName="uuid") }, 
        inverseJoinColumns = {@JoinColumn(name="assetId", referencedColumnName="uuid")})
        */
    @OneToMany(
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.EAGER,
            orphanRemoval=true,
            mappedBy="process"
    )
    private Set<AssetData> assets;
    

    
    public static ProcessData from(Process process) {
        // map fields
        return new ProcessData();
    }
    
    public Process toProcess() {
        return new ();
    }
}
