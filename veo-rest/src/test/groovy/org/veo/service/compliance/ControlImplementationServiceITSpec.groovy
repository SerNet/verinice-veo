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
package org.veo.service.compliance

import static org.veo.core.entity.compliance.ImplementationStatus.YES

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Bean
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Transactional

import org.veo.core.VeoSpringSpec
import org.veo.core.entity.Client
import org.veo.core.entity.Control
import org.veo.core.entity.Domain
import org.veo.core.entity.EntityType
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.entity.compliance.ReqImplRef
import org.veo.core.entity.event.ControlPartsChangedEvent
import org.veo.listeners.ControlChangeListener
import org.veo.persistence.access.jpa.AssetDataRepository
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.ControlDataRepository
import org.veo.persistence.access.jpa.ProcessDataRepository
import org.veo.persistence.access.jpa.ScopeDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository
import org.veo.service.ControlImplementationService

@WithUserDetails("user@domain.example")
@ContextConfiguration(classes = Config.class)
@Transactional
class ControlImplementationServiceITSpec extends VeoSpringSpec {

    @Autowired ApplicationEventPublisher publisher
    @Autowired ControlChangeListener listener

    /**
     * This test triggers change events itself at certain points to observe the resulting changes.
     * These events are otherwise published by the {@code EntityStateMapper}.
     */
    void publishPartsChanged(Control control, Set<Control> oldParts) {
        publisher.publishEvent(new ControlPartsChangedEvent(control, oldParts))
    }

    static class Config {
        @Bean
        ControlChangeListener listener(ControlImplementationService service) {
            return new ControlChangeListener(service)
        }
    }

    Client client
    Domain domain
    Unit unit
    Control control_A_1
    Control control_A_1_1
    Control control_A_1_2

    @Autowired AssetDataRepository assetRepo
    @Autowired ProcessDataRepository processRepo
    @Autowired ScopeDataRepository scopeRepo
    @Autowired UnitDataRepository unitRepo
    @Autowired ClientDataRepository clientRepo
    @Autowired ControlDataRepository controlRepo

    def setup() {
        client = clientRepo.save(newClient{
            id = Key.newUuid()
            newDomain(it)
        })
        domain = client.domains.first()
        this.unit = newUnit(client)
        unit = unitRepo.save(unit)

        this.control_A_1 = newControl(unit, {
            name = "Control A.1"
        })
        this.control_A_1_1 = newControl(unit, {
            name = "Control A.1.1"
        })
        this.control_A_1_2 = newControl(unit, {
            name = "Control A.1.2"
        })
        control_A_1.addParts([
            control_A_1_1,
            control_A_1_2
        ] as Set)

        control_A_1 = controlRepo.save(control_A_1)
        control_A_1_1 = controlRepo.save(control_A_1_1)
        control_A_1_2 = controlRepo.save(control_A_1_2)
    }

    def "changes to the structure of a control are reflected with a #type"() {
        given: "two #type elements"
        def elmt1 = makeType(type)
        def elmt2 = makeType(type)
        elmt1 = controlRepo.save(elmt1)
        elmt2 = controlRepo.save(elmt2)

        when: "a control is implemented by both"
        elmt1.implementControl(control_A_1)
        elmt2.implementControl(control_A_1)
        elmt1 = controlRepo.save(elmt1)
        elmt2 = controlRepo.save(elmt2)

        then: "the implementation of the control and its parts is documented"
        elmt1.controlImplementations.size() == 1
        elmt2.controlImplementations.size() == 1

        elmt1.requirementImplementations.size() == 3
        elmt2.requirementImplementations.size() == 3

        elmt1.requirementImplementations*.control ==~ [
            control_A_1,
            control_A_1_1,
            control_A_1_2
        ]
        elmt2.requirementImplementations*.control ==~ [
            control_A_1,
            control_A_1_1,
            control_A_1_2
        ]

        when: "a new control is added as a part to a control"
        def oldParts = control_A_1.getPartsRecursively()
        def newControl = controlRepo.save(newControl(unit))
        control_A_1.addPart(newControl)
        publishPartsChanged(control_A_1, oldParts)

        control_A_1 = controlRepo.findById(control_A_1.idAsUUID).orElseThrow()
        elmt1 = repoFor(type).findById(elmt1.idAsUUID).orElseThrow()
        elmt2 = repoFor(type).findById(elmt1.idAsUUID).orElseThrow()

        then: "new requirementImplementations were created in all elements"
        elmt1.controlImplementations.size() == 1
        elmt2.controlImplementations.size() == 1

        elmt1.requirementImplementations.size() == 4
        elmt2.requirementImplementations.size() == 4

        elmt1.requirementImplementations*.control ==~ [
            control_A_1,
            control_A_1_1,
            control_A_1_2,
            newControl
        ]
        elmt2.requirementImplementations*.control ==~ [
            control_A_1,
            control_A_1_1,
            control_A_1_2,
            newControl
        ]

        when: "the requirement for a part is edited"
        def modifiedReqImpl1 = elmt1
                .getRequirementImplementations().find { it.control == control_A_1_2}
        modifiedReqImpl1.setStatus(YES)

        def modifiedReqImpl2 = elmt2
                .getRequirementImplementations().find { it.control == control_A_1_2}
        modifiedReqImpl2.setStatus(YES)

        and: "that part is removed from the main control"
        oldParts = control_A_1.getPartsRecursively()
        control_A_1.removePart(control_A_1_2)
        publishPartsChanged(control_A_1, oldParts)
        elmt1 = repoFor(type).findById(elmt1.idAsUUID).orElseThrow()
        elmt2 = repoFor(type).findById(elmt1.idAsUUID).orElseThrow()

        then: "the control implementation itself is still present"
        elmt1.controlImplementations.size() == 1
        elmt2.controlImplementations.size() == 1

        and: "the part's requirement was removed from the control implementation"
        elmt1.controlImplementations.first().requirementImplementations.size() == 3
        elmt2.controlImplementations.first().requirementImplementations.size() == 3

        !elmt1.controlImplementations.first().requirementImplementations.find{it.references(modifiedReqImpl1)}
        !elmt2.controlImplementations.first().requirementImplementations.find{it.references(modifiedReqImpl2)}

        and: "the modified requirementImplementations are however still documented for the #type"
        elmt1.requirementImplementations.size() == 4
        elmt2.requirementImplementations.size() == 4

        def reqImplRef1 = ReqImplRef.from(modifiedReqImpl1)
        def reqImplRef2 = ReqImplRef.from(modifiedReqImpl2)
        elmt1.getRequirementImplementation(reqImplRef1) != null
        elmt1.getRequirementImplementation(reqImplRef1).status == YES
        elmt2.getRequirementImplementation(reqImplRef2) != null
        elmt2.getRequirementImplementation(reqImplRef2).status == YES

        when: "the part control is added again"
        oldParts = control_A_1.getPartsRecursively()
        control_A_1.addPart(control_A_1_2)
        publishPartsChanged(control_A_1, oldParts)
        elmt1 = repoFor(type).findById(elmt1.idAsUUID).orElseThrow()
        elmt2 = repoFor(type).findById(elmt1.idAsUUID).orElseThrow()

        then: "the retained modified requirementImplementation is acknowledged and reused"
        elmt1.controlImplementations.size() == 1
        elmt2.controlImplementations.size() == 1

        elmt1.controlImplementations.first().requirementImplementations.size() == 4
        elmt2.controlImplementations.first().requirementImplementations.size() == 4

        elmt1.controlImplementations.first().requirementImplementations.contains(reqImplRef1)
        elmt2.controlImplementations.first().requirementImplementations.contains(reqImplRef2)

        elmt1.getRequirementImplementation(reqImplRef1) != null
        elmt1.getRequirementImplementation(reqImplRef1).status == YES
        elmt2.getRequirementImplementation(reqImplRef2) != null
        elmt2.getRequirementImplementation(reqImplRef2).status == YES

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
