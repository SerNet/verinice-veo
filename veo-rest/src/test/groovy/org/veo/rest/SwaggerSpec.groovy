/*******************************************************************************
 * Copyright (c) 2020 Jochen Kemnade.
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
package org.veo.rest

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.web.servlet.MockMvc

import groovy.json.JsonSlurper

import org.veo.core.VeoSpringSpec
import org.veo.rest.configuration.WebMvcSecurityConfiguration

@SpringBootTest(
webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
classes = [WebMvcSecurityConfiguration]
)
@ComponentScan("org.veo.rest")
@AutoConfigureMockMvc
class SwaggerSpec extends VeoSpringSpec {

    @Autowired
    private MockMvc mvc

    def "Swagger documentation is available"() {
        when:
        def response = mvc.perform(get('/swagger-ui.html')).andReturn().response
        def redirectedUrl = response.redirectedUrl
        then:
        redirectedUrl != null
        when:
        response = mvc.perform(get(redirectedUrl)).andReturn().response
        then:
        response.contentAsString.contains('Swagger UI')
        when:
        response = mvc.perform(get('/v3/api-docs')).andReturn().response
        then:
        response.contentAsString.contains('verinice.VEO')
    }

    def "response DTO contains links property"() {
        when:
        def response = new JsonSlurper().parseText(mvc.perform(get('/v3/api-docs')).andReturn().response.contentAsString)
        def assetDtoSchema = response.components.schemas.AssetDto
        then:
        assetDtoSchema.properties.links != null
        assetDtoSchema.properties.links.description == 'Custom relations which do not affect the behavior.'
    }
}
