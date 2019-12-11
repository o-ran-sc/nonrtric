import connexion
import fileinput
import json
import sys

from flask import Flask, escape, request, make_response
from var_declaration import policy_instances, policy_types, policy_status

app = connexion.App(__name__, specification_dir='.')

@app.route('/policytypes/<string:policyTypeId>', methods=['PUT','DELETE'])
def policy_type(policyTypeId):
  if request.method == 'PUT':
    data = request.data.decode("utf-8")
    data = data.replace("'", "\"")
    data = json.loads(data)
    policy_types[policyTypeId] = data
    return ('The policy type was either created or updated for policy type id: ' + policyTypeId)
  elif request.method == 'DELETE':
    if policyTypeId in policy_types.keys():
      policy_types.pop(policyTypeId)
      return make_response("policy type successfully deleted for policy type id: " + policyTypeId, 200)
    else:
      return make_response("No policy type defined for the specified id", 404)

@app.route('/', methods=['GET'])
def test():
  return("I'm alive", 200)

@app.route('/deleteinstances', methods=['GET'])
def delete_instances():
  global policy_instances
  global policy_status
  policy_instances.clear()
  policy_status.clear()
  return("All policy instances deleted", 200)

@app.route('/deletetypes', methods=['GET'])
def delete_types():
  global policy_types
  policy_types.clear()
  return("All policy types deleted", 200)

port_number = 8085
if len(sys.argv) >= 2:
  if isinstance(sys.argv[1], int):
    port_number = sys.argv[1]

app.add_api('a1-openapi.yaml')
app.run(port=port_number)

