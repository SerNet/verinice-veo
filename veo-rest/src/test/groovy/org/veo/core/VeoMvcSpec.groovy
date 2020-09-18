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
package org.veo.core

import static groovy.json.JsonOutput.toJson
import static org.springframework.http.MediaType.APPLICATION_JSON

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

import groovy.json.JsonSlurper

/**
 * Base class for veo specifications that use MockMvc
 */
@AutoConfigureMockMvc
abstract class VeoMvcSpec extends VeoSpringSpec {

    @Autowired
    private MockMvc mvc



    ResultActions post(String url, Map content, boolean expectSuccessfulRequest = true) {
        doRequest(MockMvcRequestBuilders.post(url)
                .contentType(APPLICATION_JSON)
                .content(toJson(content))
                .accept(APPLICATION_JSON),
                expectSuccessfulRequest)
    }


    ResultActions get(String url) {
        doRequest(MockMvcRequestBuilders.get(url)
                .accept(APPLICATION_JSON))
    }

    ResultActions put(String url, Map content, Map headers, boolean expectSuccessfulRequest = true) {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.put(url)
                .contentType(APPLICATION_JSON)
                .content(toJson(content))
                .accept(APPLICATION_JSON)
        for (key in headers.keySet()) {
            requestBuilder.header(key, headers.get(key))
        }
        doRequest(requestBuilder,
                expectSuccessfulRequest)
    }


    ResultActions put(String url, Map content, boolean expectSuccessfulRequest = true) {
        doRequest(MockMvcRequestBuilders.put(url)
                .contentType(APPLICATION_JSON)
                .content(toJson(content))
                .accept(APPLICATION_JSON),
                expectSuccessfulRequest)
    }

    ResultActions delete(String url) {
        doRequest(MockMvcRequestBuilders.delete(url)
                .accept(APPLICATION_JSON))
    }

    ResultActions doRequest(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        doRequest(requestBuilder, true)
    }

    ResultActions doRequest(MockHttpServletRequestBuilder requestBuilder, boolean expectSuccessfulRequest) throws Exception {
        ResultActions actions = mvc.perform(requestBuilder)
        MvcResult result = actions.andReturn()
        if (expectSuccessfulRequest) {
            assert result.resolvedException == null
            actions.andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
            assert result.request.asyncStarted
        } else {
            actions.andExpect(MockMvcResultMatchers.status().is4xxClientError())
            assert result.resolvedException != null
            throw result.resolvedException
        }

        ResultActions  asyncActions = mvc
                .perform(MockMvcRequestBuilders.asyncDispatch(result))
                .andDo(MockMvcResultHandlers.print())
        MvcResult asyncResult = asyncActions.andReturn()
        assert asyncResult.resolvedException == null
        asyncActions.andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
    }

    def parseJson(ResultActions resultActions) {
        new JsonSlurper().parseText(resultActions.andReturn().response.contentAsString)
    }
}
