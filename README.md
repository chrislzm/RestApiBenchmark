# REST API Benchmarking Tool

This Java application implements an simple benchmarking tool that measures performance of a CRUD REST API. It separately tests the POST, PUT, GET, and DELETE methods of a REST API endpoint and generates randomized data for the POST and PUT requests. It also maintains a file record of which data has been added to the database so that it can be easily removed later. The number of concurrent connections, requests, test runs and time limit are all configurable.

Screenshot:

![REST API Benchmarking Tool Screenshot](doc/screenshot.png "REST API Benchmarking Tool Screenshot")

## Prerequisites

1. Java
2. Gradle
3. Internet connection

## Installing

1. Rename `application.properties.blank` to `application.properties` and update values (see Configuration below for additional notes)
    * Important: These properties are used throughout `Application.java`
2. Code each benchmark test
    * Extend the `Request` class. Use the `PostNotificationRequest`, `GetNotificationRequest`, `PutNotificationRequest`, and `DeleteNotificationRequest` classes as example implementations. These classes can be used directly to benchmark the [Back in Stock Database REST API](https://github.com/chrislzm/BackInStock/tree/master/RestApi), a component of the [Back in Stock](https://github.com/chrislzm/BackInStock) project    
    * Update the `createRequests` method in `Application.java` to properly create and initialize your tests
3. Compile the application
    * Run the command `./gradlew bootRun` (will compile and run) or
    * Build an executable JAR file using `./gradlew build`, which will create a JAR file at `./build/libs/restapi-benchmark-0.1.0.jar`

### Configuration

For a full list of options, see the [`application.properties`](src/main/resources/application.properties.blank) configuration file.
* `restapi.benchmark.notification.id.output.file`: This file will store the IDs of any objects stored to the REST API. The purpose is so that these objects can be removed from your REST API later, and so that no data currently in database is affected. To delete the data from the REST API's database, simply run a DELETE benchmark and ensure that the `restapi.benchmark.request.total` setting is greater than or equal to the number of ids in the file. The file will be updated as the objects are deleted. If the file is empty, that means that there are no objects that created by the benchmarking tool currently stored in your REST API's database.

Please note that any or all of the settings in `application.properties` can also be overridden with command line arguments. Example: `--restapi.benchmark.request.total=1000`

## Deployment

For the most accurate results, use a high-throughput low-latency Internet connection and a multi-core CPU with plenty of memory. It's highly recommended that you monitor CPU, memory and network bandwidth usage to identify bottlenecks on both the server running the REST API and the server running this benchmarking tool.

Run the JAR file. If the JAR file does not execute on your system, execute the application with the command `java -jar build/libs/restapi-benchmark-0.1.0.jar`. You may need to remove the `executable = true` line from `build.gradle` and recompile the application first.

## Developer Reference

This project can be easily edited in [Eclipse for Java](http://www.eclipse.org/downloads/eclipse-packages/):
1. Ensure both Gradle and Eclipse are installed
2. Download this repository to your computer
3. In Eclipse, open **File** then **Import...**
4. Under **Gradle**, select **Existing Gradle Project** and click **Next** 
5. On the **Import Gradle Project** click **Browse** and open the root directory of the project
6. Click **Finish**

### Notification Object

This application is currently configured to test the [Back in Stock Database REST API](https://github.com/chrislzm/BackInStock/tree/master/RestApi). The Notification object used in each request is located in the [Objects](https://github.com/chrislzm/BackInStock/tree/master/Objects) directory of the [Back in Stock](https://github.com/chrislzm/BackInStock) project.

### To Do
* Add/Update code documentation
* Refactor (e.g. create interfaces/abstract classes) so that it's easier to add and test other REST APIs
* Add features
    * Output test results in CSV format
    * Progress indicator during testing

## Built with

* [Spring Boot](https://projects.spring.io/spring-boot/)
* [Simple Logging Facade for Java](https://www.slf4j.org/)
* [Gradle](https://gradle.org/)

## See Also

Other open-source benchmarking tools:

* Simple, but unable to generate randomized data
    * [ab](https://httpd.apache.org/docs/2.4/programs/ab.html)
    * [wrk](https://github.com/wg/wrk) 
* Comprehensive
    * [JMeter](http://jmeter.apache.org/) - Randomized data must be provided to the application in a .csv file 
 
## License

Copyright (c) 2018 [Chris Leung](https://github.com/chrislzm)

Licensed under the MIT License. You may obtain a copy of the License in the [`LICENSE`](LICENSE) file included with this project.
