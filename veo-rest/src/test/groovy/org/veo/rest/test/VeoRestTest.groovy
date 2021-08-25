/*******************************************************************************
 * verinice.veo
 * Copyright (C) 2021  Alexander Koderman
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
package org.veo.rest.test

import static groovy.json.JsonOutput.toJson
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

import java.util.regex.Matcher
import java.util.regex.Pattern

import org.apache.http.HttpHost
import org.apache.http.impl.client.HttpClientBuilder
import org.keycloak.authorization.client.AuthzClient
import org.keycloak.authorization.client.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles

import org.veo.core.entity.transform.EntityFactory
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory
import org.veo.rest.RestApplication
import org.veo.rest.security.WebSecurity

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import spock.lang.Shared

@SpringBootTest(classes = [RestApplication.class, WebSecurity.class],
webEnvironment = RANDOM_PORT)
@ActiveProfiles(["test", "resttest", "local"])
@Slf4j
class VeoRestTest extends spock.lang.Specification {

    @Autowired
    TestRestTemplate restTemplate

    @Shared
    JsonSlurper jsonSlurper

    @Shared
    EntityFactory factory = new EntityDataFactory()

    @Value('${veo.resttest.baseUrl}')
    String baseUrl

    @Value('${veo.resttest.oidcUrl}')
    def oidcUrl = 'https://keycloak.staging.verinice.com'

    @Value('${veo.resttest.realm}')
    def realm = 'verinice-veo'

    @Value('${veo.resttest.clientId}')
    def clientId = 'veo-development-client'

    @Value('${veo.resttest.users.default.name}')
    String defaultUserName

    @Value('${veo.resttest.users.default.pass}')
    private String defaultUserPass

    @Value('${veo.resttest.users.admin.name}')
    String adminUserName

    @Value('${veo.resttest.users.admin.pass}')
    private String adminUserPass

    @Value('${veo.resttest.proxyHost}')
    private String proxyHost

    @Value('${veo.resttest.proxyPort}')
    private int proxyPort

    private userTokenCache = [:]

    class Response{
        Map headers
        Object body
    }

    def setupSpec() {
        jsonSlurper = new JsonSlurper()
    }

    def String getETag(String text) {
        Pattern p = Pattern.compile("\"([^\"]*)\"")
        Matcher m = p.matcher(text)
        if (m.find()) {
            return m.group(1)
        } else {
            return text
        }
    }

    Response get(String relativeUri, int assertStatusCode = 200, UserType userType = UserType.DEFAULT) {
        def resp = exchange(relativeUri, HttpMethod.GET, new HttpHeaders(), null, userType)
        assert resp.statusCodeValue == assertStatusCode
        log.info(resp.body.toString())
        new Response(
                headers: resp.headers,
                body: jsonSlurper.parseText(resp.body.toString()))
    }

    Response post(String relativeUri, Object requestBody, int assertStatusCode = 201, UserType userType = UserType.DEFAULT) {
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)

        def resp = exchange(relativeUri, HttpMethod.POST, headers, requestBody, userType)
        assert resp.statusCodeValue == assertStatusCode
        new Response(headers: resp.headers,
        body: jsonSlurper.parseText(resp.body.toString()))
    }

    void put(String relativeUri, Object requestBody, String etagHeader, int assertStatusCode = 200, UserType userType = UserType.DEFAULT) {
        HttpHeaders headers = new HttpHeaders()
        headers.setIfMatch(getETag(etagHeader))
        headers.setContentType(MediaType.APPLICATION_JSON)

        def resp = exchange(relativeUri, HttpMethod.PUT, headers, requestBody, userType)
        assert resp.statusCodeValue == assertStatusCode
    }

    void delete(String relativeUri, int assertStatusCode = 204, UserType userType = UserType.DEFAULT) {
        def resp = exchange(relativeUri, HttpMethod.DELETE, new HttpHeaders(), null, userType)
        assert resp.statusCodeValue == assertStatusCode
    }

    def getUnit(id) {
        get("/units/${id}").body
    }

    def postNewUnit(unitName) {
        def response = post("/units", [
            name: unitName
        ])
        response.body
    }

    def getDomains() {
        get("/domains").body
    }

    def getCatalog(id) {
        get("/catalogs/${id}").body
    }

    def getControl(id) {
        get("/controls/${id}").body
    }

    ResponseEntity<String> exchange(String relativeUri, HttpMethod httpMethod, HttpHeaders headers, Object requestBody = null, UserType userType = UserType.DEFAULT) {
        headers.put("Authorization", [
            "Bearer " + getToken(userType)
        ])
        return restTemplate.exchange(baseUrl + relativeUri, httpMethod, new HttpEntity(requestBody?.with { toJson(it) }, headers), String.class)
    }

    private String getToken(UserType userType) {
        def user = defaultUserName
        def pass = defaultUserPass
        if (userType == UserType.ADMIN) {
            user = adminUserName
            pass = adminUserPass
        }
        if (userTokenCache.hasProperty(user)) {
            return userTokenCache[user]
        }
        def proxy = new HttpHost(proxyHost, proxyPort)
        def newToken = HttpClientBuilder.create().with {
            it.proxy = proxy
            build()
        }.withCloseable {
            Configuration configuration = new Configuration("$oidcUrl/auth", realm, clientId, ['secret': ''], it)
            AuthzClient authzClient = AuthzClient.create(configuration)
            def accessTokenResponse = authzClient.obtainAccessToken(user, pass)
            accessTokenResponse.token
        }
        userTokenCache[user] = newToken
        return newToken
    }
}
