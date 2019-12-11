#!/bin/bash
# Different commands for the simulator.
# By running this, nothing should return an error.

# Make a test
curl -v "http://localhost:8085/"

# PUT a policy type STD_QoSNudging_0.1.0
curl -X PUT -v "http://localhost:8085/policytypes/STD_QoSNudging_0.1.0" -H "accept: application/json" -H "Content-Type: application/json" -d "{\"\$schema\": \"http://json-schema.org/draft-07/schema#\",\"title\": \"STD_QoSNudging_0.1.0\",\"description\": \"QoS policy type with ueId and qosId scope, version 0.1.0\",\"type\": \"object\",\"properties\": {\"scope\": {\"type\": \"object\",\"properties\": {\"ueId\": {\"type\": \"string\"},\"qosId\": {\"type\": \"string\"}},\"additionalProperties\": false,\"required\": [\"ueId\", \"qosId\"]},\"statement\": {\"type\": \"object\",\"properties\": {\"priorityLevel\": {\"type\": \"number\"}},\"additionalProperties\": false,\"required\": [\"priorityLevel\"]}}}"

# GET policy types
curl -v "http://localhost:8085/A1-P/v1/policytypes"

# GET policy type identities
curl -v "http://localhost:8085/A1-P/v1/policytypes/identities"

# GET policy type STD_QoSNudging_0.1.0
curl -v "http://localhost:8085/A1-P/v1/policytypes/STD_QoSNudging_0.1.0"

# PUT a policy instance pid1
curl -X PUT -v "http://localhost:8085/A1-P/v1/policies/pid1" -H "accept: application/json" -H "Content-Type: application/json" -d  "{\"policyId\": \"pid1\", \"policyTypeId\": \"STD_QoSNudging_0.1.0\", \"policyClause\": {\"scope\": {\"ueId\": \"ue1\", \"groupId\": \"group1\", \"sliceId\": \"slice1\", \"qosId\": \"qos1\", \"cellId\": \"cell1\"}, \"statement\": {\"priorityLevel\": 5}}, \"notificationDestination\": \"http://localhost:8085/policynotifications\"}"

# PUT a policy instance pid2
curl -X PUT -v "http://localhost:8085/A1-P/v1/policies/pid2" -H "accept: application/json" -H "Content-Type: application/json" -d  "{\"policyId\": \"pid2\", \"policyTypeId\": \"STD_QoSNudging_0.1.0\", \"policyClause\": {\"scope\": {\"ueId\": \"ue2\", \"groupId\": \"group2\", \"sliceId\": \"slice2\", \"qosId\": \"qos2\", \"cellId\": \"cell2\"}, \"statement\": {\"priorityLevel\": 5}}, \"notificationDestination\": \"http://localhost:8085/policynotifications\"}"

# SET status for pid1 and pid2
curl -X PUT "http://localhost:8085/pid1/NOT_ENFORCED/300"
curl -X PUT "http://localhost:8085/pid2/ENFORCED"

# GET policy status
curl -v "http://localhost:8085/A1-P/v1/policies/status"

# GET policies
curl -v "http://localhost:8085/A1-P/v1/policies"

# GET policy identities
curl -v "http://localhost:8085/A1-P/v1/policies/identities"

# DELETE policy instance pid2
curl -X DELETE -v "http://localhost:8085/A1-P/v1/policies/pid2"

# GET policy instance pid1
curl -v "http://localhost:8085/A1-P/v1/policies/pid1"

# GET policy status for pid1
curl -v "http://localhost:8085/A1-P/v1/policies/pid1/status"

# PUT policy type subscription
curl -X PUT -v "http://localhost:8085/A1-P/v1/policytypes/subscription" -H "accept: application/json" -H "Content-Type: application/json" -d  "{\"notificationDestination\": \"http://localhost:8085/subscription/address\"}"

# GET policy type subscription
curl -v "http://localhost:8085/A1-P/v1/policytypes/subscription"
