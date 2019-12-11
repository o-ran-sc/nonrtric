#!/bin/bash

if [ $# -eq 0 ]
then
  python3 ./main.py
else
  python3 ./main.py $1
fi
