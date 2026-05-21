package com.assessment.core.services.impl;

import com.assessment.core.services.CacheService;
import com.assessment.core.services.HttpConnectionService;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component(service = HttpConnectionService.class, immediate = true)
public class HttpConnectionServiceImpl implements HttpConnectionService {

    private static final Logger LOG = LoggerFactory.getLogger(HttpConnectionServiceImpl.class);
    
    private static final int DEFAULT_TIMEOUT = 10000; // 10 seconds
    private static final int DEFAULT_RETRY_ATTEMPTS = 3;
    private static final long CACHE_TTL_MINUTES = 30;
    
    private final CloseableHttpClient httpClient;
    
    @Reference
    private CacheService cacheService;

    public HttpConnectionServiceImpl() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(DEFAULT_TIMEOUT)
                .setSocketTimeout(DEFAULT_TIMEOUT)
                .setConnectionRequestTimeout(DEFAULT_TIMEOUT)
                .build();
        
        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    @Override
    public String get(URL url) throws Exception {
        return GET_withRetry(url.toString(), DEFAULT_RETRY_ATTEMPTS);
    }
    
    @Override
    public String getWithCache(URL url) throws Exception {
        String cacheKey = "http_get_" + url.toString().hashCode();
        
        // Try to get from cache first
        String cachedResponse = cacheService.get(cacheKey);
        if (cachedResponse != null) {
            LOG.debug("Returning cached response for URL: {}", url);
            return cachedResponse;
        }
        
        // If not in cache, make the request
        String response = GET_withRetry(url.toString(), DEFAULT_RETRY_ATTEMPTS);
        
        // Cache the response
        if (response != null) {
            cacheService.put(cacheKey, response, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            LOG.debug("Cached response for URL: {}", url);
        }
        
        return response;
    }

    @Override
    public CompletableFuture<String> getAsync(URL url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getWithCache(url);
            } catch (Exception e) {
                LOG.error("Async HTTP request failed for URL: {}", url, e);
                throw new RuntimeException("HTTP request failed", e);
            }
        });
    }

    @Override
    public HttpResponse post(URL url, String requestBody) throws Exception {
        HttpPost httpPost = new HttpPost(url.toURI());
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("User-Agent", "AEM-Weather-Service/1.0");
        
        if (requestBody != null && !requestBody.isEmpty()) {
            StringEntity entity = new StringEntity(requestBody, StandardCharsets.UTF_8);
            httpPost.setEntity(entity);
        }
        
        return httpClient.execute(httpPost);
    }
    
    private String GET_withRetry(String url, int maxAttempts) throws Exception {
        AtomicInteger attempts = new AtomicInteger(0);
        Exception lastException = null;
        
        while (attempts.get() < maxAttempts) {
            attempts.incrementAndGet();
            try {
                HttpGet httpGet = new HttpGet(url);
                httpGet.setHeader("User-Agent", "AEM-Weather-Service/1.0");
                
                try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    
                    if (statusCode >= 200 && statusCode < 300) {
                        HttpEntity entity = response.getEntity();
                        if (entity != null) {
                            String result = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                            EntityUtils.consume(entity);
                            LOG.debug("HTTP GET successful on attempt {} for URL: {}", attempts.get(), url);
                            return result;
                        }
                    } else if (statusCode == 404) {
                        throw new Exception("Resource not found (404) for URL: " + url);
                    } else if (statusCode >= 500) {
                        throw new Exception("Server error (" + statusCode + ") for URL: " + url);
                    } else {
                        throw new Exception("HTTP error (" + statusCode + ") for URL: " + url);
                    }
                }
            } catch (Exception e) {
                lastException = e;
                LOG.warn("HTTP GET attempt {} failed for URL: {}, error: {}", attempts.get(), url, e.getMessage());
                
                if (attempts.get() < maxAttempts) {
                    // Exponential backoff
                    long delayMs = (long) Math.pow(2, attempts.get() - 1) * 1000;
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new Exception("Request interrupted", ie);
                    }
                }
            }
        }
        
        LOG.error("HTTP GET failed after {} attempts for URL: {}", maxAttempts, url, lastException);
        throw new Exception("HTTP request failed after " + maxAttempts + " attempts", lastException);
    }
    
    @SuppressWarnings("deprecation")
    protected void deactivate() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (Exception e) {
            LOG.error("Error closing HTTP client", e);
        }
    }
}
