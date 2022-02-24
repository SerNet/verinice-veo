/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.validation.Valid;

import org.veo.core.entity.Domain;
import org.veo.core.entity.DomainTemplate;
import org.veo.core.entity.Element;
import org.veo.core.entity.Identifiable;
import org.veo.core.entity.Key;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.Scope;
import org.veo.core.entity.ScopeRisk;
import org.veo.core.entity.risk.RiskDefinitionRef;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Entity(name = "scope")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public class ScopeData extends RiskAffectedData<Scope, ScopeRisk> implements Scope {

    @ManyToMany(targetEntity = ElementData.class,
                cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "scope_members",
               joinColumns = @JoinColumn(name = "scope_id"),
               inverseJoinColumns = @JoinColumn(name = "member_id"))
    @Valid
    @Getter
    private final Set<Element> members = new HashSet<>();

    @Override
    public Class<? extends Identifiable> getModelInterface() {
        return Scope.class;
    }

    public boolean removeMemberById(Key<UUID> id) {
        return members.removeIf(compositeEntity -> compositeEntity.getId()
                                                                  .equals(id));
    }

    @Override
    ScopeRisk createRisk(Scenario scenario) {
        return new ScopeRiskData(this, scenario);
    }

    @OneToMany(cascade = CascadeType.ALL,
               orphanRemoval = true,
               targetEntity = ScopeRiskValuesAspectData.class,
               mappedBy = "owner",
               fetch = FetchType.LAZY)
    @Valid
    private final Set<ScopeRiskValuesAspectData> riskValuesAspects = new HashSet<>();

    @Override
    public void setRiskDefinition(DomainTemplate domain, RiskDefinitionRef riskDefinition) {
        var aspect = findAspectByDomain(riskValuesAspects, domain).orElseGet(() -> {
            var newAspect = new ScopeRiskValuesAspectData(domain, this);
            riskValuesAspects.add(newAspect);
            return newAspect;
        });
        aspect.setRiskDefinitionRef(riskDefinition);
    }

    public Optional<RiskDefinitionRef> getRiskDefinition(DomainTemplate domain) {
        return findAspectByDomain(riskValuesAspects,
                                  domain).map(ScopeRiskValuesAspectData::getRiskDefinitionRef);
    }

    @Override
    public void transferToDomain(Domain oldDomain, Domain newDomain) {
        findAspectByDomain(riskValuesAspects, oldDomain).ifPresent(a -> a.setDomain(newDomain));
        super.transferToDomain(oldDomain, newDomain);
    }
}
