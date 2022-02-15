/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2022  Jonas Jordan
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
package org.veo.jobs

import org.springframework.stereotype.Component

import org.veo.rest.test.UserType
import org.veo.rest.test.VeoRestTest

import groovy.json.JsonSlurper

/**
 * Creates test domain templates from resource files via HTTP during rest-tests. Each domain template is only inserted once per test execution (because rest-tests never cleanup the DB).
 */
@Component
class RestTestDomainTemplateCreator {
    private JsonSlurper jsonSlurper = new JsonSlurper()
    private Set<String> injectedDomainTemplates = new HashSet<>()

    def create(String filename, VeoRestTest test) {
        if(!injectedDomainTemplates.contains(filename)) {
            var template = jsonSlurper.parse(getClass().getResource('/testdomaintemplates/' + filename + '.json'))
            test.post("/domaintemplates/", template, null, UserType.CONTENT_CREATOR)
            injectedDomainTemplates.add(filename)
        }
    }
}
