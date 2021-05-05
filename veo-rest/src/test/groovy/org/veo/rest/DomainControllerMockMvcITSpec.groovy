/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Urs Zeidler.
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
package org.veo.rest

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import org.apache.commons.codec.digest.DigestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.VeoMvcSpec
import org.veo.core.entity.Catalog
import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.core.entity.Key
import org.veo.core.entity.Unit
import org.veo.core.entity.specification.ClientBoundaryViolationException
import org.veo.core.usecase.common.ETag
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.DomainRepositoryImpl
import org.veo.rest.configuration.WebMvcSecurityConfiguration

/**
 * Integration test for the domain controller. Uses mocked spring MVC environment.
 * Uses JPA repositories with in-memory database.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
@SpringBootTest(
webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
classes = [WebMvcSecurityConfiguration]
)
@EnableAsync
@ComponentScan("org.veo.rest")
class DomainControllerMockMvcITSpec extends VeoMvcSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private DomainRepositoryImpl domainRepository

    @Autowired
    TransactionTemplate txTemplate

    private Unit unit
    private Domain testDomain
    private Client client
    private Catalog catalog
    private Client secondClient
    private Domain domainSecondClient
    private Key clientId = Key.uuidFrom(WebMvcSecurityConfiguration.TESTCLIENT_UUID)
    String salt = "salt-for-etag"

    def setup() {
        txTemplate.execute {
            Domain domain1 = newDomain{
                name = "Domain 1"
            }
            Domain domain2 = newDomain{
                name = "Domain 2"
            }
            client = newClient {
                id = clientId
            }
            catalog = newCatalog(domain1) {
                name= 'a'
            }
            client.addToDomains(domain1)
            client.addToDomains(domain2)

            client = clientRepository.save(client)
            domain1 = client.domains.first()
            catalog = domain1.catalogs.first()
            domainSecondClient = newDomain()
            secondClient = newClient()
            secondClient.addToDomains(domainSecondClient)
            secondClient = clientRepository.save(secondClient)
            domainSecondClient = secondClient.domains.iterator().next()
        }
        ETag.setSalt(salt)

        testDomain = client.getDomains().iterator().next()
    }


    @WithUserDetails("user@domain.example")
    def "retrieve a Domain"() {
        given: "a saved domain"

        when: "a request is made to the server"
        def results = get("/domains/${testDomain.id.uuidValue()}")
        String expectedETag = DigestUtils.sha256Hex(testDomain.id.uuidValue() + "_" + salt + "_" + Long.toString(testDomain.getVersion()))

        then: "the domain is found"
        results.andExpect(status().isOk())
        and: "the eTag is set"
        String eTag = results.andReturn().response.getHeader("ETag")
        eTag != null
        getTextBetweenQuotes(eTag).equals(expectedETag)
        and:
        def result = parseJson(results)
        result.name == testDomain.name
        result.catalogs.size() == 1
        when:
        def firstCatalog = result.catalogs.first()
        then:
        firstCatalog.displayName == 'a'
        firstCatalog.targetUri == "http://localhost/catalogs/${catalog.dbId}"
    }

    @WithUserDetails("user@domain.example")
    def "retrieve a Domain wrong client"() {
        given: "a saved domain"

        when: "a request is made to the server"
        def results = get("/domains/${domainSecondClient.id.uuidValue()}", false)

        then: "the data is rejected"
        ClientBoundaryViolationException ex = thrown()

        and: "the reason is given"
        ex.message =~ /The domain is not accessable from this client./
    }

    @WithUserDetails("user@domain.example")
    def "retrieve all domains for a client"() {
        when: "a request is made to the server"
        def results = get("/domains?")

        then: "the domains are returned"
        results.andExpect(status().isOk())
        when:
        def result = parseJson(results)
        then:
        result.size == 2
        result*.name.sort().first() == 'Domain 1'
    }
}