# Prepare stage for multistage image build
## START OF STAGE0 ##
FROM nexus3.onap.org:10001/onap/ccsdk-odlsli-alpine-image:${ccsdk.docker.version} AS stage0

ENV JAVA_HOME /usr/lib/jvm/java-1.8-openjdk
ENV ODL_HOME /opt/opendaylight

USER root

# copy onap
COPY opt /opt
RUN test -L /opt/sdnc || ln -s /opt/onap/sdnc /opt/sdnc
RUN mkdir $ODL_HOME/current/certs

# copy SDNC mvn artifacts to ODL repository
COPY system /tmp/system
RUN rsync -a /tmp/system $ODL_HOME
## END OF STAGE0 ##


FROM nexus3.onap.org:10001/onap/ccsdk-odlsli-alpine-image:${ccsdk.docker.version}

MAINTAINER O-RAN-SC NONRTRIC Team

ENV JAVA_HOME /usr/lib/jvm/java-1.8-openjdk
ENV ODL_HOME /opt/opendaylight
ENV SDNC_CONFIG_DIR /opt/onap/sdnc/data/properties
ENV JAVA_SECURITY_DIR /etc/ssl/certs/java
ENV SDNC_NORTHBOUND_REPO mvn:org.o-ran-sc.nonrtric.sdnc-a1.northbound/sdnc-a1-northbound-all/${sdnc.northbound.version}/xml/features
ENV SDNC_KEYSTORE keystore.jks
ENV SDNC_KEYPASS sdnc-a1-controller
ENV SDNC_SECUREPORT 8443

USER root

COPY --from=stage0 --chown=odl:odl /opt /opt

# Add SDNC repositories to boot repositories
RUN cp $ODL_HOME/etc/org.apache.karaf.features.cfg $ODL_HOME/etc/org.apache.karaf.features.cfg.orig
RUN sed -i -e "\|featuresRepositories|s|$|,${SDNC_NORTHBOUND_REPO}|"  $ODL_HOME/etc/org.apache.karaf.features.cfg
RUN sed -i -e "\|featuresBoot[^a-zA-Z]|s|$|,sdnc-a1-northbound-all|"  $ODL_HOME/etc/org.apache.karaf.features.cfg
RUN sed -i "s/odl-restconf-all/odl-restconf-all,odl-netconf-topology/g"  $ODL_HOME/etc/org.apache.karaf.features.cfg

# Install java certificate
COPY $SDNC_KEYSTORE $JAVA_SECURITY_DIR

# Secure with TLS
RUN echo org.osgi.service.http.secure.enabled=true >> $ODL_HOME/etc/custom.properties
RUN echo org.osgi.service.http.secure.port=$SDNC_SECUREPORT >> $ODL_HOME/etc/custom.properties
RUN echo org.ops4j.pax.web.ssl.keystore=$JAVA_SECURITY_DIR/$SDNC_KEYSTORE >> $ODL_HOME/etc/custom.properties
RUN echo org.ops4j.pax.web.ssl.password=$SDNC_KEYPASS >> $ODL_HOME/etc/custom.properties
RUN echo org.ops4j.pax.web.ssl.keypassword=$SDNC_KEYPASS >> $ODL_HOME/etc/custom.properties

RUN chown -R odl:odl /opt

USER odl

ENTRYPOINT /opt/onap/sdnc/bin/startODL.sh
EXPOSE 8181 $SDNC_SECUREPORT
