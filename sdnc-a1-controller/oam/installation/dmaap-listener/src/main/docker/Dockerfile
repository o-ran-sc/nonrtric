# Base ubuntu with added packages needed for open ecomp
FROM alpine:3.8

MAINTAINER SDNC Team (onap-sdnc@lists.onap.org)

ENV JAVA_HOME /usr/lib/jvm/java-1.8-openjdk
ENV SDNC_CONFIG_DIR /opt/onap/sdnc/data/properties
    
RUN apk update && apk add \
    bash \
    git \
    openjdk8 \
    maven \
    mysql-client \
    nodejs \
    py-pip \
    graphviz \
    unzip \
    rsync

# copy deliverables to opt
COPY opt /opt


