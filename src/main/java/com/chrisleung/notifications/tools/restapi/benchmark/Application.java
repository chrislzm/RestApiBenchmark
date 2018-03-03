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
    private String requestType;
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
    
    private int totalRequests = 0;
    private long totalTime = 0;

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
            ExecutorService threadpool;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ArrayList<Object[]> completedRequests = new ArrayList<>();
            completedRequests.ensureCapacity(numRequests);
            
            /* Initialize rate map */
            for(RequestType t : RequestType.values()) {
                allRequestRates.put(t, new ArrayList<>());
            }
            
            /* Run Benchmarks for Requested Tests */
            for(int i=0; i<runs; i++) {
                log.info(String.format("\nREST API Benchmark Run %s/%s", i+1, runs,numConcurrent));
                if(requestType.equals(RequestType.ALL.toString()) || requestType.equals(RequestType.POST.toString())) {
                    threadpool = Executors.newFixedThreadPool(numConcurrent);
                    postBenchmark(threadpool,headers,completedRequests);
                    completedRequests.clear();
                }
                if(requestType.equals(RequestType.ALL.toString()) || requestType.equals(RequestType.GET.toString())) {
                    threadpool = Executors.newFixedThreadPool(numConcurrent);
                    getBenchmark(threadpool,completedRequests);
                    completedRequests.clear();
                }
                if(requestType.equals(RequestType.ALL.toString()) || requestType.equals(RequestType.PUT.toString())) {
                    threadpool = Executors.newFixedThreadPool(numConcurrent);
                    updateBenchmark(threadpool,headers,completedRequests);
                    completedRequests.clear();
                }
                if(requestType.equals(RequestType.ALL.toString()) || requestType.equals(RequestType.DELETE.toString())) {
                    threadpool = Executors.newFixedThreadPool(numConcurrent);
                    deleteBenchmark(threadpool,completedRequests);
                    completedRequests.clear();
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
            log.info(String.format("\nSummary: Total Requests = %s, Total Time = %ss, Total Runs = %s, Concurrent Connections = %s, Random Data = %s\nOverall: Best = %s req/s, Worst = %s req/s, Average = %s req/s",totalRequests,totalTime/1000f,runs,numConcurrent,randomData,overallBest,overallWorst,overallAverage));
        };
    }

    private void postBenchmark(ExecutorService threadpool,HttpHeaders headers,ArrayList<Object[]> completedRequests) throws Exception {

        Runnable[] jobs = new Runnable[numRequests];

        /* Create jobs */
        for(int i=0; i<numRequests; i++) {
            jobs[i] = new PostNotificationJob(restTemplate,endpoint,headers,generateRandomEmail(),generateRandomVariantId(),completedRequests);
        }

        /* Run jobs */
        runBenchmark(jobs,threadpool,completedRequests,RequestType.POST);

        /* Output IDs to file */
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilename,true));
        for(Object[] data : completedRequests) {
            writer.write((String)data[1]+'\n');
        }
        writer.close();
    }

    private void getBenchmark(ExecutorService threadpool, ArrayList<Object[]> completedRequests) throws Exception {
        setupHttpAuth();
        
        /* Create jobs */
        int numIds = Math.min(getNumIdRecords(), numRequests);
        Scanner scanner = new Scanner(new FileReader(outputFilename));        
        Runnable[] jobs = new Runnable[numIds];
        for(int i=0; i<numIds; i++) {
            jobs[i] = new GetNotificationJob(restTemplate, endpoint, scanner.next(), completedRequests);
        }
        scanner.close();

        /* Run jobs */
        runBenchmark(jobs,threadpool,completedRequests,RequestType.GET);
    }

    private void updateBenchmark(ExecutorService threadpool,HttpHeaders headers,ArrayList<Object[]> completedRequests) throws Exception {
        setupHttpAuth();
        
        /* Create jobs */
        int numIds = Math.min(getNumIdRecords(), numRequests);
        Scanner scanner = new Scanner(new FileReader(outputFilename));        
        Runnable[] jobs = new Runnable[numIds];
        for(int i=0; i<numIds; i++) {
            jobs[i] = new UpdateNotificationJob(restTemplate, endpoint, scanner.next(), headers, generateRandomEmail(), generateRandomVariantId(), completedRequests);
        }
        scanner.close();

        /* Run jobs */
        runBenchmark(jobs,threadpool,completedRequests,RequestType.PUT);	    
    }

    private void deleteBenchmark(ExecutorService threadpool, ArrayList<Object[]> completedRequests) throws Exception {
        setupHttpAuth();
        
        /* Create jobs */
        int numIds = Math.min(getNumIdRecords(), numRequests);
        Scanner scanner = new Scanner(new FileReader(outputFilename));
        Runnable[] jobs = new Runnable[numIds];
        for(int i=0; i<numIds; i++) {
            jobs[i] = new DeleteNotificationJob(restTemplate, endpoint, scanner.next(), completedRequests);
        }
        scanner.close();

        /* Run jobs */
        runBenchmark(jobs,threadpool,completedRequests,RequestType.DELETE);

        /* Read all ids from output file into a HashSet */
        scanner = new Scanner(new FileReader(outputFilename));
        HashSet<String> remainingIds = new HashSet<>();
        while(scanner.hasNextLine()) {
            remainingIds.add(scanner.nextLine());
        }
        scanner.close();
        
        /* Remove deleted ids from the HashSet */
        for(Object[] data : completedRequests) {
            String id = (String)data[1];
            if(remainingIds.contains(id)) {
                remainingIds.remove(id);
            }
        }
        
        /* Output the remaining ids to the file */
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilename));
        for(String id : remainingIds) {
            writer.write(id+'\n');
        }
        writer.close();
    }

    private void runBenchmark(Runnable[] jobs, ExecutorService threadpool, ArrayList<Object[]> completedRequests, RequestType requestType) throws Exception {
        /* Submit jobs */
        for(Runnable job : jobs) {
            threadpool.execute(job);
        }
        threadpool.shutdown();
        /* Start the benchmark here, because there is overhead from submitting jobs, which slows our request throughput */
        Date benchmarkStart = new Date();
        threadpool.awaitTermination(timelimit, TimeUnit.SECONDS);

        /* Find the first job after the shutdown was submitted */
        for(int i=0; i<completedRequests.size(); i++) {
            if(((Date)completedRequests.get(i)[0]).getTime() >= benchmarkStart.getTime()) {
                Date firstRequest = (Date)completedRequests.get(i)[0];
                Date lastRequest = (Date)completedRequests.get(completedRequests.size()-1)[0];
                long elapsedTime = lastRequest.getTime() - firstRequest.getTime();
                totalTime += elapsedTime;
                int numCompleted = completedRequests.size()-i;
                totalRequests += numCompleted;
                /* Log Status */
                float seconds = elapsedTime/1000.0f;
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
    }
    
    private void setupHttpAuth() {
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
    
    private String generateRandomEmail() {
        return randomString.nextString() + '@' + randomString.nextString() + ".com";
    }
    
    private int generateRandomVariantId() {
        return (int)(Math.random() * Integer.MAX_VALUE);
    }
}