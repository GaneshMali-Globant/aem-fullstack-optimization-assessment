package com.assessment.core.models;

import com.assessment.core.config.WeatherServiceConfig;
import com.assessment.core.config.WeatherServiceConfigInterface;
import com.assessment.core.services.WeatherService;
import com.day.cq.wcm.api.Page;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WeatherModelTest {

    @Mock
    private WeatherService weatherService;

    @Mock
    private Page currentPage;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Resource contentResource;

    @Mock
    private ConfigurationBuilder configurationBuilder;

    @Mock
    private WeatherServiceConfig weatherServiceConfig;

    private WeatherModel weatherModel;

    @BeforeEach
    void setUp() throws Exception {
        weatherModel = new WeatherModel();
        
        // Setup common mocks
        when(currentPage.getPath()).thenReturn("/content/weather");
        when(resourceResolver.getResource(anyString())).thenReturn(contentResource);
        when(contentResource.adaptTo(ConfigurationBuilder.class)).thenReturn(configurationBuilder);
        when(configurationBuilder.as(WeatherServiceConfig.class)).thenReturn(weatherServiceConfig);
        when(weatherServiceConfig.apiKey()).thenReturn("test-api-key");
        when(weatherServiceConfig.endpoint()).thenReturn("https://api.weather.com/weather");
        when(weatherService.getForecast(anyString(), any(WeatherServiceConfig.class)))
            .thenReturn("{\"temperature\": 25, \"city\": \"Bogota\"}");
        when(currentPage.getTitle()).thenReturn("Weather Test Page");
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void testPostConstruct_WithCity() throws Exception {
        // Given
        setPrivateField(weatherModel, "weatherService", weatherService);
        setPrivateField(weatherModel, "resourceResolver", resourceResolver);
        setPrivateField(weatherModel, "currentPage", currentPage);
        setPrivateField(weatherModel, "city", "Bogota");

        // When
        weatherModel.postConstruct();

        // Then
        assertNotNull(weatherModel.getWeatherJson());
        assertEquals("{\"temperature\": 25, \"city\": \"Bogota\"}", weatherModel.getWeatherJson());
        verify(weatherService).getForecast(eq("Bogota"), any(WeatherServiceConfig.class));
        verify(configurationBuilder).as(WeatherServiceConfig.class);
    }

    @Test
    void testPostConstruct_WithDefaultCity() throws Exception {
        // Given
        setPrivateField(weatherModel, "weatherService", weatherService);
        setPrivateField(weatherModel, "resourceResolver", resourceResolver);
        setPrivateField(weatherModel, "currentPage", currentPage);
        setPrivateField(weatherModel, "city", null);

        // When
        weatherModel.postConstruct();

        // Then
        assertNotNull(weatherModel.getWeatherJson());
        verify(weatherService).getForecast(eq("Bogota"), any(WeatherServiceConfig.class));
    }

    @Test
    void testPostConstruct_WithEmptyCity() throws Exception {
        // Given
        setPrivateField(weatherModel, "weatherService", weatherService);
        setPrivateField(weatherModel, "resourceResolver", resourceResolver);
        setPrivateField(weatherModel, "currentPage", currentPage);
        setPrivateField(weatherModel, "city", "");

        // When
        weatherModel.postConstruct();

        // Then
        assertNotNull(weatherModel.getWeatherJson());
        verify(weatherService).getForecast(eq("Bogota"), any(WeatherServiceConfig.class));
    }

    @Test
    void testGetContextAwareConfigsForWeather_ValidPath() throws Exception {
        // Given
        setPrivateField(weatherModel, "resourceResolver", resourceResolver);

        // When
        WeatherServiceConfig result = weatherModel.getContextAwareConfigsForWeather("/content/weather", resourceResolver);

        // Then
        assertNotNull(result);
        verify(resourceResolver).getResource("/content/weather");
        verify(contentResource).adaptTo(ConfigurationBuilder.class);
        verify(configurationBuilder).as(WeatherServiceConfig.class);
    }

    @Test
    void testGetContextAwareConfigsForWeather_NullPath() throws Exception {
        setPrivateField(weatherModel, "resourceResolver", resourceResolver);
        WeatherServiceConfig result = weatherModel.getContextAwareConfigsForWeather(null, resourceResolver);
        assertNull(result);
        verify(resourceResolver, never()).getResource(anyString());
    }

    @Test
    void testGetContextAwareConfigsForWeather_EmptyPath() throws Exception {
        // Given
        setPrivateField(weatherModel, "resourceResolver", resourceResolver);

        // When
        WeatherServiceConfig result = weatherModel.getContextAwareConfigsForWeather("", resourceResolver);

        // Then
        assertNull(result);
        verify(resourceResolver, never()).getResource(anyString());
    }

    @Test
    void testGetContextAwareConfigsForWeather_NullResource() throws Exception {
        // Given
        setPrivateField(weatherModel, "resourceResolver", resourceResolver);
        when(resourceResolver.getResource(anyString())).thenReturn(null);

        // When
        WeatherServiceConfig result = weatherModel.getContextAwareConfigsForWeather("/content/weather", resourceResolver);

        // Then
        assertNull(result);
        verify(resourceResolver).getResource("/content/weather");
    }

    @Test
    void testGetContextAwareConfigsForWeather_NullConfigurationBuilder() throws Exception {
        // Given
        setPrivateField(weatherModel, "resourceResolver", resourceResolver);
        when(contentResource.adaptTo(ConfigurationBuilder.class)).thenReturn(null);

        // When
        WeatherServiceConfig result = weatherModel.getContextAwareConfigsForWeather("/content/weather", resourceResolver);

        // Then
        assertNull(result);
        verify(contentResource).adaptTo(ConfigurationBuilder.class);
    }

    @Test
    void testGetPageTitle_WithTitle() throws Exception {
        // Given
        setPrivateField(weatherModel, "currentPage", currentPage);

        // When
        String result = weatherModel.getPageTitle();

        // Then
        assertEquals("Weather Test Page", result);
    }

    @Test
    void testGetPageTitle_WithNullPage() throws Exception {
        // Given
        setPrivateField(weatherModel, "currentPage", null);

        // When
        String result = weatherModel.getPageTitle();

        // Then
        assertEquals("Weather Page", result);
    }

    @Test
    void testGetWeatherJson_AfterPostConstruct() throws Exception {
        // Given
        setPrivateField(weatherModel, "weatherService", weatherService);
        setPrivateField(weatherModel, "resourceResolver", resourceResolver);
        setPrivateField(weatherModel, "currentPage", currentPage);
        setPrivateField(weatherModel, "city", "Bogota");
        weatherModel.postConstruct();

        // When
        String result = weatherModel.getWeatherJson();

        // Then
        assertNotNull(result);
        assertEquals("{\"temperature\": 25, \"city\": \"Bogota\"}", result);
    }

    @Test
    void testGetWeatherJson_BeforePostConstruct() {
        // Given
        weatherModel = new WeatherModel();

        // When
        String result = weatherModel.getWeatherJson();

        // Then
        assertNull(result);
    }

    @Test
    void testPostConstruct_WeatherServiceThrowsException() throws Exception {
        // Given
        setPrivateField(weatherModel, "weatherService", weatherService);
        setPrivateField(weatherModel, "resourceResolver", resourceResolver);
        setPrivateField(weatherModel, "currentPage", currentPage);
        setPrivateField(weatherModel, "city", "Bogota");
        when(weatherService.getForecast(anyString(), any(WeatherServiceConfig.class)))
            .thenThrow(new RuntimeException("Network error"));

        // When
        weatherModel.postConstruct();

        // Then
        assertTrue(weatherModel.hasError());
        assertNull(weatherModel.getWeatherJson());
        assertEquals("Weather service is temporarily unavailable. Please try again later.", weatherModel.getErrorMessage());
        assertEquals("DATA_RETRIEVAL", weatherModel.getErrorType());
    }

    @Test
    void testGetContextAwareConfigsForWeather_InterfaceImplementation() throws Exception {
        // Given
        WeatherServiceConfigInterface configInterface = new WeatherModel();
        weatherModel = new WeatherModel();
        setPrivateField(weatherModel, "resourceResolver", resourceResolver);

        // When
        WeatherServiceConfig result = configInterface.getContextAwareConfigsForWeather("/content/test", resourceResolver);

        // Then
        assertNotNull(result);
        verify(resourceResolver).getResource("/content/test");
    }

    @Test
    void testResourceType() {
        // Given & When
        String resourceType = WeatherModel.RESOURCE_TYPE;

        // Then
        assertEquals("assessment/components/weather", resourceType);
    }

    @Test
    void testIsConfigured_Success() throws Exception {
        // Given
        setPrivateField(weatherModel, "weatherService", weatherService);
        setPrivateField(weatherModel, "resourceResolver", resourceResolver);
        setPrivateField(weatherModel, "currentPage", currentPage);
        setPrivateField(weatherModel, "city", "Bogota");
        weatherModel.postConstruct();

        // When & Then
        assertTrue(weatherModel.isConfigured());
    }

    @Test
    void testIsConfigured_Failure() throws Exception {
        // Given
        setPrivateField(weatherModel, "weatherService", weatherService);
        setPrivateField(weatherModel, "resourceResolver", resourceResolver);
        setPrivateField(weatherModel, "currentPage", currentPage);
        setPrivateField(weatherModel, "city", "Bogota");
        when(weatherService.getForecast(anyString(), any(WeatherServiceConfig.class)))
            .thenThrow(new RuntimeException("Network error"));
        weatherModel.postConstruct();

        // When & Then
        assertFalse(weatherModel.isConfigured());
    }

    @Test
    void testGetCity() throws Exception {
        // Given
        setPrivateField(weatherModel, "city", "  New York  ");

        // When
        String result = weatherModel.getCity();

        // Then
        assertEquals("New York", result);
    }

    @Test
    void testGetCity_Default() throws Exception {
        // Given
        setPrivateField(weatherModel, "city", null);

        // When
        String result = weatherModel.getCity();

        // Then
        assertEquals("Bogota", result);
    }

    @Test
    void testIsServiceHealthy() throws Exception {
        // Given
        setPrivateField(weatherModel, "weatherService", weatherService);
        setPrivateField(weatherModel, "resourceResolver", resourceResolver);
        setPrivateField(weatherModel, "currentPage", currentPage);
        when(weatherService.isHealthy(any(WeatherServiceConfig.class))).thenReturn(true);

        // When
        boolean result = weatherModel.isServiceHealthy();

        // Then
        assertTrue(result);
        verify(weatherService).isHealthy(weatherServiceConfig);
    }
}