# v2020

A protoype of a new verinice version.

## Build

*Prerequisite:*
* Install Maven 3.x.
* Install Java 8.

*Clone project:*
```bash
git clone ssh://git@git.verinice.org:7999/rd/v2020.git
cd v2020
```

*Build project:*
```bash
export JAVA_HOME=/path/to/jdk-8
mvn package [-DskipTests]
```

## Modules

**v2020-model**

This module contains the domain model and interfaces of the application. This module be used in any other module of the application.  

**v2020-persistence**

This module contains the persistence layer of the application.

**v2020-service**

This module contains the service implementation of the application.

**v2020-rest**

This module contains the implementation of the REST services of the application.

**v2020-rest-client**

This module contains the Java clients to access of the REST services of the application.
