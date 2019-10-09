# Base ubuntu with added packages needed for open ecomp
FROM onap/ccsdk-ubuntu-image:${ccsdk.docker.version}
MAINTAINER SDN-C Team (sdnc@lists.openecomp.org)

# create link for pm2
#RUN ln -s /opt/openecomp/sdnc/admportal/node_modules/pm2/bin/pm2 /usr/bin/pm2

# copy openecomp
COPY opt /opt

RUN cd /opt/onap/sdnc/admportal && npm install

#ENTRYPOINT exec /opt/openecomp/sdnc/admportal/shell/startssl.sh
EXPOSE 8843 8443
