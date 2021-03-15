FROM openjdk:11-jre-slim

ARG VEO_VERSION

LABEL org.opencontainers.image.title="vernice.veo backend"
LABEL org.opencontainers.image.description="Backend of the verinice.veo web application."
LABEL org.opencontainers.image.ref.name=verinice.veo
LABEL org.opencontainers.image.vendor="SerNet GmbH"
LABEL org.opencontainers.image.authors=verinice@sernet.de
LABEL org.opencontainers.image.licenses=LGPL-2.0
LABEL org.opencontainers.image.source=https://github.com/verinice/verinice-veo
LABEL org.opencontainers.image.version=${VEO_VERSION}

RUN adduser --home /app --disabled-password --gecos '' veo
USER veo
WORKDIR /app

# If by accident we have more than one veo-rest-*.jar docker will complain, which is what we want.
COPY veo-rest/build/libs/veo-rest-${VEO_VERSION}.jar veo-rest.jar

EXPOSE 8070
CMD ["java", "-jar", "veo-rest.jar"]
