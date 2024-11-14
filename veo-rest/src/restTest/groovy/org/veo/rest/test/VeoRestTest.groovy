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
import static java.util.UUID.randomUUID
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import static org.veo.rest.test.UserType.ADMIN
import static org.veo.rest.test.UserType.CONTENT_CREATOR

import java.nio.charset.StandardCharsets
import java.time.Instant

import org.apache.http.HttpHost
import org.apache.http.impl.client.HttpClientBuilder
import org.keycloak.authorization.client.AuthzClient
import org.keycloak.authorization.client.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

import org.veo.core.entity.event.ClientEvent.ClientChangeType
import org.veo.core.entity.transform.EntityFactory
import org.veo.jobs.RestTestDomainTemplateCreator
import org.veo.message.EventDispatcher
import org.veo.message.EventMessage
import org.veo.persistence.entity.jpa.transformer.EntityDataFactory
import org.veo.rest.RestApplication
import org.veo.rest.security.WebSecurity

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

@SpringBootTest(classes = [RestApplication.class, WebSecurity.class],
webEnvironment = RANDOM_PORT)
@ActiveProfiles(resolver = RestTestProfileResolver.class)
@Slf4j
class VeoRestTest extends Specification {

    @Autowired
    TestRestTemplate restTemplate

    @Autowired
    RestTestDomainTemplateCreator domainTemplateCreator

    @Autowired
    EventDispatcher eventDispatcher

    @Shared
    JsonSlurper jsonSlurper

    @Shared
    EntityFactory factory = new EntityDataFactory()

    @Value('${veo.resttest.baseUrl}')
    private String configuredBaseUrl

    def baseUrl

    @Value('${veo.resttest.oidcUrl}')
    def oidcUrl

    @Value('${veo.resttest.realm}')
    def realm = 'verinice-veo'

    @Value('${veo.resttest.clientId}')
    def clientId = 'veo-development-client'

    @Value('${veo.resttest.veo-clientid}')
    def veoClientId = '21712604-ed85-4f08-aa46-1cf39607ee9e'

    @Value('${veo.resttest.veo-secondary-clientid}')
    def veoSecondaryClientId = '26ab39fb-d846-4f47-84a9-98b9aaa64419'

    @Value('${veo.resttest.users.default.name}')
    String defaultUserName

    @Value('${veo.resttest.users.default.pass}')
    private String defaultUserPass

    @Value('${veo.resttest.users.admin.name}')
    String adminUserName

    @Value('${veo.resttest.users.admin.pass}')
    private String adminUserPass

    @Value('${veo.resttest.users.content-creator.name}')
    String contentCreatorUserName

    @Value('${veo.resttest.users.content-creator.pass}')
    private String contentCreatorUserPass

    @Value('${veo.resttest.users.read-only.name}')
    String readOnlyUserName

    @Value('${veo.resttest.users.read-only.pass}')
    private String readOnlyUserPass

    @Value('${veo.resttest.users.secondary-client.name}')
    String secondaryClientUserName

    @Value('${veo.resttest.users.secondary-client.pass}')
    private String secondaryClientUserPass

    @Value('${veo.resttest.proxyHost}')
    private String proxyHost

    @Value('${veo.resttest.proxyPort}')
    private int proxyPort

    @Value('${veo.message.routing-key-prefix}')
    String routingKeyPrefix

    @Value('${veo.message.exchanges.veo-subscriptions}')
    String exchange

    String dsgvoDomainId
    String testDomainId

    private userTokenCache = [:]

    static boolean clientsCreated = false

    PollingConditions defaultPolling = new PollingConditions(delay: 0.8, timeout: 5)

    class Response {
        HttpHeaders headers
        Object body
        int statusCode
        String getETag() {
            headers.getETag()
        }
        String getLocation() {
            return headers.getLocation()?.toString()
        }
    }

    def setupSpec() {
        jsonSlurper = new JsonSlurper()
    }

    def setup() {
        baseUrl = configuredBaseUrl.empty ? restTemplate.rootUri : configuredBaseUrl
        if (baseUrl.endsWith('/')) {
            baseUrl = baseUrl[0..-2]
        }
        domainTemplateCreator.create('dsgvo', this)
        domainTemplateCreator.create('test-domain', this)
        if (!clientsCreated) {
            sendClientChangeEvent([clientId: veoClientId, name: 'veo REST test client', type: ClientChangeType.CREATION,
                domainProducts : ["DS-GVO": ["EXAMPLE_ORGANIZATION"], "test-domain": []]])
            sendClientChangeEvent([clientId: veoSecondaryClientId, name: 'veo second REST test client', type: ClientChangeType.CREATION,
                domainProducts : ["DS-GVO": ["EXAMPLE_ORGANIZATION"], "test-domain": []]])
            new PollingConditions().within(10) {
                getDomains()
                get("/domains", 200, UserType.SECONDARY_CLIENT_USER)
            }
            clientsCreated = true
        }
        dsgvoDomainId = getDomains().find { it.name == "DS-GVO" }.id
        testDomainId = getDomains().find { it.name == "test-domain" }.id
    }

    def sendClientChangeEvent(Map data) {
        eventDispatcher.send(exchange, new EventMessage("${routingKeyPrefix}client_change", JsonOutput.toJson(data+[eventType: 'client_change']), 1, Instant.now()))
    }

    Response get(CharSequence uri, Integer assertStatusCode = 200, UserType userType = UserType.DEFAULT, MediaType mediaType = MediaType.APPLICATION_JSON) {
        def resp = exchange(uri.toString(), HttpMethod.GET, new HttpHeaders().tap{
            put("accept", [mediaType.toString()])
        }, null, userType)
        assertStatusCode?.tap{
            assert resp.statusCodeValue == it
        }
        log.debug("retrieved data: {}", resp.body)
        new Response(
                headers: resp.headers,
                body: jsonSlurper.parseText(resp.body.toString()),
                statusCode: resp.statusCodeValue)
    }

    Response post(String uri, Object requestBody, Integer assertStatusCode = 201, UserType userType = UserType.DEFAULT) {
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        post(uri, headers, requestBody, assertStatusCode, userType)
    }

    Response postMultipart(String uri, Map<String, Object> requestBody, Integer assertStatusCode = 201, UserType userType = UserType.DEFAULT) {
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.MULTIPART_FORM_DATA)
        def resp = exchange(uri, HttpMethod.POST, headers, getMultipartBody(requestBody), userType)
        assertStatusCode?.tap{
            assert resp.statusCodeValue == it
        }
        new Response(
                headers: resp.headers,
                body: jsonSlurper.parseText(resp.body.toString()),
                statusCode: resp.statusCodeValue)
    }

    Response post(String uri, HttpHeaders headers, Object requestBody, Integer assertStatusCode = 201, UserType userType = UserType.DEFAULT) {
        def resp = exchange(uri, HttpMethod.POST, headers, requestBody?.with { toJson(it) }, userType)
        assertStatusCode?.tap{
            assert resp.statusCodeValue == it
        }
        new Response(
                headers: resp.headers,
                body: jsonSlurper.parseText(resp.body.toString()),
                statusCode: resp.statusCodeValue)
    }

    Response put(String uri, Object requestBody, String etag = null, Integer assertStatusCode = 200, UserType userType = UserType.DEFAULT) {
        HttpHeaders headers = new HttpHeaders()
        etag?.with { headers.setIfMatch(it) }
        headers.setContentType(MediaType.APPLICATION_JSON)

        def resp = exchange(uri, HttpMethod.PUT, headers, requestBody?.with { toJson(it) }, userType)
        assertStatusCode?.tap{
            assert resp.statusCodeValue == it
        }
        new Response(
                headers: resp.headers,
                body: jsonSlurper.parseText(resp.body.toString()),
                statusCode: resp.statusCodeValue)
    }

    Response delete(String uri, Integer assertStatusCode = 204, UserType userType = UserType.DEFAULT) {
        def resp = exchange(uri, HttpMethod.DELETE, new HttpHeaders(), null, userType)
        assertStatusCode?.tap{
            assert resp.statusCodeValue == it
        }
        new Response(
                headers: resp.headers,
                body: jsonSlurper.parseText(resp.body.toString()),
                statusCode: resp.statusCodeValue)
    }

    def getUnit(id) {
        get("/units/${id}").body
    }

    def postNewUnit(String unitName = "${this.class.simpleName} unit", List<String> domainIDs = getDomains()*.id) {
        post("/units", [
            name: unitName,
            domains: domainIDs.collect {
                [targetUri: "/domains/${it}"]
            }
        ]).body
    }

    def getDomains() {
        get("/domains").body
    }

    def exportDomain(id) {
        get("/domains/${id}/export").body
    }

    Collection<Object> getCatalogItems(String domainId) {
        get("/domains/$domainId/catalog-items?size=9000").body.items
    }

    def getControl(id) {
        get("/controls/${id}").body
    }

    ResponseEntity<String> exchange(String uri, HttpMethod httpMethod, HttpHeaders headers, Object requestBody = null, UserType userType = UserType.DEFAULT) {
        headers.put("Authorization", [
            "Bearer " + getToken(userType)
        ])
        def absoluteUri = uri.startsWith('http') ? uri : baseUrl + uri
        return restTemplate.exchange(absoluteUri, httpMethod, new HttpEntity(requestBody, headers), String.class)
    }

    ResponseEntity<String> exchange(URI uri, HttpMethod httpMethod, HttpHeaders headers, Object requestBody = null, UserType userType = UserType.DEFAULT) {
        headers.put("Authorization", [
            "Bearer " + getToken(userType)
        ])
        return restTemplate.exchange(uri, httpMethod, new HttpEntity(requestBody?.with { toJson(it) }, headers), String.class)
    }

    protected String getToken(UserType userType) {
        def user = defaultUserName
        def pass = defaultUserPass
        if (userType == UserType.ADMIN) {
            user = adminUserName
            pass = adminUserPass
        }
        else if (userType == UserType.CONTENT_CREATOR) {
            user = contentCreatorUserName
            pass = contentCreatorUserPass
        } else if (userType == UserType.READ_ONLY) {
            user = readOnlyUserName
            pass = readOnlyUserPass
        }
        else if (userType == UserType.SECONDARY_CLIENT_USER) {
            user = secondaryClientUserName
            pass = secondaryClientUserPass
        }
        if (userTokenCache.hasProperty(user)) {
            return userTokenCache[user]
        }
        def proxy = new HttpHost(proxyHost, proxyPort)
        def newToken = HttpClientBuilder.create().with {
            it.proxy = proxy
            build()
        }.withCloseable {
            Configuration configuration = new Configuration(oidcUrl, realm, clientId, ['secret': ''], it)
            AuthzClient authzClient = AuthzClient.create(configuration)
            def accessTokenResponse = authzClient.obtainAccessToken(user, pass)
            accessTokenResponse.token
        }
        userTokenCache[user] = newToken
        return newToken
    }

    /**
     * Create a copy of domain with given ID. The copy does not reference the same domain template.
     * @return ID of domain copy
     * */
    def copyDomain(String domainId) {
        def domain = get("/domains/$domainId/export").body
        // TODO #2386 use domain import instead of importing as a template
        domain.name = "copy of $domain.name $domain.templateVersion ${randomUUID().toString().subSequence(0, 5)}"
        def templateId = post("/content-creation/domain-templates", domain, 201, CONTENT_CREATOR).body.resourceId
        post("/domain-templates/$templateId/createdomains", null, 204, ADMIN)
        return get("/domains").body.find { it.name == domain.name }.id
    }

    protected uriToId(String targetUri) {
        targetUri.split('/').last()
    }

    protected MultiValueMap<String, Object> getMultipartBody(Map json) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>()
        body.add("file", getResource(json))
        return body
    }

    protected Resource getResource(Map json) {
        def jsonString = JsonOutput.prettyPrint(JsonOutput.toJson(json))
        return new ByteArrayResource(jsonString.getBytes(StandardCharsets.UTF_8)) {
                    @Override
                    public String getFilename() {
                        return "file" // Filename has to be returned in order to be able to post.
                    }
                }
    }
}
