FROM gcr.io/distroless/java11-debian11:nonroot

ARG VEO_VERSION

LABEL org.opencontainers.image.title="vernice.veo backend"
LABEL org.opencontainers.image.description="Backend of the verinice.veo web application."
LABEL org.opencontainers.image.ref.name=verinice.veo
LABEL org.opencontainers.image.vendor="SerNet GmbH"
LABEL org.opencontainers.image.authors=verinice@sernet.de
LABEL org.opencontainers.image.licenses=AGPL-3.0
LABEL org.opencontainers.image.source=https://github.com/verinice/verinice-veo
LABEL org.opencontainers.image.version=${VEO_VERSION}

USER nonroot

# If by accident we have more than one veo-rest-*.jar docker will complain, which is what we want.
COPY --chown=nonroot:nonroot veo-rest/build/libs/veo-rest-${VEO_VERSION}.jar /app/veo-rest.jar

WORKDIR /app
EXPOSE 8070
CMD ["veo-rest.jar"]
