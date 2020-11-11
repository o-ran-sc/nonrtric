.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2020 Nordix

.. sdnc-a1-controller-api:

.. |nbsp| unicode:: 0xA0
   :trim:

.. |nbh| unicode:: 0x2011
   :trim:

######################
SDNC A1 Controller API
######################

The A1 of a Near |nbh| RT |nbsp| RIC can be used through the SDNC A1 Controller.

The OSC A1 Controller supports using multiple versions of A1 API southbound. By passing the full URL for each southbound
A1 operation the problem of version-specific URL formats is avoided.

Since different versions of A1 operations may use different data formats for data payloads for similar REST requests and
responses the data formatting requirements are flexible, so version-specific encoding/decoding is handled by the service
that requests the A1 operation.

Get Policy Type
~~~~~~~~~~~~~~~

POST
++++

Gets a policy type.

Definition
""""""""""

**URL path:**

/restconf/operations/A1-ADAPTER-API:getA1PolicyType

**Parameters:**

None.

**Body:** (*Required*)

A JSON object. ::

  {
    "input": {
      "near-rt-ric-url": "<url-to-near-rt-ric-to-get-type>"
    }
  }

**Responses:**

200:

A JSON object where the body tag contains the JSON object of the policy type. ::

  {
    "output": {
      "http-status": "integer",
      "body": "{
        <policy-type>
      }"
    }
  }

Examples
""""""""

Get a policy type from a Near |nbh| RT |nbsp| RIC that is using the OSC 2.1.0 version. The STD 1.1.3 version does not
support types, so this function is not available for that version.

**Call**: ::

    curl -X POST "http://localhost:8282/restconf/operations/A1-ADAPTER-API:getA1PolicyType"
    -H "Content-Type: application/json" -d '{
      "input": {
        "near-rt-ric-url": "http://nearRtRic-sim1:8085/a1-p/policytypes/11"
      }
    }'

**Result**:

200: ::

  {
    "output": {
      "http-status": 200,
      "body": "{
        \"$schema\": \"http://json-schema.org/draft-07/schema#\",
        \"title\": \"Example_QoETarget_1.0.0\",
        \"description\": \"Example QoE Target policy type\",
        \"type\": \"object\",
        \"properties\": {
          \"scope\": {
            \"type\": \"object\",
            \"properties\": {
              \"ueId\": {
                \"type\": \"string\"
              },
              \"sliceId\": {
                \"type\": \"string\"
              },
              \"qosId\": {
                \"type\": \"string\"
              },
              \"cellId\": {
                \"type\": \"string\"
              }
            },
            \"additionalProperties\": false,
            \"required\": [
              \"ueId\",
              \"sliceId\"
            ]
          },
          \"statement\": {
            \"type\": \"object\",
            \"properties\": {
              \"qoeScore\": {
                \"type\": \"number\"
              },
              \"initialBuffering\": {
                \"type\": \"number\"
              },
              \"reBuffFreq\": {
                \"type\": \"number\"
              },
              \"stallRatio\": {
                \"type\": \"number\"
              }
            },
            \"minProperties\": 1,
            \"additionalProperties\": false
          }
        }
      }"
    }
  }

Put Policy
~~~~~~~~~~

POST
++++

Creates or updates a policy instance.

Definition
""""""""""

**URL path:**

/restconf/operations/A1-ADAPTER-API:putA1Policy

**Parameters:**

None.

**Body:** (*Required*)

A JSON object where the body tag contains the JSON object of the policy. ::

  {
    "input": {
      "near-rt-ric-url": "<url-to-near-rt-ric-to-put-policy>",
      "body": "<policy-as-json-string>"
    }
  }

**Responses:**

200:

A JSON object with the response. ::

  {
    "output": {
      "http-status": "integer"
    }
  }

Examples
""""""""

**Call**:

Create a policy in a Near |nbh| RT |nbsp| RIC that is using the OSC 2.1.0 version. ::

    curl -X POST "http://localhost:8282/restconf/operations/A1-ADAPTER-API:putA1Policy"
    -H "Content-Type: application/json" -d '{
      "input": {
        "near-rt-ric-url": "http://nearRtRic-sim1:8085/a1-p/policytypes/11/policies/5000",
        "body": "{
          "blocking_rate":20,
          "enforce":true,
          "trigger_threshold":10,
          "window_length":10
        }"
      }
    }'

Create a policy in a Near |nbh| RT |nbsp| RIC that is using the STD 1.1.3 version. ::

    curl -X POST http://localhost:8282/restconf/operations/A1-ADAPTER-API:putA1Policy
    -H Content-Type:application/json -d '{
      "input": {
        "near-rt-ric-url": "http://ricsim_g2_1:8085/A1-P/v1/policies/5000",
        "body": "{
          "scope": {
            "ueId": "ue5000",
            "qosId": "qos5000"
          },
          "qosObjective": {
            "priorityLevel": 5000
          }
        }"
      }
    }'

**Result**:

The result is the same irrespective of which API that is used.

200: ::

  {
    "output": {
      "http-status": 200
    }
  }

Get Policy
~~~~~~~~~~

POST
++++

Gets a policy instance.

Definition
""""""""""

**URL path:**

/restconf/operations/A1-ADAPTER-API:getA1Policy

**Parameters:**

None.

**Body:** (*Required*)

A JSON object. ::

  {
    "input": {
      "near-rt-ric-url": "<url-to-near-rt-ric-to-get-policy>"
    }
  }

**Responses:**

200:
  A JSON object where the body tag contains the JSON object of the policy. ::

    {
      "output": {
        "http-status": "integer",
        "body": "{
          <result>
        }"
      }
    }

Examples
""""""""

**Call**:

Get **all** policy IDs from a Near |nbh| RT |nbsp| RIC that is using the OSC 2.1.0 version. ::

    curl -X POST http://localhost:8282/restconf/operations/A1-ADAPTER-API:getA1Policy
    -H Content-Type:application/json -d '{
      "input": {
        "near-rt-ric-url":"http://ricsim_g1_1:8085/a1-p/policytypes/11/policies"
      }
    }'

Get **all** policy IDs from a Near |nbh| RT |nbsp| RIC that is using the STD 1.1.3 version. ::

    curl -X POST http://localhost:8282/restconf/operations/A1-ADAPTER-API:getA1Policy
    -H Content-Type:application/json -d '{
      "input": {
        "near-rt-ric-url":"http://ricsim_g2_1:8085/A1-P/v1/policies"
      }
    }'

**Result**:

The result is the same irrespective of which API that is used.

200: ::

  {
    "output": {
      "http-status":200,
      "body":"[
        \"5000\",
          .
          .
          .
        \"6000\"
      ]"
    }
  }

**Call**:

Get **a specific** policy from a Near |nbh| RT |nbsp| RIC that is using the OSC 2.1.0 version. ::

    curl -X POST "http://localhost:8282/restconf/operations/A1-ADAPTER-API:getA1Policy"
    -H "Content-Type: application/json" -d '{
      "input": {
        "near-rt-ric-url": "http://nearRtRic-sim1:8085/a1-p/policytypes/11/policies/5000"
      }
    }'

Get **a specific** policy from a Near |nbh| RT |nbsp| RIC that is using the STD 1.1.3 version. ::

    curl -X POST http://localhost:8282/restconf/operations/A1-ADAPTER-API:getA1Policy
    -H Content-Type:application/json -d '{
      "input": {
        "near-rt-ric-url":"http://ricsim_g2_1:8085/A1-P/v1/policies/5000"
      }
    }'

**Result**:

The result is the same irrespective of which API that is used.

200: ::

  {
    "output": {
      "http-status": 200,
      "body": "{
        \"blocking_rate\": 20,
        \"enforce\": true,
        \"trigger_threshold\": 10,
        \"window_length\": 10
      }"
    }
  }

Delete Policy
~~~~~~~~~~~~~

POST
++++

Deletes a policy instance.

Definition
""""""""""

**URL path:**

/restconf/operations/A1-ADAPTER-API:deleteA1Policy

**Parameters:**

None.

**Body:** (*Required*)

A JSON object. ::

  {
    "input": {
      "near-rt-ric-url": "<url-to-near-rt-ric-to-delete-policy>"
    }
  }

**Responses:**

200:

A JSON object with the response. ::

  {
    "output": {
      "http-status": "integer"
    }
  }

Examples
""""""""

**Call**:

Delete a policy from a Near |nbh| RT |nbsp| RIC that is using the OSC 2.1.0 version. ::

    curl -X POST "http://localhost:8282/restconf/operations/A1-ADAPTER-API:deleteA1Policy"
    -H "Content-Type: application/json" -d '{
      "input": {
        "near-rt-ric-url": "http://nearRtRic-sim1:8085/a1-p/policytypes/11/policies/5000"
      }
    }'

Delete a policy from a Near |nbh| RT |nbsp| RIC that is using the STD 1.1.3 version. ::

    curl -X POST "http://localhost:8282/restconf/operations/A1-ADAPTER-API:deleteA1Policy"
    -H "Content-Type: application/json" -d '{
      "input": {
        "near-rt-ric-url": "http://ricsim_g2_1:8085/A1-P/v1/policies/5000"
      }
    }'

**Result**:

The result is the same irrespective of which API that is used.

200: ::

  {
    "output": {
      "http-status": 202
    }
  }

Get Policy Status
~~~~~~~~~~~~~~~~~

POST
++++

Get the status of a policy instance.

Definition
""""""""""

**URL path:**

/restconf/operations/A1-ADAPTER-API:getA1PolicyStatus

**Parameters:**

None.

**Body:** (*Required*)

A JSON object. ::

  {
    "input": {
      "near-rt-ric-url": "<url-to-near-rt-ric-to-get-policy-status>"
    }
  }

**Responses:**

200:

A JSON object where the body tag contains the JSON object with the policy status according to the API version used. ::

  {
    "output": {
      "http-status": "integer",
      "body": "{
        <policy-status-object>
      }"
    }
  }

Examples
""""""""

**Call**:

Get the policy status for a specific policy from a Near |nbh| RT |nbsp| RIC that is using the OSC 2.1.0 version. ::

    curl -X POST "http://localhost:8282/restconf/operations/A1-ADAPTER-API:getA1PolicyStatus"
    -H "Content-Type: application/json" -d '{
      "input": {
        "near-rt-ric-url": "http://nearRtRic-sim1:8085/a1-p/policytypes/11/policies/5000/status"
      }
    }'

**Result**:

200: ::

  {
    "output": {
      "http-status": 200,
      "body": "{
        \"instance_status\": \"IN EFFECT\",
        \"has_been_deleted\": \"true\",
        \"created_at\": \"Wed, 01 Apr 2020 07:45:45 GMT\"
      }"
    }
  }

**Call**:

Get the policy status for a specific policy from a Near |nbh| RT |nbsp| RIC that is using the STD 1.1.3 version. ::

    curl -X POST "http://localhost:8282/restconf/operations/A1-ADAPTER-API:getA1PolicyStatus"
    -H "Content-Type: application/json" -d '{
      "input": {
        "near-rt-ric-url": "http://ricsim_g2_1:8085/A1-P/v1/policies/5000/status"
      }
    }'

**Result**:

200: ::

  {
    "output": {
      "http-status": 200,
      "body": "{
        \"enforceStatus\": \"UNDEFINED\"
      }"
    }
  }
