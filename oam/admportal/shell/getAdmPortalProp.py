#!/usr/bin/python

import json
import sys
import os

propfile = os.getenv('SDNC_CONFIG_DIR') + '/admportal.json'

from pprint import pprint

with open(propfile) as data_file:    
    data = json.load(data_file)
pprint( str(data[sys.argv[1]]))
