package com.chrisleung.notifications.tools.restapi.benchmark;

import java.util.ArrayList;
import java.util.Date;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class DeleteNotificationRequest extends Request {

    DeleteNotificationRequest(RestTemplate r, String endpoint, String id, ArrayList<CompletedRequest> c) {
        super(r, endpoint, id, c);
    }

    @Override
    public void run() {
        ResponseEntity<PostNotificationResponse> response = restTemplate.exchange(url, HttpMethod.DELETE, null, PostNotificationResponse.class);
        CompletedRequest completedRequest = new CompletedRequest(new Date(), response.getBody().getId());
        addCompletedRequest(completedRequest);
    }
}
