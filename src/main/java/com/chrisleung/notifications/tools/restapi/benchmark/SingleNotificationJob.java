package com.chrisleung.notifications.tools.restapi.benchmark;

import java.util.ArrayList;

import org.springframework.web.client.RestTemplate;

/**
 * Used to perform GET, DELETE, or PUT methods on a single notification.
 * 
 * @author Chris Leung
 */
public abstract class SingleNotificationJob implements Runnable {
    RestTemplate restTemplate;
    String url;
    ArrayList<CompletedRequest> completedRequests;
    
    SingleNotificationJob(RestTemplate r, String endpoint, String id, ArrayList<CompletedRequest> c) {
        restTemplate = r;
        url = endpoint + '/' + id;
        completedRequests = c;
    }
    
    @Override
    public abstract void run();
    
    void addCompletedRequest(CompletedRequest c) {
        synchronized(completedRequests) {
            completedRequests.add(c);
        }
    }
}
