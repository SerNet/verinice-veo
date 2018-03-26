# v2020

A prototype of a new verinice version.

## Build

**Prerequisite:**
* Install Maven 3.x (if you want to use the Maven build).
* Install Java 8.

**Clone project:**

```bash
git clone ssh://git@git.verinice.org:7999/rd/v2020.git
cd v2020
```

**Build project:**

*** using Maven:***
```bash
export JAVA_HOME=/path/to/jdk-8
mvn install [-DskipTests]
```
*** using Gradle:***
```bash
export JAVA_HOME=/path/to/jdk-8
./gradlew build [-x test]
```

## Run

**Prerequisite:**
* Install Java 8.
* Install MySQL, MariaDB or PostgreSQL
* Create an empty database _v2020_

**Run Model Schema Web Service**

*** using Maven:***

```bash
cd v2020-model-schema
mvn spring-boot:run
```

or

```bash
java -jar v2020-model-schema/target/v2020-model-schema-0.1.0-SNAPSHOT-exec.jar
```

*** using Gradle:***
```bash
./gradlew v2020-model-schema:bootRun
```

or

```bash
java -jar v2020-model-schema/build/libs/v2020-model-schema-0.1.0-SNAPSHOT-exec.jar
```

**Run Web Application**

To run the web application you have to start model schema web service first.

Set your database properties in file _v2020-jsf/src/main/resources/application.properties_ and rebuild the application.

*** using Maven:***

```bash
cd v2020-jsf
mvn spring-boot:run
```

or

```bash
java -jar v2020-jsf/target/v2020-jsf-0.1.0-SNAPSHOT.jar
```

*** using Gradle:***
```bash
./gradlew v2020-jsf:bootRun
```

or

```bash
java -jar v2020-jsf/build/libs/v2020-jsf-0.1.0-SNAPSHOT.jar
```

**Run VNA Import**

Set your database properties in file _v2020-vna-import/src/main/resources/application.properties_ and rebuild the application.

```bash
java -jar v2020-vna-import/target/v2020-vna-import-0.1.0-SNAPSHOT.jar \
-f /path/to/verinice-archive-file.vna
```
If you built with Gradle, use `v2020-vna-import/build/libs/v2020-vna-import-0.1.0-SNAPSHOT.jar` as the JAR file.

**Run REST Service**

Set your database properties in file _v2020-rest/src/main/resources/application.properties_ and rebuild the application.

*** using Maven:***

```bash
cd v2020-rest
mvn spring-boot:run
```

or

```bash
java -jar v2020-rest/target/v2020-rest-0.1.0-SNAPSHOT.jar
```

*** using Gradle:***
```bash
./gradlew v2020-rest:bootRun
```

or

```bash
java -jar v2020-rest/build/libs/v2020-rest-0.1.0-SNAPSHOT.jar
```

## Modules

**v2020-model**

This module contains the domain model and interfaces of the application. This module be used in any other module of the application.  

**v2020-data-xml**

This module contains the JAXB class files for accessing SNCA.xml from verinice.

**v2020-persistence**

This module contains the persistence layer of the application.

**v2020-service**

This module contains the service implementation of the application.

**v2020-jsf**

This module contains the web application based on JSF and PrimeFaces.

**v2020-rest**

This module contains the implementation of the REST services of the application.

**v2020-model-schema**

This module contains a service to load the schema for the elements and an REST service to access the schema.

**v2020-model-schema-client**

This module contains a client to call the model schema REST service.

**v2020-vna-import**

This module contains an importer for verinice archives (VNAs).

## Database

Entity–relationship model of the database:

![ERM of the the database](v2020-persistence/src/main/sql/database-erm.png)

## Authentication and Authorization
v2020-rest uses JWT to authorize users. Tokens can be obtained by a `POST` on `/login`, e.g.

	curl -i -H "Content-Type: application/json" -X POST -d '{
			"username": "user",
			"password": "password"
	}' http://localhost:8070/login

On success the response will contain a header

	Authorization: Bearer <token>

This header has to be part of every further request

