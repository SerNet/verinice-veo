/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Alexander Koderman.
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
package org.veo.core.usecase.domain

import org.veo.core.entity.Asset
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Process
import org.veo.core.entity.ProcessRisk
import org.veo.core.entity.Unit
import org.veo.core.entity.event.RiskAffectingElementChangeEvent
import org.veo.core.entity.profile.ProfileRef
import org.veo.core.repository.GenericElementRepository
import org.veo.core.repository.UnitRepository
import org.veo.core.service.DomainTemplateService
import org.veo.core.service.EventPublisher
import org.veo.core.usecase.DesignatorService
import org.veo.core.usecase.decision.Decider
import org.veo.service.ElementMigrationService

import spock.lang.Specification

class ProfileApplierSpec extends Specification {

    DomainTemplateService domainTemplateService = Mock()
    UnitRepository unitRepository = Mock()
    EventPublisher eventPublisher = Mock()
    GenericElementRepository genericElementRepository = Mock()
    Decider decider = Mock()
    ElementMigrationService elementMigrationService = Mock()
    DesignatorService designatorService= Mock()
    ElementBatchCreator elementBatchCreator = new ElementBatchCreator(genericElementRepository, eventPublisher, decider, elementMigrationService, designatorService)

    ProfileApplier profileApplier = new ProfileApplier(domainTemplateService, unitRepository, elementBatchCreator)

    def "apply a profile to a unit"() {
        given: "starting values for a unit"
        Client existingClient = Mock()
        Unit unit = Mock()
        Domain domain = Mock() {
            owner >> existingClient
        }
        ProfileRef profile = new ProfileRef("highProfile")

        Asset asset1 = Mock {
            getModelType() >> 'asset'
            getParts() >> []
            getLinks() >> []
            getDomains()  >> [domain]
            getRisks() >> []
            getOwningClient() >> Optional.of(existingClient)
        }
        Asset asset2 = Mock() {
            getModelType() >> 'asset'
            getParts() >> []
            getLinks() >> []
            getDomains()  >> [domain]
            getRisks() >> []
            getOwningClient() >> Optional.of(existingClient)
        }
        ProcessRisk risk = Mock {
        }

        Process process = Mock {
            getModelType() >> 'process'
            getParts() >> []
            getLinks() >> []
            getDomains()  >> [domain]
            getRisks() >> [risk]
            getOwningClient() >> Optional.of(existingClient)
        }

        when: "applying the profile"
        profileApplier.applyProfile(domain, profile, unit)

        then: "the unit is associated with the domain"
        1 * unit.addToDomains(domain)

        and: "profile elements are retrieved"
        1 * domainTemplateService.getProfileElements(domain, profile) >> [asset1, asset2, process]

        with(asset1) {
            1 * setOwner(unit)
            1 * elementMigrationService.migrate(it, domain)
        }
        with(asset2) {
            1 * setOwner(unit)
            1 * elementMigrationService.migrate(it, domain)
        }
        with(process) {
            1 * setOwner(unit)
            1 * elementMigrationService.migrate(it, domain)
        }

        and: "decisions are evaluated"
        1 * decider.decide(asset1, domain) >> [:]
        1 * decider.decide(asset2, domain) >> [:]
        1 * decider.decide(process, domain) >> [:]

        and: "everything is saved in the database"
        1 * unitRepository.save(_) >> unit
        1 * genericElementRepository.saveAll([asset1, asset2, process])
        1 * eventPublisher.publish(_ as RiskAffectingElementChangeEvent)
    }
}
