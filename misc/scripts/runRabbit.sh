# This script runs a local RabbitMQ instance in a container.
# You usually do not have to use this:
#
# The standard tests start their own RabbitMQ container using the testcontainers library.
#
# If you want to use a remote RabbitMQ server you can simply configure the correct host
# and port in the application properties.
# (If you are inside a firewalled network you may have to use SSH or other means to forward
# the external RabbitMQ port into your local network first.)
#
# If you have no externally reachable RabbitMQ server, you can use this script to start one locally.
#
# Access management console on http://localhost:15672
# user: guest / pass: guest
#
# If the container was successfully started once, you can start/stop it with these commands:
# Stop with: 	docker stop veo-rabbit
# Restart with:	docker start veo-rabbit
#

docker run -d -p 5672:5672 -p 15672:15672 --name veo-rabbit rabbitmq:3-management
