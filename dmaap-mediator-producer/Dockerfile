#==================================================================================
#   Copyright (C) 2021: Nordix Foundation
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#
#   This source code is part of the near-RT RIC (RAN Intelligent Controller)
#   platform project (RICP).
#==================================================================================

##
## Build
##
FROM golang:1.17-bullseye AS build
WORKDIR /app
COPY go.mod ./
COPY go.sum ./
RUN go mod download
COPY . ./
RUN go build -o /dmaapmediatorproducer
##
## Deploy
##
FROM gcr.io/distroless/base-debian10
WORKDIR /
## Copy from "build" stage
COPY --from=build /dmaapmediatorproducer .
COPY --from=build /app/configs/* /configs/
USER nonroot:nonroot
ENTRYPOINT ["/dmaapmediatorproducer"]
