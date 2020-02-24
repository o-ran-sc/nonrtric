import json
import subprocess
import os

print("Clean old ric configurations in Consul config file")

p = os.path.abspath('..')
consul_config = p + '/consul_cbs' + '/config.json'


def write_json(data, filename=consul_config):
    with open(filename, 'w') as f:
        json.dump(data, f, indent=4)


with open(consul_config) as json_file:
    clean = json.load(json_file)
    clean['ric'] = []


write_json(clean)
print("Clean old ric configurations from Consul config file, done")


