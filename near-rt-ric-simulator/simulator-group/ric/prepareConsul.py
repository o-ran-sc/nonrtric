import json
import subprocess
import os

print("Update fresh ric configuration in Consul configuration file")

p = os.path.abspath('..')
consul_config = p + '/consul_cbs' + '/config.json'


def write_json(data, filename=consul_config):
    with open(filename, 'w') as f:
        json.dump(data, f, indent=4)


def bash_command(cmd):
    result = []
    sp = subprocess.Popen(['/bin/bash', '-c', cmd], stdout=subprocess.PIPE)
    for line in sp.stdout.readlines():
        result.append(line.decode().strip())
    return result


command = "docker ps | grep simulator | awk '{print $NF}'"

ric_list = bash_command(command)

with open(consul_config) as json_file:
    data = json.load(json_file)
    temp = data['ric']
    for ric in ric_list:
        y = {"name": ric,
             "baseUrl": "http://" + ric + ":8085/",
             "managedElementIds": [
                 "kista_" + ric,
                 "stockholm_" + ric
             ]
             }
        temp.append(y)


write_json(data)
print("Update Consul config file with fresh ric configuration,  done")
