#!/bin/bash

#Builds the mrstub container and starts it in interactive mode

docker build -t mrstub .

docker run -it -p 6845:6845 mrstub
