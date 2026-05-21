package com.assessment.core.models;

import com.assessment.core.config.OSGiWeatherServiceConfig;
import com.assessment.core.config.WeatherServiceConfig;
import com.assessment.core.config.WeatherServiceConfigInterface;
import com.assessment.core.services.WeatherService;
import com.day.cq.wcm.api.Page;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@Model(
        adaptables = {SlingHttpServletRequest.class},
        adapters = {WeatherServiceConfigInterface.class},
        resourceType = {WeatherModel.RESOURCE_TYPE},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class WeatherModel implements WeatherServiceConfigInterface {
    
    private static final Logger LOG = LoggerFactory.getLogger(WeatherModel.class);
    private static final String DEFAULT_CITY = "Bogota";
    private static final String DEFAULT_TITLE = "Weather Page";
    
    protected static final String RESOURCE_TYPE = "assessment/components/weather";

    @SlingObject
    private ResourceResolver resourceResolver;

    @ScriptVariable
    private Page currentPage;

    @Inject
    private String city;

    @OSGiService
    private WeatherService weatherService;

    @OSGiService
    private OSGiWeatherServiceConfig osgiConfig;

    private String weatherJson;
    private String errorMessage;
    private boolean hasError = false;
    private String errorType;
    
    private static final String ERROR_TYPE_SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
    private static final String ERROR_TYPE_CONFIGURATION_NOT_FOUND = "CONFIGURATION_NOT_FOUND";
    private static final String ERROR_TYPE_DATA_RETRIEVAL = "DATA_RETRIEVAL";
    private static final String ERROR_TYPE_UNEXPECTED = "UNEXPECTED";

    @PostConstruct
    public void postConstruct() {
        try {
            if (!validateAndInitialize()) {
                hasError = true;
                weatherJson = null;
                errorType = ERROR_TYPE_SERVICE_UNAVAILABLE;
                errorMessage = "Weather service is currently unavailable. The service may be down for maintenance. Please try again later.";
                return;
            }
            
            String requestedCity = StringUtils.isNotBlank(city) ? city.trim() : DEFAULT_CITY;
            
            WeatherServiceConfig config = getContextAwareConfigsForWeather(
                currentPage != null ? currentPage.getPath() : null, 
                resourceResolver
            );
            
            // Fallback to OSGi configuration if context-aware config is not available
            if (config == null) {
                LOG.info("Context-aware WeatherServiceConfig not found, using OSGi configuration fallback");
                if (osgiConfig != null) {
                    LOG.info("Using OSGi configuration: apiKey={}, endpoint={}, enabled={}", 
                        osgiConfig.apiKey(), osgiConfig.endpoint(), osgiConfig.enabled());
                    
                    // Use OSGi config directly with the weather service
                    try {
                        weatherJson = weatherService.getForecastWithOSGiConfig(requestedCity, osgiConfig);
                        LOG.debug("Successfully retrieved weather data using OSGi config for city: {}", requestedCity);
                        return;
                    } catch (Exception e) {
                        LOG.error("Failed to retrieve weather data using OSGi config for city: {}", requestedCity, e);
                        hasError = true;
                        errorType = ERROR_TYPE_DATA_RETRIEVAL;
                        errorMessage = "Weather service is temporarily unavailable. Please try again later.";
                        
                        // Try to invalidate cache for this city to force fresh data next time
                        try {
                            weatherService.invalidateCache(requestedCity);
                        } catch (Exception cacheEx) {
                            LOG.warn("Failed to invalidate cache for city: {}", requestedCity, cacheEx);
                        }
                        return;
                    }
                } else {
                    LOG.error("Both context-aware and OSGi WeatherServiceConfig not found");
                    hasError = true;
                    errorType = ERROR_TYPE_CONFIGURATION_NOT_FOUND;
                    errorMessage = "Weather service configuration not found. Please contact your administrator.";
                    return;
                }
            }
            
            try {
                weatherJson = weatherService.getForecast(requestedCity, config);
                LOG.debug("Successfully retrieved weather data for city: {}", requestedCity);
            } catch (Exception e) {
                LOG.error("Failed to retrieve weather data for city: {}", requestedCity, e);
                hasError = true;
                errorType = ERROR_TYPE_DATA_RETRIEVAL;
                errorMessage = "Weather service is temporarily unavailable. Please try again later.";
                
                // Try to invalidate cache for this city to force fresh data next time
                try {
                    weatherService.invalidateCache(requestedCity);
                } catch (Exception cacheEx) {
                    LOG.warn("Failed to invalidate cache for city: {}", requestedCity, cacheEx);
                }
            }
            
        } catch (Exception e) {
            LOG.error("Unexpected error in WeatherModel initialization", e);
            hasError = true;
            errorType = ERROR_TYPE_UNEXPECTED;
            errorMessage = "An unexpected error occurred. Please try again later.";
        }
    }
    
    private boolean validateAndInitialize() {
        if (weatherService == null) {
            LOG.error("WeatherService is not available");
            return false;
        }
        
        if (resourceResolver == null) {
            LOG.error("ResourceResolver is not available");
            return false;
        }
        
        return true;
    }

    public WeatherServiceConfig getContextAwareConfigsForWeather(String currentPagePath, ResourceResolver resourceResolver) {
        if (StringUtils.isBlank(currentPagePath)) {
            LOG.debug("Current page path is null or empty");
            return null;
        }
        
        try {
            Resource contentResource = resourceResolver.getResource(currentPagePath);
            if (contentResource == null) {
                LOG.warn("Resource not found for path: {}", currentPagePath);
                return null;
            }
            
            ConfigurationBuilder configurationBuilder = contentResource.adaptTo(ConfigurationBuilder.class);
            if (configurationBuilder == null) {
                LOG.warn("ConfigurationBuilder not available for resource: {}", currentPagePath);
                return null;
            }
            
            return configurationBuilder.as(WeatherServiceConfig.class);
            
        } catch (Exception e) {
            LOG.error("Error getting context-aware configuration for path: {}", currentPagePath, e);
            return null;
        }
    }

    public String getWeatherJson() {
        return weatherJson;
    }
    
    public boolean hasError() {
        return hasError;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public String getErrorType() {
        return errorType;
    }
    
    public String getCity() {
        return StringUtils.isNotBlank(city) ? city.trim() : DEFAULT_CITY;
    }
    
    public boolean isConfigured() {
        return !hasError && weatherJson != null;
    }

    public String getPageTitle() {
        return currentPage != null ? currentPage.getTitle() : DEFAULT_TITLE;
    }
    
    public boolean isServiceHealthy() {
        try {
            WeatherServiceConfig config = getContextAwareConfigsForWeather(
                currentPage != null ? currentPage.getPath() : null, 
                resourceResolver
            );
            return config != null && weatherService.isHealthy(config);
        } catch (Exception e) {
            LOG.warn("Error checking service health", e);
            return false;
        }
    }
}