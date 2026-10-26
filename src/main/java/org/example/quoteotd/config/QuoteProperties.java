package org.example.quoteotd.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Application tuning knobs. {@code quote-otd.k-max} caps how many recently
 * shown quotes are excluded from eligibility.
 */
@ConfigurationProperties(prefix = "quote-otd")
public record QuoteProperties(@DefaultValue("50") int kMax) {
}
