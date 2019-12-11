#!/bin/bash

# One argument can be used along with the script call: it is the port on which one wish to run the simulator.

if [ $# -eq 0 ]
then
  python3 ./main.py
else
  python3 ./main.py $1
fi
