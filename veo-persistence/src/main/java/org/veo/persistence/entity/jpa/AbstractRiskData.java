/*******************************************************************************
 * Copyright (c) 2020 Alexander Koderman.
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

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;
import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import org.veo.core.entity.AbstractRisk;
import org.veo.core.entity.Control;
import org.veo.core.entity.Domain;
import org.veo.core.entity.Person;
import org.veo.core.entity.Scenario;
import org.veo.core.entity.exception.ModelConsistencyException;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@MappedSuperclass
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(onlyExplicitlyIncluded = true)
@Data
@EntityListeners({ AuditingEntityListener.class })
@RequiredArgsConstructor
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public abstract class AbstractRiskData implements AbstractRisk {

    @Id
    @ToString.Include
    private String dbId;

    @ToString.Include
    @Version
    @Setter(AccessLevel.PACKAGE)
    private long version;

    @Column(name = "domains")
    @ManyToMany(targetEntity = DomainData.class, fetch = FetchType.LAZY)
    final private Set<Domain> domains = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ControlData.class)
    @JoinColumn(name = "control_id")
    @Setter(AccessLevel.PRIVATE)
    private Control mitigation;

    @NotNull
    @NonNull
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ScenarioData.class, optional = false)
    @EqualsAndHashCode.Include
    @Setter(AccessLevel.PROTECTED)
    private Scenario scenario;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = PersonData.class)
    @JoinColumn(name = "person_id")
    @Setter(AccessLevel.PRIVATE)
    private Person riskOwner;

    @CreatedDate
    private Date createdOn;

    @LastModifiedDate
    private Date lastModified;

    @LastModifiedBy
    private String lastModifiedBy;

    @CreatedBy
    private String createdBy;

    @Override
    public boolean addToDomains(Domain aDomain) {
        return domains.add(aDomain);
    }

    @Override
    public boolean removeFromDomains(Domain aDomain) {
        if (domains.size() < 2) {
            throw new ModelConsistencyException(
                    "Could not remove domain '%s': cannot remove last domain from risk.", aDomain);
        }
        return domains.remove(aDomain);
    }

    public void setDomains(@NonNull @NotEmpty Set<Domain> newDomains) {
        if (newDomains.size() < 1)
            throw new IllegalArgumentException("There must be at least one domain for the risk.");
        this.domains.clear();
        this.domains.addAll(newDomains);
    }

    @Override
    public AbstractRiskData mitigate(@Nullable Control control) {
        setMitigation(control);
        return this;
    }

    @Override
    public AbstractRiskData appoint(@Nullable Person riskOwner) {
        setRiskOwner(riskOwner);
        return this;
    }

}
