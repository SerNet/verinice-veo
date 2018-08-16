# v2020

A prototype of a new verinice version.

## Build

**Prerequisite:**
* Install Java 8.

**Clone project:**

```bash
git clone ssh://git@git.verinice.org:7999/rd/v2020.git
cd v2020
```

**Build project:**

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

```bash
./gradlew v2020-model-schema:bootRun
```

or

```bash
./gradlew v2020-model-schema:bootRepackage
java -jar v2020-model-schema/build/libs/v2020-model-schema-0.1.0-SNAPSHOT-exec.jar
```

**Run VNA Import**

Set your database properties in file _v2020-vna-import/src/main/resources/application.properties_ and rebuild the application.

```bash
./gradlew v2020-vna-import:bootRepackage
java -jar v2020-vna-import/build/libs/v2020-vna-import-0.1.0-SNAPSHOT.jar \
-f /path/to/verinice-archive-file.vna
```

**Run REST Service**

Set your database properties in file _v2020-rest/src/main/resources/application.properties_ and rebuild the application.


```bash
./gradlew v2020-rest:bootRun
```

or

```bash
./gradlew v2020-rest:jar
java -jar v2020-rest/build/libs/v2020-rest-0.1.0-SNAPSHOT.jar
```

## Modules

### v2020-model
This module contains the domain model and interfaces of the application. This module be used in any other module of the application.  

### v2020-data-xml
This module contains the JAXB class files for accessing SNCA.xml from verinice.

### Rest Layers
The 3 REST components are split into 3 layers

#### v2020-persistence
This module contains the persistence layer of the REST API.

#### v2020-service
This module contains the service implementation of the REST API.

This is no generic module for all kind of "services" or interface implementations.

#### v2020-rest
This module contains the implementation of the REST services of the REST API.

The JSON schemas accepted by the API can be found in *${veo.basedir}/schemas/*. If this directory
does not exist, built-in schema files will be served as default.

*veo.basedir* can be set in *application.properties* and is */var/lib/veo* by
default. The gradle task `bootRun` sets *veo.basedir* to
*$HOME/.local/share/veo*.

### v2020-vna-import
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

