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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.hibernate.annotations.Formula;
import org.hibernate.annotations.GenericGenerator;

import org.veo.core.entity.CatalogItem;
import org.veo.core.entity.CustomLink;
import org.veo.core.entity.CustomProperties;
import org.veo.core.entity.Domain;
import org.veo.core.entity.EntityLayerSupertype;
import org.veo.core.entity.Nameable;
import org.veo.core.entity.Unit;
import org.veo.core.entity.aspects.Aspect;
import org.veo.core.entity.aspects.SubTypeAspect;
import org.veo.persistence.entity.jpa.validation.HasOwnerOrContainingCatalogItem;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

@Entity(name = "entitylayersupertype")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Data
@NamedEntityGraph(name = EntityLayerSupertypeData.FULL_AGGREGATE_GRAPH,
                  attributeNodes = { @NamedAttributeNode(value = "customAspects"),
                          @NamedAttributeNode(value = "domains"),
                          @NamedAttributeNode(value = "appliedCatalogItems"),
                          @NamedAttributeNode(value = "links"),
                          @NamedAttributeNode(value = "subTypeAspects") })
@HasOwnerOrContainingCatalogItem
public abstract class EntityLayerSupertypeData extends BaseModelObjectData
        implements NameableData, EntityLayerSupertype {

    public static final String FULL_AGGREGATE_GRAPH = "fullAggregateGraph";
    @Id
    @ToString.Include
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
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
    @Column(name = "designator")
    @ToString.Include
    @Pattern(regexp = "([A-Z]{3}-\\d+)|NO_DESIGNATOR")
    private String designator;

    @Column(name = "domains")
    @ManyToMany(targetEntity = DomainData.class, fetch = FetchType.LAZY)
    final private Set<Domain> domains = new HashSet<>();

    @Column(name = "links")
    @OneToMany(cascade = CascadeType.ALL,
               orphanRemoval = true,
               targetEntity = CustomLinkData.class,
               mappedBy = "source",
               fetch = FetchType.LAZY)
    final private Set<CustomLink> links = new HashSet<>();

    @Column(name = "customaspects")
    @OneToMany(cascade = CascadeType.ALL,
               orphanRemoval = true,
               targetEntity = CustomPropertiesData.class,
               mappedBy = "owner",
               fetch = FetchType.LAZY)
    final private Set<CustomProperties> customAspects = new HashSet<>();

    @Column(name = "sub_type_aspects")
    @OneToMany(cascade = CascadeType.ALL,
               orphanRemoval = true,
               targetEntity = SubTypeAspectData.class,
               mappedBy = "owner",
               fetch = FetchType.LAZY)
    private Set<SubTypeAspect> subTypeAspects = new HashSet<>();

    @ManyToMany(targetEntity = CatalogItemData.class, fetch = FetchType.LAZY)
    private Set<CatalogItem> appliedCatalogItems;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UnitData.class)
    @JoinColumn(name = "owner_id")
    private Unit owner;

    @OneToOne(fetch = FetchType.LAZY, targetEntity = CatalogItemData.class)
    @JoinColumn(name = "containing_catalog_item_id")
    private CatalogItem containingCatalogItem;

    @Formula("case when abbreviation is null then concat(designator,' ',name) else concat(designator,' ',abbreviation,' ',name) end")
    @Setter(AccessLevel.NONE)
    private String displayName;

    @Formula("length(designator)")
    @Setter(AccessLevel.NONE)
    private String designatorLength;

    protected <T extends Aspect> Optional<T> findAspectByDomain(Set<T> source, Domain domain) {
        return source.stream()
                     .filter(aspect -> aspect.getDomain() == domain)
                     .findFirst();
    }

    @Override
    public Optional<String> getSubType(Domain domain) {
        return findAspectByDomain(subTypeAspects, domain).map(SubTypeAspect::getSubType);
    }

    @Override
    public void setSubType(Domain domain, String subType) {
        var aspect = new SubTypeAspectData(domain, this, subType);
        subTypeAspects.remove(aspect);
        subTypeAspects.add(aspect);
    }

    public void setLinks(Set<CustomLink> newLinks) {
        links.clear();
        newLinks.forEach(l -> l.setSource(this));
        links.addAll(newLinks);
    }

    public void setCustomAspects(Set<CustomProperties> aCustomAspects) {
        this.customAspects.clear();
        aCustomAspects.forEach(aspect -> {
            if (aspect instanceof CustomPropertiesData) {
                ((CustomPropertiesData) aspect).setOwner(this);
            }
        });
        this.customAspects.addAll(aCustomAspects);
    }

    public void setDomains(Set<Domain> newDomains) {
        this.domains.clear();
        this.domains.addAll(newDomains);
    }

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

    /**
     * Add the given CustomLink to the collection of links. Manages the association
     * between EntityLayerSupertype and CustomLink.
     *
     * @return true if the link was successfully added
     */
    public boolean addToLinks(CustomLink aCustomLink) {
        aCustomLink.setSource(this);
        return this.links.add(aCustomLink);
    }

    /**
     * Remove the given CustomLink from the collection links.
     *
     * @return true if removed
     */
    @Override
    public boolean removeFromLinks(CustomLink aCustomLink) {
        aCustomLink.setSource(null);
        return this.links.remove(aCustomLink);
    }

    /**
     * Add the given CustomProperties to the collection customAspects.
     *
     * @return true if added
     */
    public boolean addToCustomAspects(CustomProperties aCustomProperties) {
        if (aCustomProperties instanceof CustomPropertiesData) {
            ((CustomPropertiesData) aCustomProperties).setOwner(this);
        }
        return this.customAspects.add(aCustomProperties);
    }

    /**
     * Remove the given CustomProperties from the collection customAspects.
     *
     * @return true if removed
     */
    public boolean removeFromCustomAspects(CustomProperties aCustomProperties) {
        if (aCustomProperties instanceof CustomPropertiesData) {
            CustomPropertiesData propertiesData = (CustomPropertiesData) aCustomProperties;
            propertiesData.setOwner(null);
        }
        return this.customAspects.remove(aCustomProperties);
    }

    @Transient
    @Override
    public String getDisplayName() {
        return displayName;

    }
}
