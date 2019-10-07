### Games Score Service

<hr/>

#### Execute Unit Tests
* Execute: `mvn clean test`


#### Execute Integration Tests
* Execute: `mvn clean integration-test -DskipUTs=true` or `mvn clean verify -DskipUTs=true`


#### Run All Tests
* Execute: `mvn clean verify`


#### Test Coverage (via JaCoCo)
* In order to generate reports execute: `mvn clean verify`
    * In order to see unit test coverage open with browser: `target/site/jacoco-ut/index.html`
    * In order to see integration test coverage open with browser: `target/site/jacoco-it/index.html`


#### Package/Build Application and Run it
* Execute: `mvn clean package -DskipUTs=true -DskipITs=true`

* Then, execute: `java -jar target/games-score-service-1.0-SNAPSHOT-jar-with-dependencies.jar`


#### Highscores List Calculation Strategy
* Select from `application.properties` file one of the following:
    * `level-scores-by-user-id-nested-strategy`
    * `level-scores-flat-strategy`


#### Dependencies Used
* Dev
    * Log4j
    * Lombok
    
* Test
    * JUnit
    * Mockito
    * Awaitility