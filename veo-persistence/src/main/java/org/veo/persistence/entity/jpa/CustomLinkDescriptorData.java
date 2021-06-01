/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import org.veo.core.entity.CustomLink;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Element;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity(name = "customlinkdescriptor")
@ToString(onlyExplicitlyIncluded = true)
@Getter
@Setter
@RequiredArgsConstructor
public class CustomLinkDescriptorData implements CustomLink {
    @Id
    @ToString.Include
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private String dbId;

    @ToString.Include
    private String type;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ElementData.class, optional = true)
    @JoinColumn(name = "target_id")
    private Element target;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ElementData.class, optional = true)
    @JoinColumn(name = "source_id")
    private Element source;

    @Type(type = "json")
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> attributes = new HashMap<>();

    @Override
    public boolean addToDomains(Domain aDomain) {
        return false;
    }

    @Override
    public boolean removeFromDomains(Domain aDomain) {
        return false;
    }

    @Override
    public Set<Domain> getDomains() {
        return Collections.emptySet();
    }

    @Override
    public void setDomains(Set<Domain> aDomains) {
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;

        if (this == o)
            return true;

        if (!(o instanceof CustomLinkDescriptorData))
            return false;

        CustomLinkDescriptorData other = (CustomLinkDescriptorData) o;
        // Transient (unmanaged) entities have an ID of 'null'. Only managed
        // (persisted and detached) entities have an identity. JPA requires that
        // an entity's identity remains the same over all state changes.
        // Therefore, a transient entity must never equal another entity.
        String dbId = getDbId();
        return dbId != null && dbId.equals(other.getDbId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
