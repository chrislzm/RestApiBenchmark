package com.chrisleung.notifications.tools.restapi.benchmark;

import java.util.ArrayList;
import java.util.Date;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.chrisleung.notifications.objects.Notification;

/**
 * Posts a new notification to the REST API
 * 
 * @author Chris Leung
 */
public class PostNotificationJob extends NotificationJob {

    HttpEntity<Notification> entity;
    
    PostNotificationJob(RestTemplate r, String url, HttpHeaders headers, String email, long variantId, ArrayList<CompletedRequest> c) {
        super(r,url,null,c);
        Notification obj = new Notification(email,variantId);
        entity = new HttpEntity<>(obj,headers);
    }
    
    @Override
    public void run() {
        ResponseEntity<Response> response = restTemplate.exchange(url, HttpMethod.POST, entity, Response.class);
        CompletedRequest completedInfo = new CompletedRequest(new Date(), response.getBody().getId());
        synchronized(completedRequests) {
            completedRequests.add(completedInfo);
        }
    }
}
