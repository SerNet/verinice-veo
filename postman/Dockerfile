FROM postman/newman:ubuntu

RUN apt-get update && apt-get install -y openjdk-11-jre-headless wget jq gawk

WORKDIR /veo
COPY misc/scripts/authenticate .
RUN chown -Rv 1002:1003 /veo