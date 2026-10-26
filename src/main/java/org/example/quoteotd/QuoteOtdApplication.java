package org.example.quoteotd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class QuoteOtdApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuoteOtdApplication.class, args);
	}

}
