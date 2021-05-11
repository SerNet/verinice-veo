/*******************************************************************************
 * Copyright (c) 2020 Alexander Koderman.
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

import static org.springframework.http.HttpStatus.OK
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.web.servlet.MockMvc

import org.veo.core.VeoSpringSpec
import org.veo.rest.configuration.WebMvcSecurityConfiguration

/**
 * Unit test for the translation controller.
 * Uses mocked spring MVC environment.
 * Does not start an embedded server.
 * Uses a test Web-MVC configuration with example accounts and clients.
 */
@SpringBootTest(
webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
classes = [WebMvcSecurityConfiguration]
)
@ComponentScan("org.veo.rest")
@AutoConfigureMockMvc
class TranslationControllerMockMvcSpec2 extends VeoSpringSpec {

    @Autowired
    private MockMvc mvc

    @WithUserDetails("user@domain.example")
    def "get the translation for all languages"() {
        when: "a request for a T10N file is made"

        def results = mvc.perform(get('/translations?languages=all'))
        def response = results.andReturn().response

        then: "a correct response is returned"
        response.status == OK.value()
        results.andExpect(jsonPath('$.lang.de.person_address_city').value("Stadt"))
        results.andExpect(jsonPath('$.lang.en.person_address_city').value("City"))
    }
}
