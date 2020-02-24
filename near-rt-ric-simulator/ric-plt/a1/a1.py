#!/u:sr/bin/env python3
import copy
import datetime
import json
import logging
#import requests

from connexion import NoContent
from flask import Flask, escape, request, make_response
from jsonschema import validate
from random import random, choice
from var_declaration import policy_instances, policy_types, policy_status, policy_type_per_instance

def get_all_policy_identities():
  if len(request.args) == 0:
    return(list(policy_instances.keys()), 200)
  elif 'policyTypeId' in request.args:
    policyTypeId = request.args.get('policyTypeId')
    if policyTypeId not in list(policy_types.keys()):
      return(set_error(None, "The policy type provided does not exist.", 400, "The policy type " + data["policyTypeId"] + " is not defined as a policy type.", None, None, "policyTypeId", None))
    else:
      return(list({key for key in policy_instances.keys() if policy_type_per_instance[key]==policyTypeId}), 200)
  else:
    return(send_error_code(request.args))

def put_policy(policyId):
  data = request.data.decode("utf-8")
  data = data.replace("'", "\"")
  data = json.loads(data)
  ps = {}
  if 'policyTypeId' in request.args:
    policyTypeId = request.args.get('policyTypeId')

    if policyTypeId not in list(policy_types.keys()):
      return(set_error(None, "The policy type provided does not exist.", 400, "The policy type " + policyTypeId + " is not defined as a policy type.", None, None, "policyTypeId", None))

    policy_schema = policy_types[policyTypeId]["policySchema"]
    try:
      validate(instance=data, schema=policy_schema)
    except:
      return(set_error(None, "The json does not validate against the schema.", 400, None, None, None, None, None))

    for i in list(policy_instances.keys()):
      if policyId != i and \
         data == policy_instances[i] and \
         policyTypeId == policy_type_per_instance[i]:
        return(set_error(None, "The policy already exists with a different id.", 400, "No action has been taken. The id of the existing policy instance is: " + i + ".", None, None, None, None))

  if policyId in list(policy_instances.keys()):
    if data["scope"] != policy_instances[policyId]["scope"]:
      return(set_error(None, "The policy already exists with a different scope.", 400, "The policy put involves a modification of the existing scope, which is not allowed.", None, None, "scope", None))

  if 'code' in request.args:
    return(send_error_code(request.args))

  if policyId in policy_instances.keys():
    code = 201
  else:
    code = 200

  policy_instances[policyId] = data
  policy_status[policyId] = set_status("UNDEFINED")
  if 'policyTypeId' in request.args:
    status_schema = policy_types[policyTypeId]["statusSchema"]
    try:
      validate(instance=policy_status[policyId], schema=status_schema)
    except:
      return(set_error(None, "The json does not validate against the status schema.", 400, None, None, None, None, None))
    policy_type_per_instance[policyId] = policyTypeId
  else:
    policy_type_per_instance[policyId] = "UNDEFINED"

  response = make_response(policy_instances[policyId], code)
  if code == 201:
    response.headers['Location'] = "http://localhost:8085/A1-P/v1/policies/" + policyId
  return response

def set_status(*args):
  ps = {}
  ps["enforceStatus"] = args[0]
  if len(args) == 2:
    ps["enforceReason"] = args[1]
  if len(args) > 2:
    return(set_error(None, "Too many arguments", 400, "There should be no more than two status arguments: enforceStatus and enforceReason", None, None, None, None))
  return ps

def get_policy(policyId):
  if len(request.args) == 0:
    if policyId in policy_instances.keys():
      res = policy_instances[policyId]
      res["enforceStatus"] = policy_status[policyId]["enforceStatus"]
      return(res, 200)
    else:
      return(set_error(None, "The requested policy does not exist.", 404, None, None, None, "policyId", None))
  else:
    return(send_error_code(request.args))

def delete_policy(policyId):
  if len(request.args) == 0:
    if policyId in policy_instances.keys():
      policy_instances.pop(policyId)
      policy_status.pop(policyId)
      policy_type_per_instance.pop(policyId)
      return(None, 204)
    else:
      return(set_error(None, "The policy identity does not exist.", 404, "No policy instance has been deleted.", None, None, "policyId", None))
  else:
    return(send_error_code(request.args))

def get_policy_status(policyId):
  if len(request.args) == 0:
    if policyId in policy_instances.keys():
      return(policy_status[policyId], 200)
    else:
      return(set_error(None, "The policy identity does not exist.", 404, "There is no existing policy instance with the identity: " + policyId, None, None, "policyId", None))
  else:
    return(send_error_code(request.args))

def get_all_policytypes_identities():
  if len(request.args) == 0:
    return(list(policy_types.keys()), 200)
  else:
    return(send_error_code(request.args))

def get_policytypes(policyTypeId):
  if len(request.args) == 0:
    if policyTypeId in policy_types.keys():
      return(policy_types[policyTypeId], 200)
    else:
      return(set_error(None, "The requested policy type does not exist.", 404, None, None, None, "policyTypeId", None))
  else:
    return(send_error_code(request.args))

def set_error(type_of, title, status, detail, instance, cause, param, reason):
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
  if cause is not None:
    error["cause"] = cause
  if param is not None:
    params["param"] = param
  if reason is not None:
    params["reason"] = reason
  if params:
    error["invalidParams"] = params
  return(error, error["status"])

def send_error_code(args):
  if 'code' in args.keys():
    code = args['code']
    if code == '405':
      return(set_error(None, "Method not allowed", 405, "Method not allowed for the URI", None, None, None, None))
    elif code == '429':
      return(set_error(None, "Too many requests", 429, "Too many requests have been sent in a given amount of time", None, None, None, None))
    elif code == '507':
      return(set_error(None, "Insufficient storage", 507, "The method could not be performed on the resource because the provider is unable to store the representation needed to successfully complete the request", None, None, None, None))
    elif code == '503':
      return(set_error(None, "Service unavailable", 503, "The provider is currently unable to handle the request due to a temporary overload", None, None, None, None))
    else:
      return(set_error(None, "Not found", 400, "No resource found at the URI", None, None, None, None))
  else:
    return(set_error(None, "Not found", 400, "No resource found at the URI", None, None, None, None))
