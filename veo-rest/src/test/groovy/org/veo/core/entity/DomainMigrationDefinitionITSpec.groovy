/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2024  Jochen Kemnade
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

import org.veo.core.entity.definitions.DomainMigrationDefinition
import org.veo.core.entity.definitions.DomainMigrationStep
import org.veo.test.VeoSpec

class DomainMigrationDefinitionITSpec extends VeoSpec {
    Domain domain
    Unit unit

    def setup() {
        def client = newClient {}
        domain = newDomain(client).tap {
            domainTemplate = newDomainTemplate()
        }
    }

    def "An empty migration step is fine"() {
        given:
        DomainMigrationDefinition dmd = new DomainMigrationDefinition([])

        when:
        dmd.validate(domain, domain.domainTemplate)

        then:
        noExceptionThrown()
    }

    def "New definitions are optional for a migration step"() {
        given:
        DomainMigrationDefinition dmd = new DomainMigrationDefinition([
            new DomainMigrationStep("step-1", new TranslatedText([(Locale.US):'A simple step']), [], null)
        ])

        when:
        dmd.validate(domain, domain.domainTemplate)

        then:
        noExceptionThrown()
    }
}