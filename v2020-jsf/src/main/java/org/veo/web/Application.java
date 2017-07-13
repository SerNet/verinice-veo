/*******************************************************************************
 * Copyright (c) 2017 Urs Zeidler.
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
 *
 * Contributors:
 *     Urs Zeidler uz<at>sernet.de - initial API and implementation
 ******************************************************************************/
package org.veo.web;

import javax.faces.webapp.FacesServlet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * The Spring application.
 * 
 * @author urszeidler
 *
 */
@Configuration
@ComponentScan(basePackages = { "org.veo" })
@EnableAutoConfiguration
public class Application extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }

    @Bean
    public ServletRegistrationBean servletRegistrationBean() {
        ServletRegistrationBean registration = new ServletRegistrationBean(new FacesServlet(),
                new String[] { "*.xhtml", "*.jsf" });
        registration.setLoadOnStartup(1);
        registration.setName("Faces Servlet");
        return registration;
    }

    @Bean
    public ServletContextInitializer servletContextInitializer() {
        return servletContext -> {
            servletContext.setInitParameter("com.sun.faces.forceLoadConfiguration",
                    Boolean.TRUE.toString());
//            servletContext.setInitParameter("primefaces.THEME", "poseidon-verinice");
//            servletContext.setInitParameter("javax.faces.FACELETS_LIBRARIES",
//                    "/taglibs/verinice-taglib.xml;/taglibs/primefaces-poseidon.taglib.xml");
            servletContext.setInitParameter("primefaces.CLIENT_SIDE_VALIDATION",
                    Boolean.TRUE.toString());
            servletContext.setInitParameter("javax.faces.FACELETS_SKIP_COMMENTS",
                    Boolean.TRUE.toString());
//            servletContext.setInitParameter("primefaces.FONT_AWESOME", Boolean.TRUE.toString());
            // servletContext.setInitParameter("primefaces.UPLOADER",
            // "commons");
        };
    }
}
