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
    ArrayList<CompletedRequest> completedData;
    
    SingleNotificationJob(RestTemplate r, String endpoint, String id, ArrayList<CompletedRequest> c) {
        restTemplate = r;
        url = endpoint + '/' + id;
        completedData = c;
    }
    
    @Override
    public abstract void run();
}
