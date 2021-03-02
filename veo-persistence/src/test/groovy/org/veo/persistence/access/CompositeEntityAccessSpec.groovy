/*******************************************************************************
 * Copyright (c) 2020 Jonas Jordan.
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
package org.veo.persistence.access

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.entity.Client
import org.veo.core.entity.CompositeEntity
import org.veo.core.entity.EntityTypeNames
import org.veo.core.entity.Unit
import org.veo.core.usecase.repository.AssetRepository
import org.veo.core.usecase.repository.ControlRepository
import org.veo.core.usecase.repository.DocumentRepository
import org.veo.core.usecase.repository.IncidentRepository
import org.veo.core.usecase.repository.PersonRepository
import org.veo.core.usecase.repository.ProcessRepository
import org.veo.core.usecase.repository.ScenarioRepository
import org.veo.persistence.access.jpa.AssetDataRepository
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.ControlDataRepository
import org.veo.persistence.access.jpa.CustomLinkDataRepository
import org.veo.persistence.access.jpa.DocumentDataRepository
import org.veo.persistence.access.jpa.IncidentDataRepository
import org.veo.persistence.access.jpa.PersonDataRepository
import org.veo.persistence.access.jpa.ProcessDataRepository
import org.veo.persistence.access.jpa.ScenarioDataRepository
import org.veo.persistence.access.jpa.ScopeDataRepository
import org.veo.persistence.access.jpa.UnitDataRepository
import org.veo.persistence.entity.jpa.AbstractJpaSpec
import org.veo.persistence.entity.jpa.ModelObjectValidation

import spock.lang.Unroll

class CompositeEntityAccessSpec extends AbstractJpaSpec {


    @Autowired
    AssetDataRepository assetDataRepository

    @Autowired
    ControlDataRepository controlDataRepository

    @Autowired
    DocumentDataRepository documentDataRepository

    @Autowired
    IncidentDataRepository incidentDataRepository

    @Autowired
    PersonDataRepository personDataRepository

    @Autowired
    ProcessDataRepository processDataRepository

    @Autowired
    ScenarioDataRepository scenarioDataRepository

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
    ScopeDataRepository scopeDataRepository

    @Autowired
    CustomLinkDataRepository linkDataRepository

    ModelObjectValidation validationMock = Mock()

    @Autowired
    TransactionTemplate txTemplate

    Client client
    Unit unit

    def setup() {
        client = clientRepository.save(newClient())
        unit = unitRepository.save(newUnit(client))

        assetRepository = new AssetRepositoryImpl(assetDataRepository, validationMock, linkDataRepository, scopeDataRepository)
        controlRepository = new ControlRepositoryImpl(controlDataRepository, validationMock, linkDataRepository, scopeDataRepository, assetDataRepository)
        documentRepository = new DocumentRepositoryImpl(documentDataRepository, validationMock, linkDataRepository, scopeDataRepository)
        incidentRepository = new IncidentRepositoryImpl(incidentDataRepository, validationMock, linkDataRepository, scopeDataRepository)
        personRepository = new PersonRepositoryImpl(personDataRepository, validationMock, linkDataRepository, scopeDataRepository, assetDataRepository)
        processRepository = new ProcessRepositoryImpl(processDataRepository, validationMock, linkDataRepository, scopeDataRepository)
        scenarioRepository = new ScenarioRepositoryImpl(scenarioDataRepository, validationMock, linkDataRepository, scopeDataRepository, assetDataRepository)
    }

    @Unroll
    def "delete parts from a #type.simpleName composite"() {
        given: "A nested structure of composites and entities"
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
        assert scopeDataRepository.findById(scope.id.uuidValue()).get().members.size()>0

        when: "the composite is removed"
        txTemplate.execute{
            typeRepository.delete(composite)
        }

        then: "the entities remain"
        typeRepository.findById(part.id).present
        typeRepository.findById(composite.id).empty

        and: "the member was removed from the scope"
        def persistedScope = scopeDataRepository.findById(scope.id.uuidValue())
        persistedScope.present
        persistedScope.get().members.empty

        where:
        type << EntityTypeNames.getKnownEntityClasses().findAll{CompositeEntity.isAssignableFrom(it)}
    }
}
