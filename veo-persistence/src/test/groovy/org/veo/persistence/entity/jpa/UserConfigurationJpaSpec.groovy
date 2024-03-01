/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Urs Zeidler
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
package org.veo.persistence.entity.jpa

import org.hibernate.exception.ConstraintViolationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.entity.Client
import org.veo.core.entity.transform.EntityFactory
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.jpa.ClientDataRepository
import org.veo.persistence.access.jpa.DomainDataRepository
import org.veo.persistence.access.jpa.UserConfigurationDataRepository
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext

class UserConfigurationJpaSpec extends AbstractJpaSpec {

    @Autowired
    TransactionTemplate txTemplate
    @Autowired
    DomainDataRepository domainDataRepository
    @Autowired
    ClientDataRepository clientRepository
    @Autowired
    UserConfigurationDataRepository userConfigRepository
    @PersistenceContext
    EntityManager entityManager

    EntityFactory factory
    Client client
    ClientRepositoryImpl clientRepo
    ValidationService validationMock = Mock()

    def setup() {
        factory = new EntityDataFactory()
        txTemplate.execute{
            client = newClient()
            client = clientRepository.save(client)
        }
        clientRepo = new ClientRepositoryImpl(clientRepository, domainDataRepository, userConfigRepository, validationMock)
    }

    def "test non null constraint client"() {
        when: "we check non null client"
        userConfigRepository.save(factory.createUserConfiguration(null, "TestName", "TestApp"))
        entityManager.flush()

        then:
        thrown(ConstraintViolationException)
    }

    def "test non null constraint username"() {
        when: "we check non null username"
        userConfigRepository.save(factory.createUserConfiguration(client, null, "TestApp"))
        entityManager.flush()

        then:
        thrown(ConstraintViolationException)
    }

    def "application id must not be null"() {
        when: "we check non null application id"
        def conf = userConfigRepository.save(factory.createUserConfiguration(client, "TestName", ""))
        conf.setApplicationId(null)
        entityManager.flush()

        then:
        thrown(ConstraintViolationException)
    }

    def 'user configuration is inserted only once'() {
        when:
        def conf1 = userConfigRepository.save(factory.createUserConfiguration(client, "TestName", "TestApp"))
        entityManager.flush()
        def list = userConfigRepository.findAll()

        then:
        conf1.dbId != null
        list.size() == 1

        when:
        userConfigRepository.save(factory.createUserConfiguration(client, "TestName", "TestApp"))
        entityManager.flush()

        then:
        thrown(ConstraintViolationException)
    }

    def 'user configs are delete when the client is deleted'() {
        when:
        txTemplate.execute{
            userConfigRepository.save(factory.createUserConfiguration(client, "TestName1", "TestApp"))
            userConfigRepository.save(factory.createUserConfiguration(client, "TestName2", "TestApp"))
            userConfigRepository.save(factory.createUserConfiguration(client, "TestName3", "TestApp"))
            userConfigRepository.save(factory.createUserConfiguration(client, "TestName4", "TestApp"))
            userConfigRepository.save(factory.createUserConfiguration(client, "TestName1", "TestApp1"))
            userConfigRepository.save(factory.createUserConfiguration(client, "TestName2", "TestApp1"))
            userConfigRepository.save(factory.createUserConfiguration(client, "TestName3", "TestApp1"))
            userConfigRepository.save(factory.createUserConfiguration(client, "TestName4", "TestApp1"))
        }
        def list = userConfigRepository.findUserConfigurationsByClient(client.idAsString)

        then:
        list.size() == 8

        when:
        txTemplate.execute{
            clientRepo.delete(client);
        }
        list = userConfigRepository.findAll()

        then:
        list.empty
    }
}
