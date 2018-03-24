package com.chrisleung.notifications.tools.restapi.benchmark;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
 * This class implements a simple benchmarking tool for a CRUD REST API.
 * 
 * @author Chris Leung
 */
@SpringBootApplication
public class Application {
    
    static final int EMAIL_ADDRESS_LENGTH = 9; // total length will be 2x this plus 4 (@ and .com chars)
    static final float MS_PER_SECOND = 1000;
    static final String ALL_REQUEST_TYPES = "ALL";
    
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    @Value("${restapi.endpoint}")
    private String endpoint;
    @Value("${restapi.benchmark.request.total}")
    private int numRequests;
    @Value("${restapi.benchmark.request.concurrent}")
    private int numConcurrent;
    @Value("${restapi.benchmark.timelimit}")
    private int timelimit;
    @Value("${restapi.benchmark.request.type}")
    private String requestTypeString;
    @Value("${restapi.username}")
    private String username;
    @Value("${restapi.password}")
    private String password;
    @Value("${restapi.benchmark.notification.id.output.file}")
    private String outputFilename;
    @Value("${restapi.benchmark.runs}")
    private int runs;
    @Value("${restapi.benchmark.randomData}")
    private boolean randomData;
    
    // Variables used for log output
    private int totalRequests = 0;
    private long totalElapsedTime = 0;

    // All requests completed/second rates are stored in a list, mapped to the request type
    private Map<RequestType,List<Float>> allRequestRates  = new HashMap<>();
    
    // Used to generate random alphanumeric email addresses
    private RandomString randomString = new RandomString(EMAIL_ADDRESS_LENGTH);
    
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
            
            /* Initialize rate map */
            for(RequestType t : RequestType.values()) {
                allRequestRates.put(t, new ArrayList<>());
            }
            
            /* Run Benchmarks for Requested Tests */
            for(int i=0; i<runs; i++) {
                log.info(String.format("\nREST API Benchmark Run %s/%s", i+1, runs,numConcurrent));
                if(requestTypeString.equals(ALL_REQUEST_TYPES)) {
                    for(RequestType requestType : RequestType.values()) {
                        runBenchmark(requestType);
                    }
                } else if(requestTypeString.equals(RequestType.POST.toString())) {
                    runBenchmark(RequestType.POST);
                } else if(requestTypeString.equals(RequestType.GET.toString())) {
                    runBenchmark(RequestType.GET);
                } else if(requestTypeString.equals(RequestType.PUT.toString())) {
                    runBenchmark(RequestType.PUT);
                } else if(requestTypeString.equals(RequestType.DELETE.toString())) {
                    runBenchmark(RequestType.DELETE);
                }
            }
            
            /* Output Summary */
            float totalDataPoints = 0;
            float overallAverage = 0;
            float overallBest = 0;
            float overallWorst = Float.MAX_VALUE;
            for(RequestType t : RequestType.values()) {
                for(float datapoint : allRequestRates.get(t)) {
                    overallBest = Math.max(datapoint, overallBest);
                    overallWorst = Math.min(datapoint, overallWorst);
                    overallAverage = ((overallAverage*totalDataPoints) + datapoint) / (totalDataPoints+1);
                    totalDataPoints++;
                }
            }
            log.info(String.format("\nSummary: Total Requests = %s, Total Time = %ss, Total Runs = %s, Concurrent Connections = %s, Random Data = %s\nOverall: Best = %s req/s, Worst = %s req/s, Average = %s req/s",totalRequests,totalElapsedTime/1000f,runs,numConcurrent,randomData,overallBest,overallWorst,overallAverage));
        };
    }

    private Runnable[] createJobs(RequestType requestType, ArrayList<CompletedRequest> completedRequests) throws Exception {
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Runnable[] jobs = new Runnable[numRequests];
        int numIds;
        Scanner scanner;
        if(requestType == RequestType.POST) {
            for(int i=0; i<numRequests; i++) {
                jobs[i] = new PostNotificationJob(restTemplate,endpoint,headers,generateRandomEmail(),generateRandomVariantId(),completedRequests);
            }
        } else {
            addHttpAuth();  
            numIds = Math.min(getNumIdRecords(), numRequests);
            scanner = new Scanner(new FileReader(outputFilename));        
            switch(requestType) {
            case GET:                
                for(int i=0; i<numIds; i++) {
                    jobs[i] = new GetNotificationJob(restTemplate, endpoint, scanner.next(), completedRequests);
                }
                break;
            case PUT:
                for(int i=0; i<numIds; i++) {
                    jobs[i] = new PutNotificationJob(restTemplate, endpoint, scanner.next(), headers, generateRandomEmail(), generateRandomVariantId(), completedRequests);
                }
                break;
            case DELETE:
                for(int i=0; i<numIds; i++) {
                    jobs[i] = new DeleteNotificationJob(restTemplate, endpoint, scanner.next(), completedRequests);
                }
                break;
            default:
                throwUnhandledException();
            }
            scanner.close();
        }
        return jobs;
    }
    
    private void runBenchmark(RequestType requestType) throws Exception {

        ArrayList<CompletedRequest> completedRequests = new ArrayList<>();
        completedRequests.ensureCapacity(numRequests);

        ExecutorService threadpool = Executors.newFixedThreadPool(numConcurrent);
        
        /* Create jobs */
        Runnable[] jobs = createJobs(requestType,completedRequests);
        
        /* Submit jobs */
        for(Runnable job : jobs) {
            threadpool.execute(job);
        }
        threadpool.shutdown();
        /* Start the benchmark here, because there is overhead from submitting jobs and ramp-up, which biases request throughput */
        Date benchmarkStart = new Date();
        threadpool.awaitTermination(timelimit, TimeUnit.SECONDS);
        threadpool.shutdownNow();
        
        /* Find the first job after the shutdown was submitted */
        for(int i=0; i<completedRequests.size(); i++) {
            if(completedRequests.get(i).getDate().getTime() >= benchmarkStart.getTime()) {
                Date firstRequest = completedRequests.get(i).getDate();
                Date lastRequest = completedRequests.get(completedRequests.size()-1).getDate();
                long elapsedTime = lastRequest.getTime() - firstRequest.getTime();
                totalElapsedTime += elapsedTime;
                int numCompleted = completedRequests.size()-i;
                totalRequests += numCompleted;
                
                /* Log Status */
                float seconds = elapsedTime/MS_PER_SECOND;
                float currentRate = numCompleted/seconds;
                float bestRate = 0;
                float worstRate = Float.MAX_VALUE;
                List<Float> rates = allRequestRates.get(requestType);
                rates.add(currentRate);
                float averageRate = 0;
                for(float rate : rates) {
                    averageRate += rate;
                    worstRate = Math.min(worstRate, rate);
                    bestRate = Math.max(bestRate, rate);
                }
                averageRate /= rates.size();
                log.info(String.format("\nCompleted %s %s requests in %ss (%s req/s). Best = %s req/s, Worst = %s req/s, Average = %s req/s", numCompleted,requestType.toString(),seconds,currentRate,bestRate,worstRate,averageRate));
                break;
            }
        }
        /* Cleanup / Housekeeping */
        BufferedWriter writer;
        switch(requestType) {
        case POST:
            /* Write added IDs out to file */
            writer = new BufferedWriter(new FileWriter(outputFilename,true));
            for(CompletedRequest completedRequest : completedRequests) {
                writer.write(completedRequest.getId() + '\n');
            }
            writer.close();
            break;
        case DELETE:
            /* Read all ids from output file into a HashSet */
            Scanner scanner = new Scanner(new FileReader(outputFilename));
            HashSet<String> remainingIds = new HashSet<>();
            while(scanner.hasNextLine()) {
                remainingIds.add(scanner.nextLine());
            }
            scanner.close();
            /* Remove deleted ids from the HashSet */
            for(CompletedRequest completedRequest : completedRequests) {
                String id = completedRequest.getId();
                if(remainingIds.contains(id)) {
                    remainingIds.remove(id);
                }
            }            
            /* Output the remaining ids to the file */
            writer = new BufferedWriter(new FileWriter(outputFilename));
            for(String id : remainingIds) {
                writer.write(id+'\n');
            }
            writer.close();
            // Fall through
        default:
            removeHttpAuth();
        }
    }

    private void throwUnhandledException() throws Exception {
        throw new Exception("Unhandled HTTP request method");
    }
    
    private void addHttpAuth() {
        BasicAuthorizationInterceptor auth  = new BasicAuthorizationInterceptor(username, password);
        restTemplate.getInterceptors().add(auth);
    }

    private void removeHttpAuth() {
        restTemplate.getInterceptors().remove(0);
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
    
    private String generateRandomEmail() {
        return randomString.nextString() + '@' + randomString.nextString() + ".com";
    }
    
    private int generateRandomVariantId() {
        return (int)(Math.random() * Integer.MAX_VALUE);
    }
}