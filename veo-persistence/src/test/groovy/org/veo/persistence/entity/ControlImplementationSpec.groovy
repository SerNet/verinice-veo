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
package org.veo.persistence.entity

import org.veo.core.entity.Control
import org.veo.core.entity.EntityType
import org.veo.core.entity.Key
import org.veo.core.entity.RiskAffected
import org.veo.core.entity.Unit
import org.veo.core.entity.compliance.ImplementationStatus
import org.veo.core.entity.compliance.Origination
import org.veo.test.VeoSpec

class ControlImplementationSpec extends VeoSpec {

    public static final String IMPL_STATEMENT = "I am SO implemented."

    Unit unit
    Control control
    Control controlStatement1
    Control controlStatement2

    def setup() {
        def client = newClient {}
        unit = newUnit(client)

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
    }

    def "implement a control twice for the same #type"() {
        given: "a control that is implemented for a #type"
        def elmt = makeType(type)

        def impl = elmt.implementControl(control)
        impl.setDescription(IMPL_STATEMENT)

        when: "the user tries to implement the control a second time"
        def impl2 = elmt.implementControl(control)

        then: "the existing implementation is returned"
        impl2.getDescription() == IMPL_STATEMENT
        impl2 == impl

        where:
        type << EntityType.RISK_AFFECTED_TYPES*.singularTerm
    }

    def "implement a control for a #type"() {
        when: "a control is associated with a #type"
        def elmt = makeType(type)
        def implementation = elmt.implementControl(control)
        def implementations = elmt.getControlImplementations()

        then: "a controlImplementation was created"
        implementations.size() == 1
        with(implementations.first()) {
            it.owner == elmt
            it.control.idAsString == this.control.idAsString
            responsible == null
            description == null
        }
        implementations ==~ [implementation]

        and: "requirementImplementations are created for the control and its parts"
        with(implementations.first()) {
            requirementImplementations.size() == 3
        }
        def refs = implementations.first().requirementImplementations.toSorted()
        refs.each {
            assert it.keyRef != null
        }

        def reqImpls = elmt.getRequirementImplementations()
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

    def "disassociate a control from a #type"() {
        given: "a #type with multiple controlImplementations"
        def elmt = makeType(type)
        def person = newPerson(unit) {id = Key.newUuid()}
        def control2 = newControl(unit) {id = Key.newUuid()}
        def control3 = newControl(unit) {id = Key.newUuid()}

        when: "a requirement is changed"
        elmt.implementControl(control2)
        elmt.implementControl(control3)
        def reqImpl = elmt.implementControl(control).requirementImplementations.first()
        def changedReq = elmt.getRequirementImplementation(reqImpl)
        changedReq.setImplementationStatement(IMPL_STATEMENT)
        changedReq.setResponsible(person)
        def changedControl = changedReq.getControl()
        def createdRIs = elmt.requirementImplementations.clone()

        then: "the change is visible"
        elmt.controlImplementations.size() == 3
        elmt.requirementImplementations.size() == 5
        elmt.requirementImplementations ==~ createdRIs
        IMPL_STATEMENT in elmt.requirementImplementations*.implementationStatement
        person.idAsString in elmt.requirementImplementations*.responsible.idAsString

        when: "one controlAssociation is removed from the #type"
        elmt.disassociateControl(control)

        then: "it is no longer listed"
        elmt.controlImplementations*.control ==~ [control2, control3]

        and: "all unmodified requirementImplementations were removed"
        elmt.requirementImplementations.size() == 3
        def remainingRIs = elmt.requirementImplementations.findAll {
            it.control in  [
                changedControl,
                control2,
                control3
            ]
        }
        elmt.requirementImplementations ==~ remainingRIs

        where:
        type << EntityType.RISK_AFFECTED_TYPES*.singularTerm
    }

    def "disassociate controls with a shared RI"() {
        given:
        def asset = newAsset(unit)
        def parentControl1 = newControl(unit) {id = Key.newUuid()}
        def parentControl2 = newControl(unit) {id = Key.newUuid()}
        def childControl = newControl(unit) {id = Key.newUuid()}

        asset.implementControl(parentControl1).addRequirement(childControl)
        asset.implementControl(parentControl2).addRequirement(childControl)

        when:
        asset.disassociateControl(parentControl1)

        then:
        asset.controlImplementations*.control ==~ [parentControl2]
        asset.requirementImplementations*.control ==~ [parentControl2, childControl]

        when:
        asset.disassociateControl(parentControl2)

        then:
        asset.controlImplementations.empty
        asset.requirementImplementations.empty
    }

    def "existing implemented requirements are recognized for a #type"() {
        given: "a #type implementing control A.1.1"
        def elmt = makeType(type)
        def controlImplementation = elmt.implementControl(controlStatement1)

        when: "an implemented requirement is described"
        def reqImplRef = controlImplementation.getRequirementImplementations().first()
        elmt.getRequirementImplementation(reqImplRef).setImplementationStatement(IMPL_STATEMENT)

        then: "the requirementImplementation is present"
        elmt.getRequirementImplementation(reqImplRef).getImplementationStatement() == IMPL_STATEMENT
        def reqImplId = elmt.getRequirementImplementation(reqImplRef).getId()

        when: "the parent control A.1 is implemented"
        elmt.implementControl(control)

        then: "all remaining requirementImplementations were added"
        def reqImpls = elmt.requirementImplementations.findAll {
            it.control in  [
                control,
                controlStatement1,
                controlStatement2
            ]
        }
        reqImpls.size() == 3

        and: "the modified requirementImplementation is present"
        def modified = elmt.getRequirementImplementation(reqImplRef)
        modified != null

        and: "it's values were not touched"
        modified.implementationStatement == IMPL_STATEMENT
        modified.getId() == reqImplId

        where:
        type << EntityType.RISK_AFFECTED_TYPES*.singularTerm
    }

    RiskAffected makeType(String type) {
        return "new${type.capitalize()}"(unit)
    }
}
