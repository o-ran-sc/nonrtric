#!/u:sr/bin/env python3
import connexion
import copy
import datetime
import json
import logging
import requests

from connexion import NoContent
from flask import Flask, escape, request
from random import random, choice
from var_declaration import policy_instances, policy_types, policy_status, notification_destination, notificationDestination

def get_all_policies():
  all_p = copy.deepcopy(policy_instances)
  all_policies = []
  for i in all_p.keys():
    all_p[i]["enforceStatus"] = policy_status[i]
    all_policies.insert(len(all_policies)-1, all_p[i])
  return(all_policies, 200)

def put_policy(policyId):
  data = request.data.decode("utf-8")
  data = data.replace("'", "\"")
  data = json.loads(data)
  ps = {}
  if policyId in policy_instances.keys():
    up_or_cre = 'updated'
    code = 201
  else:
    up_or_cre = 'created'
    code = 200
  policy_instances[policyId] = data
  ps["enforceStatus"] = "UNDEFINED"
  policy_status[policyId] = ps
  notification_destination[policyId] = data["notificationDestination"]
  #rand_status = randomise_status()
  #ps["policyId"] = policyId
  #ps["enforceStatus"] = rand_status
  #if rand_status == "NOT_ENFORCED":
    #rand_reason = randomise_reason()
    #ps["enforceReason"] = rand_reason
  #policy_status[policyId] = ps
  #return('The policy was ' + up_or_cre + ' for policy identity: ' + policyId, code)
  return(notification_destination, 200)

def get_policy(policyId):
  if policyId in policy_instances.keys():
    res = policy_instances[policyId]
    return(res, 200)
  else:
    return('The requested policy does not exist', 404)

def delete_policy(policyId):
  if policyId in policy_instances.keys():
    policy_instances.pop(policyId)
    policy_status.pop(policyId)
    return('The policy was deleted for policy id:' + policyId, 204)
  else:
    return('Policy id does not exist', 404)

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
    return('The requested policy type does not exist', 404)

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
  return(notificationDestination, 200)
