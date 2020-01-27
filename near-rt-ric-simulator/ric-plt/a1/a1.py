#!/u:sr/bin/env python3
import copy
import datetime
import json
import logging
import requests

from connexion import NoContent
from flask import Flask, escape, request
from jsonschema import validate
from random import random, choice
from var_declaration import policy_instances, policy_types, policy_status, notification_destination, notificationDestination

def get_all_policies():
  all_p = copy.deepcopy(policy_instances)
  all_policies = []
  for i in all_p.keys():
    all_p[i]["enforceStatus"] = policy_status[i]["enforceStatus"]
    all_policies.insert(len(all_policies)-1, all_p[i])
  return(all_policies, 200)

def put_policy(policyId):
  data = request.data.decode("utf-8")
  data = data.replace("'", "\"")
  data = json.loads(data)
  ps = {}

  if data["policyTypeId"] not in list(policy_types.keys()):
    return(set_error(None, "The policy type provided does not exist.", 404, "The policy type " + data["policyTypeId"] + " is not defined as a policy type.", None, "policyTypeId", None))

  pt = data["policyTypeId"]
  schema = policy_types[pt]
  try:
    validate(instance=data["policyClause"], schema=schema)
  except:
    return(set_error(None, "The json does not validate against the schema.", 400, None, None, None, None))

  if data["policyId"] in list(policy_instances.keys()):
    if data["policyClause"]["scope"] != policy_instances[data["policyId"]]["policyClause"]["scope"]:
      return(set_error(None, "The policy already exists with a different scope.", 404, "The policy put involves a modification of the existing scope, which is not allowed.", None, "scope", None))

  if data["policyId"] != policyId:
    return(set_error(None, "Wrong policy identity.", 400, "The policy instance's identity does not match with the one specified in the address.", None, "policyId", "The policy identity " + data["policyId"] + " is different from the address: " + policyId))

  for i in list(policy_instances.keys()):
    if data["policyId"] != i and \
       data["policyClause"] == policy_instances[i]["policyClause"] and \
       data["policyTypeId"] == policy_instances[i]["policyTypeId"] and \
       data["notificationDestination"] == policy_instances[i]["notificationDestination"]:
      return(set_error(None, "The policy already exists with a different id.", 404, "No action has been taken. The id of the existing policy instance is: " + i + ".", None, None, None))

  if policyId in policy_instances.keys():
    code = 201
  else:
    code = 200
  policy_instances[policyId] = data
  policy_status[policyId] = set_status("UNDEFINED")
  notification_destination[policyId] = data["notificationDestination"]
  return(policy_instances[policyId], code)

def set_status(*args):
  ps = {}
  if len(args) == 0:
    rand_status = randomise_status()
    ps["policyId"] = policyId
    ps["enforceStatus"] = rand_status
    if rand_status == "NOT_ENFORCED":
      rand_reason = randomise_reason()
      ps["enforceReason"] = rand_reason
  if args[0] in ["UNDEFINED", "ENFORCED", "NOT_ENFORCED"]:
    ps["enforceStatus"] = args[0]
  else:
    return(set_error(None, "Wrong enforceStatus.", 400, None, None, "enforceStatus", "enforceStatus should be one of \"UNDEFINED\", \"ENFORCED\" or \"NOT_ENFORCED\""))
  if args[0] == "NOT_ENFORCED":
    if args[1] in ["100", "200", "300", "800"]:
      ps["enforceReason"] = args[1]
    else:
      return(set_error(None, "Wrong enforceReason.", 400, None, None, "enforceReason", "enforceReason should be one of \"100\", \"200\", \"300\" or \"800\""))
  return ps

def get_policy(policyId):
  if policyId in policy_instances.keys():
    res = policy_instances[policyId]
    res["enforceStatus"] = policy_status[policyId]["enforceStatus"]
    return(res, 200)
  else:
    return(set_error(None, "The requested policy does not exist.", 404, None, None, "policyId", None))

def delete_policy(policyId):
  if policyId in policy_instances.keys():
    policy_instances.pop(policyId)
    policy_status.pop(policyId)
    return(None, 204)
  else:
    return(set_error(None, "The policy identity does not exist.", 404, "No policy instance has been deleted.", None, "policyId", None))

def randomise_status():
  x = random()
  if x > 0.5001:
    res = "ENFORCED"
  elif x < 0.4999:
    res = "NOT_ENFORCED"
  else:
    res = "UNDEFINED"
  return res

def randomise_reason():
  options = ["100", "200", "300", "800"]
  return choice(options)

def get_policy_status(policyId):
  return(policy_status[policyId], 200)

def get_all_policytypes():
  all_policytypes = []
  for i in policy_types.keys():
    all_policytypes.insert(len(all_policytypes)-1, policy_types[i])
  return(all_policytypes, 200)

def get_policytypes(policyTypeId):
  if policyTypeId in policy_types.keys():
    return(policy_types[policyTypeId], 200)
  else:
    return(set_error(None, "The requested policy type does not exist.", 404, None, None, "policyTypeId", None))

def set_error(type_of, title, status, detail, instance, param, reason):
  error = {}
  params = {}
  if type_of is not None:
    error["type"] = type_of
  if title is not None:
    error["title"] = title
  if status is not None:
    error["status"] = status
  if detail is not None:
    error["detail"] = detail
  if instance is not None:
    error["instance"] = instance
  if param is not None:
    params["param"] = param
  if reason is not None:
    params["reason"] = reason
  if params:
    error["invalidParams"] = params
  return(error, error["status"])
