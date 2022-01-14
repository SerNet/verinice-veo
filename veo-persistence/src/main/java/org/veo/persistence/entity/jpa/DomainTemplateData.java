/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.TypeDef;

import com.vladmihalcea.hibernate.type.json.JsonType;

import org.veo.core.entity.Catalog;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.definitions.ElementTypeDefinition;
import org.veo.core.entity.riskdefinition.RiskDefinition;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity(name = "domaintemplate")
@TypeDef(name = "json", typeClass = JsonType.class, defaultForType = Map.class)
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true)
@Data
public class DomainTemplateData extends IdentifiableVersionedData
        implements DomainTemplate, Nameable {
    @Id
    @ToString.Include
    private String dbId;

    @NotNull
    @Column(name = "name")
    @ToString.Include
    private String name;

    @Column(name = "abbreviation")
    private String abbreviation;

    @Column(name = "description", length = Nameable.DESCRIPTION_MAX_LENGTH)
    private String description;

    @NotNull
    @Column(name = "authority")
    @ToString.Include
    private String authority;

    @NotNull
    @Column(name = "templateversion")
    @ToString.Include
    private String templateVersion;

    @OneToMany(cascade = CascadeType.ALL,
               orphanRemoval = true,
               targetEntity = CatalogData.class,
               mappedBy = "domainTemplate",
               fetch = FetchType.EAGER)
    @Valid
    private Set<Catalog> catalogs = new HashSet<>();

    @NotNull
    @Column(name = "revision")
    @ToString.Include
    private String revision;

    @Override
    public boolean addToCatalogs(Catalog aCatalog) {
        aCatalog.setDomainTemplate(this);
        return catalogs.add(aCatalog);
    }

    @Override
    public void removeFromCatalog(Catalog aCatalog) {
        catalogs.remove(aCatalog);
        aCatalog.setDomainTemplate(null);
    }

    @OneToMany(cascade = CascadeType.ALL,
               orphanRemoval = true,
               targetEntity = ElementTypeDefinitionData.class,
               mappedBy = "owner")
    private Set<ElementTypeDefinition> elementTypeDefinitions = new HashSet<>();

    @Column(columnDefinition = "jsonb")
    private Map<String, RiskDefinition> riskDefinitions = new HashMap<>();

    public void setElementTypeDefinitions(Set<ElementTypeDefinition> elementTypeDefinitions) {
        elementTypeDefinitions.forEach(d -> ((ElementTypeDefinitionData) d).setOwner(this));
        this.elementTypeDefinitions.clear();
        this.elementTypeDefinitions.addAll(elementTypeDefinitions);
    }
}
