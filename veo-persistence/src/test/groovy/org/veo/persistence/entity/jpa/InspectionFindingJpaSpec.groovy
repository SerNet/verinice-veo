/*
 * verinice.veo
 * Copyright (C) 2026  Jochen Kemnade.
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
 */
package org.veo.persistence.entity.jpa

import org.springframework.beans.factory.annotation.Autowired

import com.github.zafarkhaja.semver.Version

import org.veo.core.entity.CatalogItem
import org.veo.core.entity.Client
import org.veo.core.entity.Control
import org.veo.core.entity.Domain
import org.veo.core.entity.DomainTemplate
import org.veo.core.entity.ElementType
import org.veo.core.entity.TailoringReferenceType
import org.veo.core.entity.TranslatedText
import org.veo.core.entity.Unit
import org.veo.core.entity.inspection.Severity
import org.veo.core.entity.transform.EntityFactory
import org.veo.core.repository.InspectionFindingRepository
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.ControlDataRepository
import org.veo.persistence.access.jpa.DomainDataRepository
import org.veo.persistence.access.jpa.DomainTemplateDataRepository
import org.veo.persistence.access.jpa.InspectionFindingDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory

class InspectionFindingJpaSpec extends AbstractJpaSpec {
    @Autowired
    DomainTemplateDataRepository domainTemplateRepository
    @Autowired
    DomainDataRepository repository
    @Autowired
    ClientDataRepository clientRepository
    @Autowired
    UnitDataRepository unitRepository
    @Autowired
    ControlDataRepository controlRepository
    EntityFactory factory

    @Autowired
    InspectionFindingDataRepository inspectionFindingDataRepository

    Client client
    Domain domain

    def setup() {
        factory = new EntityDataFactory()
        client = newClient()
        newDomain(client)
        client = clientRepository.save(client)
        domain = client.domains.first()
    }

    def 'CRUD inspection findings'() {
        when:
        def unit = unitRepository.save(newUnit(client))
        def person = personDataRepository.save(newPerson(unit))
        def asset = assetDataRepository.save(newAsset(unit))
        def asset2 = assetDataRepository.save(newAsset(unit))

        inspectionFindingDataRepository.save(new InspectionFindingData().tap {
            domain = owner.domain
            it.unit = unit
            element = asset
            inspectionId = 'holmes'
            severity = Severity.WARNING
            taskSummary = TranslatedText.of {"check it out"}
            responsible = person
        })

        def inspectionFindings = inspectionFindingDataRepository.findAllByElementIsAndDomainIs(asset, domain)
        def inspectionFindings2 = inspectionFindingDataRepository.findAllByElementIsAndDomainIs(asset2, domain)

        then:
        inspectionFindings.size() == 1
        with(inspectionFindings.first()) {
            element == asset
            it.unit == unit
            it.domain == owner.domain
            inspectionId == 'holmes'
            severity == Severity.WARNING
            taskSummary.get(EN) == "check it out"
            responsible == person
        }

        inspectionFindings2.empty
    }
}
