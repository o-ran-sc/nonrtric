#!/bin/bash
# Different commands for the simulator.
# By running this, nothing should return an error.

# Make a test
curl -v "http://localhost:8085/"

# PUT a policy type STD_QoSNudging_0.1.0
curl -X PUT -v "http://localhost:8085/policytypes/STD_QoSNudging_0.1.0" -H "accept: application/json" -H "Content-Type: application/json" --data-binary @policy_type_STD_QoSNudging_0.1.0.json.json

# GET policy types
curl -v "http://localhost:8085/A1-P/v1/policytypes"

# GET policy type identities
curl -v "http://localhost:8085/A1-P/v1/policytypes/identities"

# GET policy type STD_QoSNudging_0.1.0
curl -v "http://localhost:8085/A1-P/v1/policytypes/STD_QoSNudging_0.1.0"

# PUT a policy instance pi1
curl -X PUT -v "http://localhost:8085/A1-P/v1/policies/pi1" -H "accept: application/json" -H "Content-Type: application/json" --data-binary @policy_instance_1_STD_QoSNudging_0.1.0.json

# PUT a policy instance pi2
curl -X PUT -v "http://localhost:8085/A1-P/v1/policies/pi2" -H "accept: application/json" -H "Content-Type: application/json" --data-binary @policy_instance_2_STD_QoSNudging_0.1.0.json

# SET status for pi1 and pi2
curl -X PUT "http://localhost:8085/pi1/NOT_ENFORCED/300"
curl -X PUT "http://localhost:8085/pi2/ENFORCED"

# GET policy status
curl -v "http://localhost:8085/A1-P/v1/policies/status"

# GET policies
curl -v "http://localhost:8085/A1-P/v1/policies"

# GET policy identities
curl -v "http://localhost:8085/A1-P/v1/policies/identities"

# DELETE policy instance pi2
curl -X DELETE -v "http://localhost:8085/A1-P/v1/policies/pi2"

# PUT a different policy instance pi1 (i.e. update it)
curl -X PUT -v "http://localhost:8085/A1-P/v1/policies/pi1" -H "accept: application/json" -H "Content-Type: application/json" --data-binary @policy_instance_1_bis_STD_QoSNudging_0.1.0.json

# GET policy instance pi1
curl -v "http://localhost:8085/A1-P/v1/policies/pi1"

# GET policy status for pi1
curl -v "http://localhost:8085/A1-P/v1/policies/pi1/status"

# PUT policy type subscription
curl -X PUT -v "http://localhost:8085/A1-P/v1/policytypes/subscription" -H "accept: application/json" -H "Content-Type: application/json" -d  "{\"notificationDestination\": \"http://localhost:8085/subscription/address\"}"

# GET policy type subscription
curl -v "http://localhost:8085/A1-P/v1/policytypes/subscription"
