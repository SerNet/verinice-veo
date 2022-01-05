/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2020  Jochen Kemnade.
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
package org.veo.core

import static groovy.json.JsonOutput.toJson
import static org.springframework.http.MediaType.APPLICATION_JSON
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import java.nio.charset.StandardCharsets

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

import org.veo.rest.configuration.WebMvcSecurityConfiguration

import groovy.json.JsonSlurper

/**
 * Base class for veo specifications that use MockMvc
 */
@AutoConfigureMockMvc
@SpringBootTest(
webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
classes = [WebMvcSecurityConfiguration]
)
@EnableAsync
abstract class VeoMvcSpec extends VeoSpringSpec {

    // TODO VEO-663 make private again
    @Autowired
    protected MockMvc mvc


    ResultActions postUnauthorized(String url, Map content) {
        ResultActions asyncActions = mvc
                .perform(
                MockMvcRequestBuilders.post(url)
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON))
        return asyncActions.andExpect(status().is4xxClientError())
    }

    ResultActions post(String url, Map content, boolean expectSuccessfulRequest = true) {
        doRequest(MockMvcRequestBuilders.post(url)
                .contentType(APPLICATION_JSON)
                .content(toJson(content))
                .accept(APPLICATION_JSON),
                201,
                expectSuccessfulRequest)
    }

    ResultActions post(String url, Map content, int expectedStatusCode) {
        doRequest(MockMvcRequestBuilders.post(url)
                .contentType(APPLICATION_JSON)
                .content(toJson(content))
                .accept(APPLICATION_JSON),
                expectedStatusCode)
    }


    ResultActions get(String url, boolean expectSuccessfulRequest = true) {
        doRequest(MockMvcRequestBuilders.get(url)
                .accept(APPLICATION_JSON), 200, expectSuccessfulRequest)
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
                200,
                expectSuccessfulRequest)
    }


    ResultActions put(String url, Map content, boolean expectSuccessfulRequest = true) {
        doRequest(MockMvcRequestBuilders.put(url)
                .contentType(APPLICATION_JSON)
                .content(toJson(content))
                .accept(APPLICATION_JSON),
                200,
                expectSuccessfulRequest)
    }

    ResultActions delete(String url, boolean expectSuccessfulRequest = true) {
        doRequest(MockMvcRequestBuilders.delete(url)
                .accept(APPLICATION_JSON), 204, expectSuccessfulRequest)
    }

    ResultActions doRequest(MockHttpServletRequestBuilder requestBuilder, int successfulStatusCode, boolean expectSuccessfulRequest) throws Exception {
        ResultActions asyncActions = mvc
                .perform(MockMvcRequestBuilders.asyncDispatch(prepareAsyncRequest(requestBuilder)))
        MvcResult asyncResult = asyncActions.andReturn()

        if (expectSuccessfulRequest) {
            assert asyncResult.resolvedException == null
            asyncActions.andExpect(status().is(successfulStatusCode))
        } else {
            asyncActions.andExpect({result -> result.response.status != successfulStatusCode})
            assert asyncResult.resolvedException != null
            throw asyncResult.resolvedException
        }
        return asyncActions
    }

    ResultActions doRequest(MockHttpServletRequestBuilder requestBuilder, int expectedStatusCode) throws Exception {
        ResultActions asyncActions = mvc
                .perform(MockMvcRequestBuilders.asyncDispatch(prepareAsyncRequest(requestBuilder)))
        MvcResult asyncResult = asyncActions.andReturn()

        boolean expectSuccessfulRequest = HttpStatus.resolve(expectedStatusCode).is2xxSuccessful()

        if (expectSuccessfulRequest) {
            assert asyncResult.resolvedException == null
            asyncActions.andExpect(status().is(expectedStatusCode))
        } else {
            asyncActions.andExpect({result -> result.response.status == expectedStatusCode})
            assert asyncResult.resolvedException != null
            throw asyncResult.resolvedException
        }
        return asyncActions
    }

    private MvcResult prepareAsyncRequest(MockHttpServletRequestBuilder requestBuilder) throws Exception{
        def actions = mvc.perform(requestBuilder)
        try {
            return actions
                    .andExpect(request().asyncStarted())
                    .andReturn()
        }
        // The async request may fail to start if the request is invalid (e.g. has invalid HTTP header or params).
        catch (AssertionError ignored) {
            if(actions.andReturn().resolvedException != null) {
                throw actions.andReturn().resolvedException
            }
        }
    }

    String parseETag(ResultActions resultActions) {
        getTextBetweenQuotes(resultActions.andReturn().response.getHeader("ETag"))
    }

    def parseJson(ResultActions resultActions) {
        parseJson(resultActions.andReturn().response.getContentAsString(StandardCharsets.UTF_8))
    }

    def parseJson(String json) {
        new JsonSlurper().parseText(json)
    }

    String getETag(ResultActions resultActions) {
        return getTextBetweenQuotes(resultActions.andReturn().response.getHeader("ETag"))
    }
}
