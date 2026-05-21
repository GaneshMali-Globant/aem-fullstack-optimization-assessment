package com.assessment.core.services;

import com.assessment.core.config.OSGiWeatherServiceConfig;
import com.assessment.core.config.WeatherServiceConfig;
import java.util.concurrent.CompletableFuture;

public interface WeatherService {

    String getForecast(String city) throws Exception;
    
    String getForecast(String city, WeatherServiceConfig config) throws Exception;
    
    String getForecastWithOSGiConfig(String city, OSGiWeatherServiceConfig config) throws Exception;
    
    CompletableFuture<String> getForecastAsync(String city, WeatherServiceConfig config);
    
    void invalidateCache(String city);
    
    void invalidateAllCache();
    
    boolean isHealthy(WeatherServiceConfig config);
}

