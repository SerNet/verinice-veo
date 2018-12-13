package org.veo.core;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ComponentScan("org.veo")
@EntityScan("org.veo.model")
@EnableJpaRepositories("org.veo.persistence")
public class VeoCoreConfiguration {

}
