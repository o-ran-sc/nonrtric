import connexion
import fileinput
import json
import sys

from flask import Flask, escape, request, make_response
from jsonschema import validate
from var_declaration import policy_instances, policy_types, policy_status, policy_type_per_instance

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
    return("Everything is fine", 200)

@app.route('/deleteinstances', methods=['DELETE'])
def delete_instances():
  global policy_instances
  global policy_status
  global policy_type_per_instance
  policy_instances.clear()
  policy_status.clear()
  policy_type_per_instance.clear()
  return("All policy instances deleted", 200)

@app.route('/deletetypes', methods=['DELETE'])
def delete_types():
  global policy_types
  policy_types.clear()
  return("All policy types deleted", 200)

@app.route('/<string:policyId>/<string:enforceStatus>', methods=['PUT'])
def set_status(policyId, enforceStatus):
  if policyId in policy_instances.keys():
    if policy_type_per_instance[policyId] == "UNDEFINED":
      ps = {}
      ps["policyId"] = policyId
      ps["enforceStatus"] = enforceStatus
    else:
      policy_type_id = policy_type_per_instance[policyId]
      status_schema = policy_types[policy_type_id]["statusSchema"]
      ps = {}
      ps["policyId"] = policyId
      ps["enforceStatus"] = enforceStatus
      try:
        validate(instance=ps, schema=status_schema)
      except:
        return(set_error(None, "The json does not validate against the status schema.", 400, None, None, None, None, None))
  policy_status.pop(policyId)
  policy_status[policyId] = ps
  return("Status updated for policy: " + policyId, 200)

@app.route('/<string:policyId>/<string:enforceStatus>/<string:enforceReason>', methods=['PUT'])
def set_status_with_reason(policyId, enforceStatus, enforceReason):
  if policyId in policy_instances.keys():
    if policy_type_per_instance[policyId] == "UNDEFINED":
      ps = {}
      ps["policyId"] = policyId
      ps["enforceStatus"] = enforceStatus
      ps["enforceReason"] = enforceReason
    else:
      policy_type_id = policy_type_per_instance[policyId]
      status_schema = policy_types[policy_type_id]["statusSchema"]
      ps = {}
      ps["policyId"] = policyId
      ps["enforceStatus"] = enforceStatus
      ps["enforceReason"] = enforceReason
      try:
        validate(instance=ps, schema=status_schema)
      except:
        return(set_error(None, "The json does not validate against the status schema.", 400, None, None, None, None, None))
  policy_status.pop(policyId)
  policy_status[policyId] = ps
  return("Status updated for policy: " + policyId, 200)

#Metrics function

@app.route('/counter/<string:countername>', methods=['GET'])
def getCounter(countername):
    if (countername == "num_instances"):
        return str(len(policy_instances)),200
    elif (countername == "num_types"):
        return str(len(policy_types)),200
    else:
        return "Counter name: "+countername+" not found.",404


port_number = 8085
if len(sys.argv) >= 2:
  if isinstance(sys.argv[1], int):
    port_number = sys.argv[1]

app.add_api('a1-openapi.yaml')
app.run(port=port_number)

