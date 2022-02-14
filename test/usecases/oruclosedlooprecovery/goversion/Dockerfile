##
## Build
##
FROM golang:1.17.1-bullseye AS build

WORKDIR /app

COPY go.mod ./
COPY go.sum ./
RUN go mod download

COPY . ./

RUN go build -o /docker-oruclosedloop

##
## Deploy
##
FROM gcr.io/distroless/base-debian10

WORKDIR /

## Copy from "build" stage
COPY --from=build /docker-oruclosedloop .
COPY --from=build /app/security/ ./security/
COPY --from=build /app/o-ru-to-o-du-map.csv .

USER nonroot:nonroot

ENTRYPOINT ["/docker-oruclosedloop"]
