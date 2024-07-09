/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jonas Jordan.
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
import org.veo.core.entity.CompositeElement
import org.veo.core.entity.EntityType
import org.veo.core.entity.Unit
import org.veo.core.repository.AssetRepository
import org.veo.core.repository.ControlRepository
import org.veo.core.repository.DocumentRepository
import org.veo.core.repository.IncidentRepository
import org.veo.core.repository.PersonRepository
import org.veo.core.repository.ProcessRepository
import org.veo.core.repository.ScenarioRepository
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.ControlImplementationDataRepository
import org.veo.persistence.access.jpa.CustomLinkDataRepository
import org.veo.persistence.access.jpa.RequirementImplementationDataRepository
import org.veo.persistence.access.jpa.RiskAffectedDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository
import org.veo.persistence.entity.jpa.AbstractJpaSpec
import org.veo.persistence.entity.jpa.RiskAffectedData
import org.veo.persistence.entity.jpa.ValidationService

class CompositeElementAccessSpec extends AbstractJpaSpec {
    AssetRepository assetRepository
    ControlRepository controlRepository
    DocumentRepository documentRepository
    IncidentRepository incidentRepository
    PersonRepository personRepository
    ProcessRepository processRepository
    ScenarioRepository scenarioRepository

    @Autowired
    UnitDataRepository unitRepository

    @Autowired
    ClientDataRepository clientRepository

    @Autowired
    CustomLinkDataRepository linkDataRepository

    @Autowired
    ControlImplementationDataRepository controlImplementationRepository

    @Autowired
    RequirementImplementationDataRepository requirementImplementationDataRepository

    @Autowired
    RiskAffectedDataRepository<RiskAffectedData<?,?>> riskAffectedDataRepository

    ValidationService validationMock = Mock()

    @Autowired
    TransactionTemplate txTemplate

    Client client
    Unit unit

    def setup() {
        client = clientRepository.save(newClient())
        unit = unitRepository.save(newUnit(client))

        assetRepository = new AssetRepositoryImpl(assetDataRepository, validationMock, linkDataRepository, scopeDataRepository, elementQueryFactory)
        controlRepository = new ControlRepositoryImpl(controlDataRepository, validationMock, linkDataRepository, scopeDataRepository, elementQueryFactory, riskAffectedDataRepository)
        documentRepository = new DocumentRepositoryImpl(documentDataRepository, validationMock, linkDataRepository, scopeDataRepository, elementQueryFactory)
        incidentRepository = new IncidentRepositoryImpl(incidentDataRepository, validationMock, linkDataRepository, scopeDataRepository, elementQueryFactory)
        personRepository = new PersonRepositoryImpl(personDataRepository, validationMock, linkDataRepository, scopeDataRepository, assetDataRepository, processDataRepository, elementQueryFactory, controlImplementationRepository, requirementImplementationDataRepository)
        processRepository = new ProcessRepositoryImpl(processDataRepository, validationMock, linkDataRepository, scopeDataRepository, elementQueryFactory)
        scenarioRepository = new ScenarioRepositoryImpl(scenarioDataRepository, validationMock, linkDataRepository, scopeDataRepository, riskAffectedDataRepository, elementQueryFactory)
    }

    def "delete parts from a #type.simpleName composite"() {
        given: "A nested structure of composites and elements"
        def typeRepository = getProperty("${type.simpleName.toLowerCase()}Repository")
        def part = "new${type.simpleName}"(unit)
        def composite = "new${type.simpleName}"(unit) {
            parts << part
        }
        def scope = newScope(unit) {
            members << composite
        }
        txTemplate.execute {
            typeRepository.save(composite)
            scopeDataRepository.save(scope)
        }
        assert scopeDataRepository.findById(scope.dbId).get().members.size()>0

        when: "the composite is removed"
        txTemplate.execute{
            typeRepository.delete(composite)
        }

        then: "the elements remain"
        typeRepository.findById(part.id).present
        typeRepository.findById(composite.id).empty

        and: "the member was removed from the scope"
        def persistedScope = scopeDataRepository.findById(scope.dbId)
        persistedScope.present
        persistedScope.get().members.empty

        where:
        type << EntityType.ELEMENT_TYPE_CLASSES.findAll{CompositeElement.isAssignableFrom(it)}
    }
}
