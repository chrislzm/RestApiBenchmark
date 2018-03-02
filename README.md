# REST API Benchmarking Tool

This Java application implements an simple benchmarking tool that measures performance of a CRUD REST API.

This documentation is not complete. This application is under active development.

This benchmarking tool is by default configured to test the [Back in Stock Database REST API](https://github.com/chrislzm/BackInStock/tree/master/RestApi). For more information, visit the [Back in Stock](https://github.com/chrislzm/BackInStock) project. 

## Prerequisites

1. Java
2. Gradle
3. Fast internet connection

## Installing

1. Code each request for your API: Use the `PostNotificationJob`,  `GetNotificationJob`, `UpdateNotificationJob`, and `DeleteNotificationJob` as examples for POST, GET, PUT, and DELETE http methods respectively
2. Rename `application.properties.blank` to `application.properties` and update values
3. Compile the application
    * Run the command `./gradlew bootRun` (will compile and run) or
    * Build an executable JAR file using `./gradlew build`, which will create a JAR file in `./build/libs/notification-service-0.1.0.jar`

## Deployment

Run the JAR file. If the JAR file does not execute on your system, execute the application with the command `java -jar build/libs/restapi-benchmark-0.1.0.jar`. You may need to remove the `executable = true` line from `build.gradle` and recompile the application first.

### Command Line Arguments

Please note that any or all of the settings in `application.properties` can also be overridden with command line arguments. Example: `--restapi.benchmark.request.total=1000`

## Development Notes

To Do:
* Add/Update code documentation
* Refactor (e.g. create interfaces/abstract classes) so that it's easier to add and test other REST APIs
* Add ability to disable randomly generated data

## License

Copyright (c) 2018 [Chris Leung](https://github.com/chrislzm)

Licensed under the MIT License. You may obtain a copy of the License in the [`LICENSE`](LICENSE) file included with this project.
