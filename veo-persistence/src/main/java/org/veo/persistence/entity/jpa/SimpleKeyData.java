/*******************************************************************************
SimpleKeyData.java * Copyright (c) 2019 Alexander Koderman.
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

import java.io.Serializable;
import java.util.Objects;

// Does not need JPA annotations. Referenced as IdClass by entities.
public class SimpleKeyData implements Serializable{

    private static final long serialVersionUID = -3297691428729918495L;

    public SimpleKeyData(String uuid) {
        super();
        this.uuid = uuid;
    }

    protected String getUuid() {
        return uuid;
    }

    protected void setUuid(String uuid) {
        this.uuid = uuid;
    }

    private String uuid;

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SimpleKeyData other = (SimpleKeyData) obj;
        return Objects.equals(uuid, other.uuid);
    }
    
    
}
