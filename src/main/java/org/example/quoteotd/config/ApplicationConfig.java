package org.example.quoteotd.config;

import java.util.Random;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ApplicationConfig {

	@Bean
	Random random() {
		return new Random();
	}
}
