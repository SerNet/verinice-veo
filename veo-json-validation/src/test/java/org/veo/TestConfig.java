package org.veo;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * This class configures the Spring container for text execution
 */
@SpringBootConfiguration
@ComponentScan(basePackages = "org.veo")
@EnableAutoConfiguration
public class TestConfig {
    // empty
}