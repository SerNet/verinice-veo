/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2023  Alexander Koderman
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
package org.veo.persistence.access

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.entity.Client
import org.veo.core.entity.Control
import org.veo.core.entity.Domain
import org.veo.core.entity.EntityType
import org.veo.core.entity.Unit
import org.veo.core.entity.compliance.ImplementationStatus
import org.veo.core.entity.compliance.Origination
import org.veo.persistence.access.jpa.AssetDataRepository
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.ControlDataRepository
import org.veo.persistence.access.jpa.ControlImplementationDataRepository
import org.veo.persistence.access.jpa.ProcessDataRepository
import org.veo.persistence.access.jpa.RequirementImplementationDataRepository
import org.veo.persistence.access.jpa.ScopeDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository
import org.veo.persistence.entity.jpa.AbstractJpaSpec

class ControlImplementationITSpec extends AbstractJpaSpec {

    @Autowired RequirementImplementationDataRepository requirementImplementationRepo
    @Autowired ControlImplementationDataRepository controlImplementationRepo

    @Autowired AssetDataRepository assetRepo
    @Autowired ProcessDataRepository processRepo
    @Autowired ScopeDataRepository scopeRepo
    @Autowired UnitDataRepository unitRepo
    @Autowired ClientDataRepository clientRepo
    @Autowired ControlDataRepository controlRepo

    @Autowired
    TransactionTemplate txTemplate

    Client client
    Domain domain
    Unit unit
    Control control
    Control controlStatement1
    Control controlStatement2

    def setup() {
        client = clientRepo.save(newClient{
            newDomain(it)
        })
        domain = client.domains.first()
        unit = txTemplate.execute{unitRepo.save(newUnit(client))}

        control = newControl(unit, {
            name = "Control A.1"
        })
        controlStatement1 = newControl(unit, {
            name = "Control A.1.1"
        })
        controlStatement2 = newControl(unit, {
            name = "Control A.1.2"
        })
        control.addParts([
            controlStatement1,
            controlStatement2
        ] as Set)

        control = txTemplate.execute{controlRepo.save(control)}
    }

    def "implement a control for a #type"() {
        when: "a control is asociated with a #type"
        def elmt = makeType(type)
        elmt = txTemplate.execute{repoFor(type).save(elmt) }
        def implementation = elmt.implementControl(control)
        elmt = txTemplate.execute{repoFor(type).save(elmt) }
        def implementations = elmt.getControlImplementations()

        then: "a controlImplementation was persisted"
        def storedImplementation = controlImplementationRepo
                .findByUUID(implementation.getId()).get()
        implementations ==~ [storedImplementation]

        and: "references to requirementImplementations are persisted"
        with(storedImplementation) {
            requirementImplementations.size() == 3
        }
        def refs = storedImplementation.requirementImplementations.toSorted()
        refs.each {
            assert it.keyRef != null
        }

        and: "requirementImplementations are persisted"
        def reqImpls = requirementImplementationRepo
                .findAllByUUID(refs.collect{UUID.fromString(it.keyRef)} as Set)
        reqImpls.size() == 3
        reqImpls.each { reqImpl ->
            with(reqImpl) {
                it.origin == elmt
                it.control != null
                origination == Origination.SYSTEM_SPECIFIC
                responsible == null
                status == ImplementationStatus.UNKNOWN
            }
        }
        reqImpls.collect{it.id.toString()} ==~ refs.collect{it.keyRef}

        where:
        type << EntityType.RISK_AFFECTED_TYPES*.singularTerm
    }

    def makeType(String type) {
        return "new${type.capitalize()}"(unit)
    }

    def repoFor(String type) {
        return "get${type.capitalize()}Repo"()
    }
}
