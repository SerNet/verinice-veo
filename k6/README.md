# k6 Load Tests

This folder contains test scripts for the load test tool [k6](https://k6.io/).

Load Test is primarily concerned with assessing the performance of your system in terms of concurrent users or requests per second. There are several types of load tests, each type serving a different purpose:

[Smoke Test's](https://k6.io/docs/test-types/smoke-testing/) role is to verify that your System can handle minimal load, without any problems.

[Load Test](https://k6.io/docs/test-types/load-testing/) is primarily concerned with assessing the performance of your system in terms of concurrent users or requests per second.

[Stress Test](https://k6.io/docs/test-types/stress-testing/) and [Spike testing](https://k6.io/docs/test-types/stress-testing/#spike-testing-in-k6) are concerned with assessing the limits of your system and stability under extreme conditions.

[Soak Test](https://k6.io/docs/test-types/soak-testing/) tells you about reliability and performance of your system over the extended period of time.

Load tests do not test the functionality of an application. They are not executed automatically during the build but started manually.

k6 is a developer-oriented, free and open source tool for all types of load testing. A k6 test consists of one or more JavaScript files.

## Run Tests

Prerequisite for running tests is the installation of k6, which is described in the [documentation](https://k6.io/docs/getting-started/installation/).

k6 tests are executed with a CLI tool. The simplest call of such a test is:

	k6 run script.js

See k6 documentation for more options: [Running k6](https://k6.io/docs/getting-started/running-k6).

To run the script behind a proxy set environment variables before executing:

	export HTTPS_PROXY=http://[HOST]:[PORT]
	export HTTP_PROXY=http://[HOST]:[PORT]

### Test _Create Processing Activity_

In the following the call of the load test _Create Processing Activity_ is described, which is implemented in the script `create_processing_activity.js`. The test creates a processing activity and all the objects needed for it, which are referenced in the processing activity. This test requires its own special parameters when called:

`-e name=<USER_NAME>` required - User name of an account for verinice.veo.

`-e password=<PASSWORD>` required - Password of the account.

`-e host=<HOSTNAME>` optional, _develop.verinice.com_ | _staging.verinice.com_ (default)

Run the test with the runtime and number of virtual users defined in the script:

	k6 run create_processing_activity.js \
	-e host=staging.verinice.com -e name=foo -e password=bar

Run the test with 10 virtual users (vus) for 30s:

	k6 run --vus 10 --duration 30s create_processing_activity.js \
	-e host=staging.verinice.com -e name=foo -e password=bar
