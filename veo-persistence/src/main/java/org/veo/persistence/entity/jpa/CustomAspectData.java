/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2019  Urs Zeidler.
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
package org.veo.persistence.entity.jpa;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import com.vladmihalcea.hibernate.type.json.JsonType;

import org.veo.core.entity.CustomAspect;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;

import lombok.Data;
import lombok.ToString;

@Entity(name = "custom_aspect")
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
@TypeDef(name = "json", typeClass = JsonType.class)
public class CustomAspectData implements CustomAspect {

    @Id
    @ToString.Include
    private String dbId = UUID.randomUUID()
                              .toString();

    @NotNull
    @ToString.Include
    private String type;

    @ManyToOne(fetch = FetchType.LAZY,
               targetEntity = ElementData.class,
               // 'links' are also custom aspects, saved in the same table but mapped by
               // 'source'
               // column, due to the single-table inheritance mapping used here.
               // 'owner' must therefore be nullable for these entities:
               optional = true)
    private Element owner;

    @ManyToMany(targetEntity = DomainData.class)
    final protected Set<Domain> domains = new HashSet<>();

    @Type(type = "json")
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> attributes = new HashMap<>();

    /**
     * Add the given Domain to the collection domains.
     *
     * @return true if added
     */
    public boolean addToDomains(Domain aDomain) {
        return this.domains.add(aDomain);
    }

    /**
     * Remove the given Domain from the collection domains.
     *
     * @return true if removed
     */
    public boolean removeFromDomains(Domain aDomain) {
        return this.domains.remove(aDomain);
    }

    @Override
    public void setDomains(Set<Domain> newDomains) {
        domains.clear();
        domains.addAll(newDomains);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (this == o)
            return true;

        if (!(o instanceof CustomAspectData))
            return false;

        CustomAspectData other = (CustomAspectData) o;
        // Transient (unmanaged) entities have an ID of 'null'. Only managed
        // (persisted and detached) entities have an identity. JPA requires that
        // an entity's identity remains the same over all state changes.
        // Therefore a transient entity must never equal another entity.
        String dbId = getDbId();
        return dbId != null && dbId.equals(other.getDbId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
