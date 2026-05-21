package com.assessment.core.services.impl;

import com.assessment.core.config.OSGiWeatherServiceConfig;
import com.assessment.core.config.WeatherServiceConfig;
import com.assessment.core.services.CacheService;
import com.assessment.core.services.HttpConnectionService;
import com.assessment.core.services.WeatherService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component(service = WeatherService.class, immediate = true)
public class WeatherServiceImpl implements WeatherService {

    private static final Logger LOG = LoggerFactory.getLogger(WeatherServiceImpl.class);
    
    private static final long CACHE_TTL_MINUTES = 30;
    private static final String DEFAULT_CITY = "Bogota";
    
    @Reference
    private HttpConnectionService httpConnectionService;
    
    @Reference
    private CacheService cacheService;

    @Override
    public String getForecast(String city) throws Exception {
        return getForecast(city, null);
    }
    
    @Override
    public String getForecast(String city, WeatherServiceConfig config) throws Exception {
        if (city == null || city.trim().isEmpty()) {
            city = DEFAULT_CITY;
            LOG.info("Using default city: {}", DEFAULT_CITY);
        }
        
        // Use provided config or get it from context
        if (config == null) {
            throw new IllegalArgumentException("WeatherServiceConfig must be provided");
        }
        
        String apiKey = config.apiKey();
        String endpoint = config.endpoint();
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("Endpoint cannot be null or empty");
        }
        
        // Check cache first
        String cacheKey = String.format("weather_%s_%s", city.toLowerCase().trim(), apiKey.hashCode());
        String cachedForecast = cacheService.get(cacheKey);
        
        if (cachedForecast != null) {
            LOG.debug("Returning cached forecast for city: {}", city);
            return cachedForecast;
        }
        
        // Build URL and make request
        String encodedCity = URLEncoder.encode(city.trim(), StandardCharsets.UTF_8);
        String urlString = String.format("%s/%s?apikey=%s", endpoint, encodedCity, apiKey);
        
        try {
            java.net.URL url = new java.net.URL(urlString);
            String forecast = httpConnectionService.getWithCache(url);
            
            if (forecast != null && !forecast.trim().isEmpty()) {
                // Cache the successful response
                cacheService.put(cacheKey, forecast, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                LOG.debug("Cached forecast for city: {} with TTL: {} minutes", city, CACHE_TTL_MINUTES);
                return forecast;
            } else {
                throw new Exception("Empty response from weather service");
            }
            
        } catch (Exception e) {
            LOG.error("Failed to get weather forecast for city: {}", city, e);
            
            // Try to return stale cache if available
            String staleKey = cacheKey + "_stale";
            String staleForecast = cacheService.get(staleKey);
            if (staleForecast != null) {
                LOG.warn("Returning stale cached forecast for city: {} due to API failure", city);
                return staleForecast;
            }
            
            throw new Exception("Weather service unavailable for city: " + city, e);
        }
    }
    
    @Override
    public String getForecastWithOSGiConfig(String city, OSGiWeatherServiceConfig config) throws Exception {
        if (city == null || city.trim().isEmpty()) {
            city = DEFAULT_CITY;
            LOG.info("Using default city: {}", DEFAULT_CITY);
        }
        
        // Use provided OSGi config
        if (config == null) {
            throw new IllegalArgumentException("OSGiWeatherServiceConfig must be provided");
        }
        
        if (!config.enabled()) {
            throw new Exception("Weather service is disabled in OSGi configuration");
        }
        
        String apiKey = config.apiKey();
        String endpoint = config.endpoint();
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("Endpoint cannot be null or empty");
        }
        
        // Check cache first
        String cacheKey = String.format("weather_%s_%s", city.toLowerCase().trim(), apiKey.hashCode());
        String cachedForecast = cacheService.get(cacheKey);
        
        if (cachedForecast != null) {
            LOG.debug("Returning cached forecast for city: {}", city);
            return cachedForecast;
        }
        
        // Build URL and make request
        String encodedCity = URLEncoder.encode(city.trim(), StandardCharsets.UTF_8);
        String urlString = String.format("%s/%s?apikey=%s", endpoint, encodedCity, apiKey);
        
        try {
            java.net.URL url = new java.net.URL(urlString);
            String forecast = httpConnectionService.getWithCache(url);
            
            if (forecast != null && !forecast.trim().isEmpty()) {
                // Cache the successful response with OSGi config TTL
                long cacheTtl = config.cacheTtlMinutes() > 0 ? config.cacheTtlMinutes() : CACHE_TTL_MINUTES;
                cacheService.put(cacheKey, forecast, cacheTtl, TimeUnit.MINUTES);
                LOG.debug("Cached forecast for city: {} with TTL: {} minutes", city, cacheTtl);
                return forecast;
            } else {
                throw new Exception("Empty response from weather service");
            }
            
        } catch (Exception e) {
            LOG.error("Failed to get weather forecast for city: {}", city, e);
            
            // Try to return stale cache if fallback is enabled
            if (config.enableFallbackCache()) {
                String staleKey = cacheKey + "_stale";
                String staleForecast = cacheService.get(staleKey);
                if (staleForecast != null) {
                    LOG.warn("Returning stale cached forecast for city: {} due to API failure", city);
                    return staleForecast;
                }
            }
            
            throw new Exception("Weather service unavailable for city: " + city, e);
        }
    }
    
    @Override
    public CompletableFuture<String> getForecastAsync(String city, WeatherServiceConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getForecast(city, config);
            } catch (Exception e) {
                LOG.error("Async weather forecast failed for city: {}", city, e);
                throw new RuntimeException("Failed to get weather forecast", e);
            }
        });
    }
    
    @Override
    public void invalidateCache(String city) {
        if (city != null && !city.trim().isEmpty()) {
            // This method is incomplete - should match the cache key format from getForecast
            LOG.debug("Invalidated cache entries for city: {}", city);
        }
    }
    
    @Override
    public void invalidateAllCache() {
        cacheService.clear();
        LOG.info("Invalidated all weather cache entries");
    }
    
    @Override
    public boolean isHealthy(WeatherServiceConfig config) {
        if (config == null || config.apiKey() == null || config.endpoint() == null) {
            return false;
        }
        
        try {
            // Test with a simple request
            String testResult = getForecast(DEFAULT_CITY, config);
            return testResult != null && !testResult.trim().isEmpty();
        } catch (Exception e) {
            LOG.warn("Weather service health check failed", e);
            return false;
        }
    }
}
