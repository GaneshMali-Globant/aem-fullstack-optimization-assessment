package com.assessment.core.services.impl;

import com.assessment.core.config.WeatherServiceConfig;
import com.assessment.core.services.CacheService;
import com.assessment.core.services.HttpConnectionService;
import com.assessment.core.services.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WeatherServiceImplTest {

    @Mock
    private HttpConnectionService httpConnectionService;

    @Mock
    private CacheService cacheService;

    @Mock
    private WeatherServiceConfig weatherServiceConfig;

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherServiceImpl();
        
        // Use reflection to inject mocks
        try {
            java.lang.reflect.Field field = WeatherServiceImpl.class.getDeclaredField("httpConnectionService");
            field.setAccessible(true);
            field.set(weatherService, httpConnectionService);

            field = WeatherServiceImpl.class.getDeclaredField("cacheService");
            field.setAccessible(true);
            field.set(weatherService, cacheService);
        } catch (Exception e) {
            fail("Failed to inject mocks: " + e.getMessage());
        }
        
        // Setup common mock behavior
        when(weatherServiceConfig.apiKey()).thenReturn("test-api-key");
        when(weatherServiceConfig.endpoint()).thenReturn("https://api.weather.com/weather");
        when(cacheService.get(anyString())).thenReturn(null);
    }

    @Test
    void testGetForecast_WithValidConfig() throws Exception {
        String city = "Bogota";
        String expectedResponse = "{\"temperature\": 25, \"description\": \"Sunny\"}";
        
        when(httpConnectionService.getWithCache(any(URL.class))).thenReturn(expectedResponse);

        String result = weatherService.getForecast(city, weatherServiceConfig);

        assertEquals(expectedResponse, result);
        try {
            verify(httpConnectionService).getWithCache(any(URL.class));
        } catch (Exception e) {
            fail("Verification should not throw exception: " + e.getMessage());
        }
        try {
            verify(cacheService).get(anyString());
        } catch (Exception e) {
            fail("Verification should not throw exception: " + e.getMessage());
        }
        try {
            verify(cacheService).put(anyString(), eq(expectedResponse), eq(30L), any());
        } catch (Exception e) {
            fail("Verification should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void testGetForecast_WithNullCity() throws Exception {
        String expectedResponse = "{\"temperature\": 25, \"description\": \"Sunny\"}";
        
        when(httpConnectionService.getWithCache(any(URL.class))).thenReturn(expectedResponse);

        String result = weatherService.getForecast(null, weatherServiceConfig);

        assertEquals(expectedResponse, result);
        // Should use default city
        try {
            verify(httpConnectionService).getWithCache(argThat(url -> url.toString().contains("Bogota")));
        } catch (Exception e) {
            fail("Verification should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void testGetForecast_WithEmptyCity() throws Exception {
        String expectedResponse = "{\"temperature\": 25, \"description\": \"Sunny\"}";
        
        when(httpConnectionService.getWithCache(any(URL.class))).thenReturn(expectedResponse);

        String result = weatherService.getForecast("", weatherServiceConfig);

        assertEquals(expectedResponse, result);
        // Should use default city
        try {
            verify(httpConnectionService).getWithCache(argThat(url -> url.toString().contains("Bogota")));
        } catch (Exception e) {
            fail("Verification should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void testGetForecast_WithCacheHit() throws Exception {
        String city = "Bogota";
        String cachedResponse = "{\"temperature\": 20, \"description\": \"Cloudy\"}";

        when(cacheService.get(anyString())).thenReturn(cachedResponse);

        String result = weatherService.getForecast(city, weatherServiceConfig);

        assertEquals(cachedResponse, result);
        try {
            verify(cacheService).get(anyString());
        } catch (Exception e) {
            fail("Verification should not throw exception: " + e.getMessage());
        }
        try {
            verify(httpConnectionService, never()).getWithCache(any(URL.class));
        } catch (Exception e) {
            fail("Verification should not throw exception: " + e.getMessage());
        }
        try {
            verify(cacheService, never()).put(anyString(), anyString(), anyLong(), any());
        } catch (Exception e) {
            fail("Verification should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void testGetForecast_WithNullConfig() {
        String city = "Bogota";

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            weatherService.getForecast(city, null);
        });

        assertEquals("WeatherServiceConfig must be provided", exception.getMessage());
    }

    @Test
    void testGetForecast_WithNullApiKey() {
        String city = "Bogota";
        when(weatherServiceConfig.apiKey()).thenReturn(null);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            weatherService.getForecast(city, weatherServiceConfig);
        });

        assertEquals("API key cannot be null or empty", exception.getMessage());
    }

    @Test
    void testGetForecast_WithEmptyApiKey() {
        String city = "Bogota";
        when(weatherServiceConfig.apiKey()).thenReturn("");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            weatherService.getForecast(city, weatherServiceConfig);
        });

        assertEquals("API key cannot be null or empty", exception.getMessage());
    }

    @Test
    void testGetForecast_WithNullEndpoint() {
        String city = "Bogota";
        when(weatherServiceConfig.endpoint()).thenReturn(null);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            weatherService.getForecast(city, weatherServiceConfig);
        });

        assertEquals("Endpoint cannot be null or empty", exception.getMessage());
    }

    @Test
    void testGetForecast_WithEmptyEndpoint() {
        String city = "Bogota";
        when(weatherServiceConfig.endpoint()).thenReturn("");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            weatherService.getForecast(city, weatherServiceConfig);
        });

        assertEquals("Endpoint cannot be null or empty", exception.getMessage());
    }

    @Test
    void testGetForecast_WithHttpException() throws Exception {
        String city = "Bogota";
        when(httpConnectionService.getWithCache(any(URL.class))).thenThrow(new RuntimeException("Network error"));

        Exception exception = assertThrows(Exception.class, () -> {
            weatherService.getForecast(city, weatherServiceConfig);
        });

        assertTrue(exception.getMessage().contains("Weather service unavailable for city: " + city));
        try {
            verify(cacheService, times(2)).get(anyString()); // Called once initially, once for stale cache
        } catch (Exception e) {
            fail("Verification should not throw exception: " + e.getMessage());
        }
        try {
            verify(cacheService, never()).put(anyString(), anyString(), anyLong(), any());
        } catch (Exception e) {
            fail("Verification should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void testGetForecast_WithEmptyResponse() throws Exception {
        String city = "Bogota";
        when(httpConnectionService.getWithCache(any(URL.class))).thenReturn("");
        when(cacheService.get(anyString())).thenReturn(null); // No stale cache available

        Exception exception = assertThrows(Exception.class, () -> {
            weatherService.getForecast(city, weatherServiceConfig);
        });

        assertTrue(exception.getMessage().contains("Weather service unavailable for city: " + city));
        try {
            verify(httpConnectionService).getWithCache(any(URL.class));
        } catch (Exception e) {
            fail("Verification should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void testGetForecast_WithStaleCacheFallback() throws Exception {
        String city = "Bogota";
        String staleResponse = "{\"temperature\": 20, \"description\": \"Cloudy\"}";
        
        when(httpConnectionService.getWithCache(any(URL.class))).thenThrow(new RuntimeException("Network error"));
        when(cacheService.get(anyString())).thenReturn(null, staleResponse); // First call null, second call with stale key

        String result = weatherService.getForecast(city, weatherServiceConfig);

        assertEquals(staleResponse, result);
        try {
            verify(cacheService, times(2)).get(anyString());
        } catch (Exception e) {
            fail("Verification should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void testGetForecastAsync() throws Exception {
        String city = "Bogota";
        String expectedResponse = "{\"temperature\": 25, \"description\": \"Sunny\"}";
        
        when(httpConnectionService.getWithCache(any(URL.class))).thenReturn(expectedResponse);

        CompletableFuture<String> future = weatherService.getForecastAsync(city, weatherServiceConfig);
        String result = future.get();

        assertEquals(expectedResponse, result);
        try {
            verify(httpConnectionService).getWithCache(any(URL.class));
        } catch (Exception e) {
            fail("Verification should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void testInvalidateCache() {
        String city = "Bogota";

        weatherService.invalidateCache(city);

        // Basic test to ensure method doesn't throw exception
        assertTrue(true, "Invalidate cache should complete without exception");
    }

    @Test
    void testInvalidateCache_WithNullCity() {
        weatherService.invalidateCache(null);
        
        // Basic test to ensure method doesn't throw exception
        assertTrue(true, "Invalidate cache with null city should complete without exception");
    }

    @Test
    void testInvalidateCache_WithEmptyCity() {
        weatherService.invalidateCache("");
        
        // Basic test to ensure method doesn't throw exception
        assertTrue(true, "Invalidate cache with empty city should complete without exception");
    }

    @Test
    void testInvalidateAllCache() {
        weatherService.invalidateAllCache();
        
        // Basic test to ensure method doesn't throw exception
        assertTrue(true, "Invalidate all cache should complete without exception");
    }

    @Test
    void testIsHealthy() throws Exception {
        when(httpConnectionService.getWithCache(any(URL.class))).thenReturn("{\"temperature\": 25}");

        boolean result = weatherService.isHealthy(weatherServiceConfig);

        assertTrue(result);
        try {
            verify(httpConnectionService).getWithCache(any(URL.class));
        } catch (Exception e) {
            fail("Verification should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void testIsHealthy_WithNullConfig() {
        boolean result = weatherService.isHealthy(null);

        assertFalse(result);
    }

    @Test
    void testIsHealthy_WithNullApiKey() {
        when(weatherServiceConfig.apiKey()).thenReturn(null);

        boolean result = weatherService.isHealthy(weatherServiceConfig);

        assertFalse(result);
    }

    @Test
    void testIsHealthy_WithNullEndpoint() {
        when(weatherServiceConfig.endpoint()).thenReturn(null);

        boolean result = weatherService.isHealthy(weatherServiceConfig);

        assertFalse(result);
    }

    @Test
    void testIsHealthy_WithException() throws Exception {
        when(httpConnectionService.getWithCache(any(URL.class))).thenThrow(new RuntimeException("Network error"));

        boolean result = weatherService.isHealthy(weatherServiceConfig);

        assertFalse(result);
    }

    @Test
    void testGetForecast_UrlEncoding() throws Exception {
        String city = "New York";
        String expectedResponse = "{\"temperature\": 15, \"description\": \"Cold\"}";
        
        when(httpConnectionService.getWithCache(any(URL.class))).thenReturn(expectedResponse);

        String result = weatherService.getForecast(city, weatherServiceConfig);

        assertEquals(expectedResponse, result);
        try {
            verify(httpConnectionService).getWithCache(argThat(url -> 
                url.toString().contains("New+York") || url.toString().contains("New%20York")
            ));
        } catch (Exception e) {
            fail("Verification should not throw exception: " + e.getMessage());
        }
    }

    @Test
    void testGetForecast_TrimmedCity() throws Exception {
        String city = "  Bogota  ";
        String expectedResponse = "{\"temperature\": 25, \"description\": \"Sunny\"}";
        
        when(httpConnectionService.getWithCache(any(URL.class))).thenReturn(expectedResponse);

        String result = weatherService.getForecast(city, weatherServiceConfig);

        assertEquals(expectedResponse, result);
        try {
            verify(httpConnectionService).getWithCache(argThat(url -> 
                url.toString().contains("Bogota") && !url.toString().contains("  ")
            ));
        } catch (Exception e) {
            fail("Verification should not throw exception: " + e.getMessage());
        }
    }
}