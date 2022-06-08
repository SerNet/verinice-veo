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
package org.veo.rest.domaintemplates

import org.springframework.core.io.support.PathMatchingResourcePatternResolver

import org.veo.adapter.service.domaintemplate.dto.TransformDomainTemplateDto

import groovy.json.JsonSlurper
import spock.lang.Specification

/**
 * Makes sure assembled domain template files are present and look valid. This tests the generated PROD domain template
 * files, not the test files.
 */
class DomainTemplateAssemblerITSpec extends Specification {

    def "some templates are present"() {
        expect:
        templates.size() > 0
    }

    def "template #template.name is ok"() {
        expect:
        template.id != null
        template.name != null
        template.catalogs.size() > 0
        with(template.catalogs.first()) {
            it.name != null
            it.catalogItems.size() > 0
            with(it.catalogItems.first()) {
                it.element.name != null
            }
        }
        where:
        template << templates
    }

    def getTemplates() {
        new PathMatchingResourcePatternResolver(getClass().classLoader)
                .getResources("classpath*:/domaintemplates/*.json")
                .collect {
                    new JsonSlurper().parse(it.inputStream) as TransformDomainTemplateDto
                }
    }
}
