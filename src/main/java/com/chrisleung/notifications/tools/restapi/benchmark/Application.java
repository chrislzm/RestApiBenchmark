package com.chrisleung.notifications.tools.restapi.benchmark;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.web.client.RestTemplate;

/**
 * This class implements a simple benchmarking tool for the Database REST API.
 * It measures performance of the single endpoint exposed to the world:
 * Submittinga new notification.
 * 
 * @author Chris Leung
 */
@SpringBootApplication
public class Application {
        
    static final String REQUEST_TYPE_ALL = "all";
    static final String REQUEST_TYPE_POST = "post";
    static final String REQUEST_TYPE_GET = "get";
    static final String REQUEST_TYPE_UPDATE = "update";
    static final String REQUEST_TYPE_DELETE = "delete";

    private static final Logger log = LoggerFactory.getLogger(Application.class);
    
    @Value("${restapi.endpoint}")
    private String endpoint;
    @Value("${restapi.benchmark.request.total}")
    private int numRequests;
    @Value("${restapi.benchmark.request.concurrent}")
    private int numConcurrent;
    @Value("${restapi.benchmark.request.email}")
    private String email;
    @Value("${restapi.benchmark.timelimit}")
    private int timelimit;
    @Value("${restapi.benchmark.request.type}")
    private String requestType;
    @Value("${restapi.username}")
    private String username;
    @Value("${restapi.password}")
    private String password;
    @Value("${restapi.benchmark.notification.id.output.file}")
    private String outputFilename;

    @Autowired
    private RestTemplate restTemplate;    
    
	public static void main(String args[]) {
		SpringApplication.run(Application.class, args);
	}
		
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
    
	@Bean
	public CommandLineRunner run() throws Exception {
		return args -> {
	        ExecutorService threadpool = Executors.newFixedThreadPool(numConcurrent);

	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.APPLICATION_JSON);
	        
	        ArrayList<Object[]> completedData = new ArrayList<>();
	        completedData.ensureCapacity(numRequests);
	       
		    if(requestType.equals(REQUEST_TYPE_ALL) || requestType.equals(REQUEST_TYPE_POST)) {
		        postBenchmark(threadpool,headers,completedData);
		        completedData.clear();
		    }
		    if(requestType.equals(REQUEST_TYPE_ALL) || requestType.equals(REQUEST_TYPE_GET)) {
		        threadpool = Executors.newFixedThreadPool(numConcurrent);
		        getBenchmark(threadpool,completedData);
		        completedData.clear();
            }
            if(requestType.equals(REQUEST_TYPE_ALL) || requestType.equals(REQUEST_TYPE_UPDATE)) {
                threadpool = Executors.newFixedThreadPool(numConcurrent);
                updateBenchmark(threadpool,headers,completedData);
                completedData.clear();
            }
		    if(requestType.equals(REQUEST_TYPE_ALL) || requestType.equals(REQUEST_TYPE_DELETE)) {
		        threadpool = Executors.newFixedThreadPool(numConcurrent);
                deleteBenchmark(threadpool,completedData);
                completedData.clear();
            }
		};
	}
	
	private void postBenchmark(ExecutorService threadpool,HttpHeaders headers,ArrayList<Object[]> completedData) throws Exception {
         
        Runnable[] jobs = new Runnable[numRequests];
        
        /* Create jobs */
        for(int i=0; i<numRequests; i++) {
            jobs[i] = new PostNotificationJob(restTemplate,endpoint,headers,email,i,completedData);
        }
        
        /* Run jobs */
        runBenchmark(jobs,threadpool,completedData,REQUEST_TYPE_POST);

        /* Write IDs to file */
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilename,true));
        for(Object[] data : completedData) {
            writer.write((String)data[1]+'\n');
        }
        writer.close();
	}
	
	private void getBenchmark(ExecutorService threadpool, ArrayList<Object[]> completedData) throws Exception {
	    setupAuth();
        int numIds = Math.min(getNumIdRecords(), numRequests);
        Scanner scanner = new Scanner(new FileReader(outputFilename));        
        /* Create jobs */
        Runnable[] jobs = new Runnable[numIds];
        for(int i=0; i<numIds; i++) {
            jobs[i] = new GetNotificationJob(restTemplate, endpoint, scanner.next(), completedData);
        }
        scanner.close();
        
        /* Run jobs */
        runBenchmark(jobs,threadpool,completedData,REQUEST_TYPE_GET);
	}
	
	private void updateBenchmark(ExecutorService threadpool,HttpHeaders headers,ArrayList<Object[]> completedData) throws Exception {
        setupAuth();
        int numIds = Math.min(getNumIdRecords(), numRequests);
        Scanner scanner = new Scanner(new FileReader(outputFilename));        
        /* Create jobs */
        Runnable[] jobs = new Runnable[numIds];
        String randomEmail=Math.random()*Integer.MAX_VALUE + '@' + Math.random()*Integer.MAX_VALUE + ".com";
        int randomVariantId = (int)(Math.random() * Integer.MAX_VALUE);
        for(int i=0; i<numIds; i++) {
            jobs[i] = new UpdateNotificationJob(restTemplate, endpoint, scanner.next(), headers, randomEmail, randomVariantId, completedData);
        }
        scanner.close();
        
        /* Run jobs */
        runBenchmark(jobs,threadpool,completedData,REQUEST_TYPE_UPDATE);	    
	}
	
	private void deleteBenchmark(ExecutorService threadpool, ArrayList<Object[]> completedData) throws Exception {
        setupAuth();
        int numIds = Math.min(getNumIdRecords(), numRequests);
        Scanner scanner = new Scanner(new FileReader(outputFilename));
        
        /* Create jobs */
        Runnable[] jobs = new Runnable[numIds];
        for(int i=0; i<numIds; i++) {
            jobs[i] = new DeleteNotificationJob(restTemplate, endpoint, scanner.next(), completedData);
        }
        scanner.close();
        
        /* Run jobs */
        runBenchmark(jobs,threadpool,completedData,REQUEST_TYPE_DELETE);
        
        /* Delete deleted ids from file */
        scanner = new Scanner(new FileReader(outputFilename));
        HashSet<String> remainingIds = new HashSet<>();
        while(scanner.hasNextLine()) {
            remainingIds.add(scanner.nextLine());
        }
        scanner.close();
        for(Object[] data : completedData) {
            String id = (String)data[1];
            if(remainingIds.contains(id)) {
                remainingIds.remove(id);
            }
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilename));
        for(String id : remainingIds) {
            writer.write(id+'\n');
        }
        writer.close();
	}
	
	private void setupAuth() {
        BasicAuthorizationInterceptor auth  = new BasicAuthorizationInterceptor(username, password);
        restTemplate.getInterceptors().add(auth);
	}
	
	private int getNumIdRecords() throws FileNotFoundException {
        Scanner scanner = new Scanner(new FileReader(outputFilename));
        int count = 0;
        while(scanner.hasNextLine()) {
            count++;
            scanner.nextLine();
        }
        scanner.close();
        return count;
	}
	
	private void runBenchmark(Runnable[] jobs, ExecutorService threadpool, ArrayList<Object[]> completedData, String type) throws Exception {
        /* Submit jobs */
        for(Runnable job : jobs) {
            threadpool.execute(job);
        }
        threadpool.shutdown();
        Date start = new Date();
        threadpool.awaitTermination(timelimit, TimeUnit.SECONDS);
        
        /* Find the first job after the shutdown was submitted */
        for(int i=0; i<completedData.size(); i++) {
            if(((Date)completedData.get(i)[0]).getTime() >= start.getTime()) {
                Date first = (Date)completedData.get(i)[0];
                Date last = (Date)completedData.get(completedData.size()-1)[0];
                long elapsed = last.getTime() - first.getTime();
                int numCompleted = completedData.size()-i;
                /* Log Status */
                log.info(String.format("Completed %s %s requests with %s concurrent connections in %s seconds. Average = %s requests/second", numCompleted,type,numConcurrent,elapsed/1000.0f,numCompleted/(elapsed/1000.0f)));
                break;
            }
        }
	}
}