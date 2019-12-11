import connexion
import fileinput
import json

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

app.add_api('a1-openapi.yaml')
app.run(port=8085)

