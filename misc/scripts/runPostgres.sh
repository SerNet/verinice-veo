# This script runs a local postgres instance in a container.
# You usually do not have to use this:
#
# The standard tests start their own postgres container using the testcontainers library.

docker run\
  --name veo-postgres\
  -e POSTGRES_USER=test\
  -e POSTGRES_PASSWORD=test\
  -e POSTGRES_DB=veo\
  -d\
  -p 5434:5432\
  postgres:13.14-alpine
  