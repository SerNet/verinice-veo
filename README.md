# verinice veo

A prototype of a new verinice version.

## Build

**Prerequisite:**
* Install Java 11.
* If you are using an IDE (Eclipse, IntelliJ, VSCode,...) you need to run it with the lombok compile time preprocessor. Simply download lombok.jar (i.e. from Maven Central) and run the helper: "java -jar lombok.jar". This will add the required parameter to your eclipse.ini or other configuration.  If you want to do it by hand, add the following parameter at the end of your eclipse.ini:

```
-javaagent:/home/akoderman/eclipse-workspace/verince-workspace-develop2/eclipse/lombok.jar
```


**Clone project:**

```bash
git clone ssh://git@git.verinice.org:7999/rd/v2020.git
cd v2020
```

**Build project:**

```bash
export JAVA_HOME=/path/to/jdk-11
./gradlew build [-x test]
```

## Run

**Prerequisite:**
* Install Java 11.
* Install MySQL, MariaDB or PostgreSQL
* Create an empty database _v2020_

**Run VNA Import**

Set your database properties in file _veo-vna-import/src/main/resources/application.properties_ and rebuild the application.

```bash
./gradlew veo-vna-import:bootJar
java -jar veo-vna-import/build/libs/veo-vna-import-0.1.0-SNAPSHOT.jar \
-f /path/to/verinice-archive-file.vna
```

**Run REST Service**

Set your database properties in file _veo-rest/src/main/resources/application.properties_ and rebuild the application.


```bash
./gradlew veo-rest:bootRun
```

or

```bash
./gradlew veo-rest:jar
java -jar veo-rest/build/libs/veo-rest-0.1.0-SNAPSHOT.jar
```

**Deploy to Cloud Foundry**

Check and set your Cloud Foundry parameters in file `veo-rest/build.gradle` in section `cfConfig`. After that export user name and password as environment variables:

```bash
export CF_CCUSER=NAME
export CF_CCPASSWORD=PASSWORD
```

You can not push your the REST service to a Cloud Foundry instance:

```bash
./gradlew cf-push
```


## Modules

### veo-core
This module contains the core components of the application. This module can be used in any other module of the application.

### veo-data-xml
This module contains the JAXB class files for accessing SNCA.xml from verinice.

#### veo-rest
This module contains the implementation of the REST services of the REST API.

The JSON schemas accepted by the API can be found in *${veo.basedir}/schemas/*. If this directory
does not exist, built-in schema files will be served as default.

*veo.basedir* can be set in *application.properties* and is */var/lib/veo* by
default. The gradle task `bootRun` sets *veo.basedir* to
*$HOME/.local/share/veo*.

### veo-vna-import
This module contains an importer for verinice archives (VNAs).

### veo-json-validation
This module provides functionality to validate JSON used throughout the API.

Each JSON has to be valid against a JSON schema, possibly defined by clients,
in particular unknown to the developer. To define a common base for all such
veo JSON schemas a meta schema is defined, see [META-SCHEMA.md](META-SCHEMA.md)

## Database
Entity–relationship model of the database:

![ERM of the the database](veo-persistence/src/main/sql/database-erm.png)

## Authentication and Authorization
veo-rest uses JWT to authorize users. Tokens can be obtained by a `POST` on `/login`, e.g.

	curl -i -H "Content-Type: application/json" -X POST -d '{
			"username": "user",
			"password": "password"
	}' http://localhost:8070/login

On success the response will contain a header

	Authorization: Bearer <token>

This header has to be part of every further request

## Code Styles
Code styles are enforced using the gradle plugins
[pmd](https://docs.gradle.org/current/userguide/pmd_plugin.html) and
[spotless](https://github.com/diffplug/spotless).

To verify your code run

	./gradlew pmdMain
	./gradlew spotlessCheck

or just

	./gradlew check

Sample git hooks are provided in *misc/git*, e. g.
[pre-commit](misc/git/pre-commit), which checks the code style on each commit.
To install, copy the file to *.git/hooks/pre-commit*, etc. Make sure the
scripts are executable.
