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

import org.veo.core.entity.condition.ConstantExpression
import org.veo.core.entity.condition.CustomAspectAttributeValueExpression
import org.veo.core.entity.condition.EqualsExpression
import org.veo.core.entity.condition.TernaryExpression
import org.veo.core.entity.definitions.attribute.EnumAttributeDefinition
import org.veo.core.entity.domainmigration.CustomAspectAttribute
import org.veo.core.entity.domainmigration.CustomAspectMigrationTransformDefinition
import org.veo.core.entity.domainmigration.DomainMigrationDefinition
import org.veo.core.entity.domainmigration.DomainMigrationStep
import org.veo.core.entity.domainmigration.MigrationTransformDefinition
import org.veo.core.entity.exception.UnprocessableDataException
import org.veo.test.VeoSpec

class DomainMigrationDefinitionITSpec extends VeoSpec {
    Domain domain
    Unit unit

    def setup() {
        def client = newClient {}
        def caDefinition = [
            scope_details: newCustomAspectDefinition {
                attributeDefinitions = [
                    very_detailed_attribute: new EnumAttributeDefinition(['foo'])
                ]
            }
        ]
        domain = newDomain(client).tap {
            applyElementTypeDefinition(newElementTypeDefinition(ElementType.SCOPE, it) {
                customAspects = caDefinition
            })
            domainTemplate = newDomainTemplate{
                applyElementTypeDefinition(newElementTypeDefinition(ElementType.SCOPE, it) {
                    customAspects = caDefinition
                })
            }
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
            new DomainMigrationStep("step-1", new TranslatedText([(Locale.US):'A simple step']), [], null, false)
        ])

        when:
        dmd.validate(domain, domain.domainTemplate)

        then:
        noExceptionThrown()
    }

    def "Interactive migration step without newDefinitions"() {
        given:
        DomainMigrationDefinition dmd = new DomainMigrationDefinition([
            new DomainMigrationStep("step-1", new TranslatedText([(Locale.US):'A simple step']), [], null, true)
        ])

        when:
        dmd.validate(domain, domain.domainTemplate)

        then:
        noExceptionThrown()
    }

    def "Interactive migration step must not have newDefinitions"() {
        given:
        DomainMigrationDefinition dmd = new DomainMigrationDefinition([
            new DomainMigrationStep("step-1", new TranslatedText([(Locale.US):'A simple step']), [], [
                Mock(MigrationTransformDefinition)
            ], true)
        ])

        when:
        dmd.validate(domain, domain.domainTemplate)

        then:
        UnprocessableDataException e = thrown()
        e.message == 'Interactive step step-1 does not support new definitions.'
    }

    def "Ternary expression"() {
        given:
        DomainMigrationDefinition dmd = new DomainMigrationDefinition([
            new DomainMigrationStep("step-1",
            new TranslatedText([(Locale.US):'A ternary expression step']),
            [],
            [
                new CustomAspectMigrationTransformDefinition(
                new TernaryExpression(
                new EqualsExpression(
                new CustomAspectAttributeValueExpression("scope_details", "very_detailed_attribute"),
                new ConstantExpression("old")
                ),
                new ConstantExpression(null),
                new CustomAspectAttributeValueExpression("scope_details", "very_detailed_attribute"),
                ),
                new CustomAspectAttribute(ElementType.SCOPE, "scope_details", "very_detailed_attribute")
                )
            ],
            false)
        ])

        when:
        dmd.validate(domain, domain.domainTemplate)

        then:
        noExceptionThrown()
    }
}