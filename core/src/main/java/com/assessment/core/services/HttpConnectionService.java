package com.assessment.core.services;

import org.apache.http.HttpResponse;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

public interface HttpConnectionService {

    String get(URL url) throws Exception;
    
    String getWithCache(URL url) throws Exception;
    
    CompletableFuture<String> getAsync(URL url);
    
    HttpResponse post(URL url, String requestBody) throws Exception;
}
