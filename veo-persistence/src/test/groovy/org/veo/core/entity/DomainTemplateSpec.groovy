/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Urs Zeidler.
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
package org.veo.core.entity


import org.veo.test.VeoSpec

class DomainTemplateSpec extends VeoSpec {

    def "Create a new DomainTemplate"() {
        given: "a domain template name, authority, ..."
        String name = 'Test domain'
        String auth = 'authority'
        String templateVersion = '1.0'

        when : "DomainTemplate is created"
        DomainTemplate domainTemplate = newDomainTemplate() {
            it.name = name
            it.authority = auth
            it.templateVersion = templateVersion
            it.riskDefinitions = ["id":
                createRiskDefinition("id1")
            ] as Map
        }

        then: "domain is correct initatlized"
        domainTemplate.getName() == name
        domainTemplate.getAuthority() == auth
        domainTemplate.getTemplateVersion() == templateVersion
        domainTemplate.getRiskDefinitions() != null
    }
}
