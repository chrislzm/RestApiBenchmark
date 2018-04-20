package com.chrisleung.notifications.tools.restapi.benchmark;

import java.util.ArrayList;

import org.springframework.web.client.RestTemplate;

/**
 * Template for a POST, GET, DELETE, or PUT request used to benchmark a REST API.
 * 
 * @author Chris Leung
 */
public abstract class Request implements Runnable {
    RestTemplate restTemplate;
    String url;
    ArrayList<CompletedRequest> completedRequests;
    
    Request(RestTemplate r, String endpoint, String id, ArrayList<CompletedRequest> c) {
        restTemplate = r;
        url = id == null ? endpoint : endpoint + '/' + id;
        completedRequests = c;
    }
    
    /**
     * run() should call addCompletedRequest() with a CompletedRequest object  
     */
    @Override
    public abstract void run();
    
    void addCompletedRequest(CompletedRequest c) {
        synchronized(completedRequests) {
            completedRequests.add(c);
        }
    }
}
