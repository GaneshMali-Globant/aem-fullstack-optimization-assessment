package com.assessment.core.config;

import org.apache.sling.api.resource.ResourceResolver;

public interface WeatherServiceConfigInterface {
    public WeatherServiceConfig getContextAwareConfigsForWeather(String currentPage, ResourceResolver resourceResolver);
}
