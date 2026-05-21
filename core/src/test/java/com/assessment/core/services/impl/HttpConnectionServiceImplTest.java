package com.assessment.core.services.impl;

import com.assessment.core.services.HttpConnectionService;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HttpConnectionServiceImplTest {

    private HttpConnectionService httpConnectionService;

    @Mock
    private CloseableHttpClient mockHttpClient;

    @Mock
    private CloseableHttpResponse mockHttpResponse;

    @Mock
    private HttpEntity mockHttpEntity;

    @Mock
    private StatusLine mockStatusLine;

    @BeforeEach
    void setUp() throws Exception {
        httpConnectionService = new HttpConnectionServiceImpl();
        
        // Inject mock HttpClient using reflection
        java.lang.reflect.Field httpClientField = HttpConnectionServiceImpl.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(httpConnectionService, mockHttpClient);
    }

    @Test
    void testGet_Success() throws Exception {
        // Given
        String expectedResponse = "{\"temperature\": 25, \"city\": \"Bogota\"}";
        URL testUrl = new URL("https://api.weather.com/weather/Bogota?apikey=test-key");
        
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockHttpResponse);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        when(mockHttpResponse.getEntity()).thenReturn(mockHttpEntity);
        when(mockHttpEntity.getContent()).thenReturn(new ByteArrayInputStream(expectedResponse.getBytes()));
        when(mockHttpEntity.getContentLength()).thenReturn((long) expectedResponse.length());
        
        // When
        String result = httpConnectionService.get(testUrl);
        
        // Then
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(mockHttpClient).execute(any(HttpGet.class));
        verify(mockHttpResponse).getEntity();
        verify(mockHttpEntity).getContent();
    }

    @Test
    void testGet_HttpError() throws Exception {
        // Given
        URL testUrl = new URL("https://api.weather.com/weather/Bogota?apikey=test-key");
        
        // Mock multiple calls due to retry logic
        when(mockHttpClient.execute(any(HttpGet.class))).thenReturn(mockHttpResponse);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(404);
        when(mockStatusLine.getReasonPhrase()).thenReturn("Not Found");
        
        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            httpConnectionService.get(testUrl);
        });
        
        // Should fail after all retries
        assertTrue(exception.getMessage().contains("HTTP request failed after 3 attempts"));
        verify(mockHttpClient, times(3)).execute(any(HttpGet.class));
    }

    @Test
    void testGet_IOException() throws Exception {
        // Given
        URL testUrl = new URL("https://api.weather.com/weather/Bogota?apikey=test-key");
        
        // Mock multiple calls due to retry logic
        when(mockHttpClient.execute(any(HttpGet.class))).thenThrow(new IOException("Network error"));
        
        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            httpConnectionService.get(testUrl);
        });
        
        // Should fail after all retries
        assertTrue(exception.getMessage().contains("HTTP request failed after 3 attempts"));
        verify(mockHttpClient, times(3)).execute(any(HttpGet.class));
    }

    @Test
    void testGet_InvalidUrl() {
        // Given
        URL invalidUrl = null;

        // When & Then
        assertThrows(Exception.class, () -> {
            httpConnectionService.get(invalidUrl);
        });
    }

    @Test
    void testPost_Success() throws Exception {
        // Given
        URL testUrl = new URL("https://api.weather.com/weather");
        String requestBody = "{\"city\": \"Bogota\"}";
        
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockHttpResponse);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        
        // When
        org.apache.http.HttpResponse result = httpConnectionService.post(testUrl, requestBody);
        
        // Then
        assertNotNull(result);
        assertEquals(mockHttpResponse, result);
        verify(mockHttpClient).execute(any(HttpPost.class));
    }

    @Test
    void testPost_EmptyRequestBody() throws Exception {
        // Given
        URL testUrl = new URL("https://api.weather.com/weather");
        String emptyRequestBody = "";
        
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockHttpResponse);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        
        // When
        org.apache.http.HttpResponse result = httpConnectionService.post(testUrl, emptyRequestBody);
        
        // Then
        assertNotNull(result);
        assertEquals(mockHttpResponse, result);
        verify(mockHttpClient).execute(any(HttpPost.class));
    }

    @Test
    void testPost_NullRequestBody() throws Exception {
        // Given
        URL testUrl = new URL("https://api.weather.com/weather");
        
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockHttpResponse);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(200);
        
        // When
        org.apache.http.HttpResponse result = httpConnectionService.post(testUrl, null);
        
        // Then
        assertNotNull(result);
        assertEquals(mockHttpResponse, result);
        verify(mockHttpClient).execute(any(HttpPost.class));
    }

    @Test
    void testPost_HttpError() throws Exception {
        // Given
        URL testUrl = new URL("https://api.weather.com/weather");
        String requestBody = "{\"city\": \"Bogota\"}";
        
        when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockHttpResponse);
        when(mockHttpResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockStatusLine.getStatusCode()).thenReturn(500);
        when(mockStatusLine.getReasonPhrase()).thenReturn("Internal Server Error");
        
        // When - POST method returns HttpResponse even for errors
        org.apache.http.HttpResponse result = httpConnectionService.post(testUrl, requestBody);
        
        // Then
        assertNotNull(result);
        assertEquals(mockHttpResponse, result);
        verify(mockHttpClient).execute(any(HttpPost.class));
        
        // Verify the response has the expected error status
        assertEquals(500, result.getStatusLine().getStatusCode());
        assertEquals("Internal Server Error", result.getStatusLine().getReasonPhrase());
    }

    @Test
    void testPost_InvalidUrl() {
        // Given
        URL invalidUrl = null;
        String requestBody = "{\"test\": \"data\"}";

        // When & Then
        assertThrows(Exception.class, () -> {
            httpConnectionService.post(invalidUrl, requestBody);
        });
    }

    @Test
    void testPost_IOException() throws Exception {
        // Given
        URL testUrl = new URL("https://api.weather.com/weather");
        String requestBody = "{\"city\": \"Bogota\"}";
        
        when(mockHttpClient.execute(any(HttpPost.class))).thenThrow(new IOException("Network error"));
        
        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            httpConnectionService.post(testUrl, requestBody);
        });
        
        assertTrue(exception.getMessage().contains("Network error"));
        verify(mockHttpClient).execute(any(HttpPost.class));
    }

    @Test
    void testServiceConstruction() {
        // Given & When
        HttpConnectionService service = new HttpConnectionServiceImpl();

        // Then
        assertNotNull(service);
    }
}