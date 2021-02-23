/*******************************************************************************
 * Copyright (c) 2017 Daniel Murygin.
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
package org.veo.rest;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import org.veo.SpringPropertyLogger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * Main application class for the REST service. Uses JPA repositories.
 *
 * Supports asynchronous method execution by the Spring environment. The
 * taskExecutor bean allows configuring the thread pool that is being used.
 *
 * @see <a href=
 *      "https://spring.io/guides/gs/async-method/">https://spring.io/guides/gs/async-method/</a>
 */
@SpringBootApplication()
@EnableAsync
@Import(VeoRestConfiguration.class)
// @SecurityScheme(name = RestApplication.SECURITY_SCHEME_BEARER_AUTH,
// type = SecuritySchemeType.HTTP,
// scheme = "bearer",
// bearerFormat = "jwt",
// description = "# Please note: \n"
// + "This is a placeholder implementation of a JWT-based authorization. The
// JW-Token has to be created using the '/login' resource. It will be valid for
// ten days."
// + "To acquire a token, send a POST-request to the [/login](/login') endpoint
// with a JSON object of the following format: \n\n"
// + "```\n" + "{\n" + "username: <username>, \n" + "password: <password>\n"
// + "}\n" + "```\n"
// + "The returned JWT must be provided in the HTTP-Bearer fields. You can
// copy-and-paste it into the following form field:")
@SecurityScheme(name = RestApplication.SECURITY_SCHEME_OAUTH,
                type = SecuritySchemeType.OAUTH2,
                in = SecuritySchemeIn.HEADER,
                description = "openidconnect Login",
                flows = @OAuthFlows(implicit = @OAuthFlow(authorizationUrl = "${spring.security.oauth2.resourceserver.jwt.issuer-uri}/protocol/openid-connect/auth",
                                                          scopes = @OAuthScope(name = "veo-datenschutz",
                                                                               description = "Optional scope for access to specific content."))))
@OpenAPIDefinition(info = @Info(title = "verinice.VEO REST API",
                                description = "OpenAPI documentation for verinice.VEO.",
                                license = @License(name = "GNU Lesser General Public License",
                                                   url = "https://www.gnu.org/licenses/lgpl-3.0.de.html"),
                                contact = @Contact(url = "http://verinice.com",
                                                   email = "verinice@sernet.de")))

public class RestApplication {

    public static final String SECURITY_SCHEME_OAUTH = "OAuth2";
    private static final Logger logger = LoggerFactory.getLogger("veo-rest application properties");
    public static final String THREAD_NAME_PREFIX = "Verinice.VEO-Worker-";

    @Autowired
    private ApplicationContext appContext;

    public static void main(String[] args) {
        SpringApplication.run(RestApplication.class, args);
    }

    @Bean
    public TaskExecutor threadPoolTaskExecutor(
            @Value("${veo.threads.corePoolSize:2}") int corePoolSize,
            @Value("${veo.threads.maxPoolSize:4}") int maxPoolSize,
            @Value("${veo.threads.queueCapacity:500}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
        executor.initialize();
        return new DelegatingSecurityContextAsyncTaskExecutor(executor) {
            @PreDestroy
            @SuppressFBWarnings // suppress warning on bean destroy method
            public void shutdown() {
                executor.destroy();
            }
        };
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logSpringProperties() {
        SpringPropertyLogger.logProperties(logger, appContext.getEnvironment());
    }
}
