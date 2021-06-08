ARG NEXUS_PROXY_REPO

FROM ${NEXUS_PROXY_REPO}node:16-alpine3.12

ENV NODE_ENV=production

WORKDIR /usr/src/app/cert
COPY cert/*.crt ./   #Need trailing slash on dest for multiple file copy
COPY cert/pass .

WORKDIR /usr/src/app
COPY http_proxy.js .

CMD [ "node", "http_proxy.js" ]