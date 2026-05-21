package com.assessment.core.config;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
    name = "AEM Weather Service OSGi Configuration",
    description = "Secure OSGi configuration for Weather API service. Use this for production environments to avoid exposing API keys in context-aware configurations."
)
public @interface OSGiWeatherServiceConfig {
    
    @AttributeDefinition(
        name = "API Key",
        description = "Secure API key for weather service. This overrides context-aware configuration.",
        type = AttributeType.PASSWORD
    )
    String apiKey() default "";
    
    @AttributeDefinition(
        name = "API Endpoint",
        description = "Weather service endpoint URL"
    )
    String endpoint() default "https://goweather.xyz/weather";
    
    @AttributeDefinition(
        name = "Enabled",
        description = "Enable/disable weather service"
    )
    boolean enabled() default true;
    
    @AttributeDefinition(
        name = "Cache TTL Minutes",
        description = "Cache time-to-live in minutes"
    )
    long cacheTtlMinutes() default 30;
    
    @AttributeDefinition(
        name = "Connection Timeout Seconds",
        description = "HTTP connection timeout in seconds"
    )
    int connectionTimeoutSeconds() default 10;
    
    @AttributeDefinition(
        name = "Max Retry Attempts",
        description = "Maximum retry attempts for failed requests"
    )
    int maxRetryAttempts() default 3;
    
    @AttributeDefinition(
        name = "Enable Fallback Cache",
        description = "Enable fallback to stale cache when API fails"
    )
    boolean enableFallbackCache() default true;
    
    @AttributeDefinition(
        name = "Service Tenant ID",
        description = "Tenant identifier for multi-tenant deployments"
    )
    String tenantId() default "default";
}