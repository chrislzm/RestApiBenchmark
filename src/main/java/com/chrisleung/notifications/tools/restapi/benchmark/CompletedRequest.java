package com.chrisleung.notifications.tools.restapi.benchmark;

import java.util.Date;

/**
 * Stores information about a completed REST API request
 * @author Chris Leung
 *
 */
public class CompletedRequest {
    private Date date; // The date+time the request was completed
    private String id; // The object ID associated with the completed request 
    
    public CompletedRequest(Date date, String id) {
        super();
        this.date = date;
        this.id = id;
    }
    
    public Date getDate() {
        return date;
    }
    public void setDate(Date date) {
        this.date = date;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    
    
}
