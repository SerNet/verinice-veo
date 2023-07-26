/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Alexander Koderman
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

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.transaction.support.TransactionTemplate

import org.veo.core.entity.Client
import org.veo.core.entity.Domain
import org.veo.persistence.access.ClientRepositoryImpl
import org.veo.persistence.access.jpa.DomainTemplateDataRepository

import groovy.json.JsonSlurper

class TranslationValidationMockMvcITSpec extends ContentSpec {

    @Autowired
    private ClientRepositoryImpl clientRepository
    @Autowired
    private DomainTemplateDataRepository domainTemplateRepository

    @Autowired
    TransactionTemplate txTemplate

    private Domain testDomain
    private Client client

    def setup() {
        txTemplate.execute {
            this.client = createTestClient()
            newDomain(client) {
                name = "Domain"
            }
            client = clientRepository.save(client)
            testDomain = client.domains.find{it.name == "Domain"}
        }
    }

    @WithUserDetails("content-creator")
    def "updating an object schema with invalid translations is prevented"() {
        given: "an object schema with translation errors"
        def schemaJson = TranslationValidationMockMvcITSpec.getResourceAsStream('/os_scope.json').withCloseable {
            new JsonSlurper().parse(it)
        }
        def modifiedSchema = [:] << schemaJson
        modifiedSchema.properties.translations.de.scope_SCP_Scope_status_NEW = ' Neu'
        modifiedSchema.properties.translations.de.scope_SCP_Scope_status_RELEASED = 'Freigegeben '
        modifiedSchema.properties.translations.de.remove('scope_management') // remove an attribute
        modifiedSchema.properties.translations.de.remove('scope_informationSecurityOfficer') // remove a link id
        modifiedSchema.properties.translations.en.remove('scope_SCP_Scope_status_IN_PROGRESS') // remove a subtype status
        modifiedSchema.properties.translations.en.remove('scope_dataProtectionOfficer_affiliation_external') // remove an enum
        modifiedSchema.properties.translations.de.superfluous_key = "I'm not even supposed to be here today!" // add a superfluous (i.e. mistyped) key

        when: "a request is made to the server"
        post("/content-creation/domains/${testDomain.id.uuidValue()}/element-type-definitions/scope/object-schema", schemaJson, 422)

        then: "all missing and  mistyped/superfluous translations are listed"
        Exception ex = thrown()
        ex.message == "Issues were found in the translations: Language 'de': LEADING_SPACES: scope_SCP_Scope_status_NEW ; MISSING: scope_informationSecurityOfficer, scope_management ; SUPERFLUOUS: superfluous_key ; TRAILING_SPACES: scope_SCP_Scope_status_RELEASED    /    Language 'en': MISSING: scope_SCP_Scope_status_IN_PROGRESS, scope_dataProtectionOfficer_affiliation_external"
    }
}