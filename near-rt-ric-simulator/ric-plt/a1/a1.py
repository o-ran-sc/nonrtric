#!/u:sr/bin/env python3
import connexion
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

error = {}
params = {}

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
    reset_error()
    error["title"] = "The policy type provided does not exist."
    error["status"] = 404
    error["detail"] = "The policy type " + data["policyTypeId"] + " is not defined as a policy type."
    params["param"] = "policyTypeId"
    error["invalidParams"] = params
    return(error, 404)
  if data["policyId"] != policyId:
    reset_error()
    error["title"] = "Wrong policy identity."
    error["status"] = 400
    error["detail"] = "The policy instance's identity does not match with the one specified in the address."
    params["param"] = "policyId"
    params["reason"] = "The policy identity " + data["policyId"] + " is different from the address: " + policyId
    error["invalidParams"] = params
    return(error, 400)
  pt = data["policyTypeId"]
  schema = policy_types[pt]
  validate(instance=data, schema=schema)
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
    reset_error()
    error["title"] = "Wrong enforceStatus."
    error["status"] = 400
    params["param"] = "enforceStatus"
    params["reason"] = "enforceStatus should be one of \"UNDEFINED\", \"ENFORCED\" or \"NOT_ENFORCED\""
    error["invalidParams"] = params
    return(error, 400)
  if args[0] == "NOT_ENFORCED":
    if args[1] in ["100", "200", "300", "800"]:
      ps["enforceReason"] = args[1]
    else:
      reset_error()
      error["title"] = "Wrong enforceReason."
      error["status"] = 400
      params["param"] = "enforceReason"
      params["reason"] = "enforceReason should be one of \"100\", \"200\", \"300\" or \"800\""
      error["invalidParams"] = params
      return(error, 400)
  return ps

def get_policy(policyId):
  if policyId in policy_instances.keys():
    res = policy_instances[policyId]
    res["enforceStatus"] = policy_status[policyId]["enforceStatus"]
    return(res, 200)
  else:
    reset_error()
    error["title"] = "The requested policy does not exist."
    error["status"] = 404
    params["param"] = "policyId"
    error["invalidParams"] = params
    return(error, 404)

def delete_policy(policyId):
  if policyId in policy_instances.keys():
    policy_instances.pop(policyId)
    policy_status.pop(policyId)
    return('The policy was deleted for policy id:' + policyId, 204)
  else:
    reset_error()
    error["title"] = "The policy identity does not exist."
    error["status"] = 404
    error["detail"] = "No policy instance has been deleted."
    params["param"] = "policyId"
    error["invalidParams"] = params
    return(error, 404)

def get_all_policy_identities():
  return(list(policy_instances.keys()), 200)

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

def get_all_policy_status():
  all_s = copy.deepcopy(policy_status)
  all_status = []
  for i in all_s.keys():
    all_s[i]["policyId"] = i
    all_status.insert(len(all_status)-1, all_s[i])
  return(all_status, 200)

def get_policy_status(policyId):
  return(policy_status[policyId], 200)

def get_all_policytypes():
  all_policytypes = []
  for i in policy_types.keys():
    all_policytypes.insert(len(all_policytypes)-1, policy_types[i])
  return(all_policytypes, 200)

def get_all_policytypes_identities():
  return(list(policy_types.keys()), 200)

def get_policytypes(policyTypeId):
  if policyTypeId in policy_types.keys():
    return(policy_types[policyTypeId], 200)
  else:
    reset_error()
    error["title"] = "The requested policy type does not exist."
    error["status"] = 404
    params["param"] = "policyTypeId"
    error["invalidParams"] = params
    return(error, 404)

def put_policytypes_subscription():
  global notificationDestination
  data = request.data.decode("utf-8")
  data = data.replace("'", "\"")
  data = json.loads(data)
  if notificationDestination == '':
    notificationDestination = data
    return('The subscription was created', 201)
  else:
    notificationDestination = data
    return('The subscription was updated', 200)

def get_policytypes_subscription():
  if notificationDestination is '':
    #res = "{'notificationDestination':''}"
    reset_error()
    error["title"] = "The notification destination has not been defined."
    error["status"] = 404
    params["param"] = "notificationDestination"
    error["invalidParams"] = params
    return(error, 404)
  else:
    return(notificationDestination, 200)

def reset_error():
  error.clear()
  params.clear()

