package com.chrisleung.notifications.tools.restapi.benchmark;

import java.util.ArrayList;
import java.util.Date;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.chrisleung.notifications.objects.Notification;

public class PostNotificationJob implements Runnable {

    RestTemplate restTemplate;
    HttpEntity<Notification> entity;
    String endPoint;
    ArrayList<Object[]> completedData;
    
    PostNotificationJob(RestTemplate r, String url, HttpHeaders headers, String email, long index, ArrayList<Object[]> c) {
        restTemplate = r;
        endPoint = url;
        Notification obj = new Notification(email,index);
        entity = new HttpEntity<>(obj,headers);
        completedData = c;
    }
    
    @Override
    public void run() {
        ResponseEntity<Response> response = restTemplate.exchange(endPoint, HttpMethod.POST, entity, Response.class);
        Object[] completedInfo = new Object[] {new Date(), response.getBody().getId()};
        synchronized(completedData) {
            completedData.add(completedInfo);
        }
    }
}
