package com.assessment.core.config;

import org.apache.sling.caconfig.annotation.Configuration;
import org.apache.sling.caconfig.annotation.Property;

@Configuration(
    label = "Weather Service Configuration",
    description = "Context-aware configuration for Weather API service. Configure API keys through OSGi configuration for better security."
)
public @interface WeatherServiceConfig {

    @Property(
        label = "API Key",
        description = "The API key for accessing the weather service. Note: For security, configure this through OSGi configuration instead of context-aware config in production."
    )
    String apiKey() default "legacy-weather-api-key-12345";

    @Property(
        label = "API Endpoint",
        description = "The base endpoint URL for the weather service"
    )
    String endpoint() default "https://goweather.xyz/weather";

    @Property(
        label = "Country",
        description = "Default country for weather requests (ISO 3166-1 alpha-2 code)"
    )
    String country() default "us";

    @Property(
        label = "Language",
        description = "Default language for weather responses (ISO 639-1 code)"
    )
    String language() default "en";
    
    @Property(
        label = "Cache TTL Minutes",
        description = "Time-to-live for cached weather data in minutes"
    )
    long cacheTtlMinutes() default 30;
    
    @Property(
        label = "Enable Fallback",
        description = "Enable fallback to stale cache when API is unavailable"
    )
    boolean enableFallback() default true;
    
    @Property(
        label = "Max Retry Attempts",
        description = "Maximum number of retry attempts for failed API calls"
    )
    int maxRetryAttempts() default 3;
}
