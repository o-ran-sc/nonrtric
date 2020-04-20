.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2020 Nordix

.. _sdnc-a1-controller-api:

.. |nbsp| unicode:: 0xA0
   :trim:

.. |nbh| unicode:: 0x2011
   :trim:

##################
SDNC A1 Controller
##################

The A1 of a Near |nbh| RT |nbsp| RIC can be used through the SDNC A1 Controller.

Any version of the A1 API can be used. A call to the SDNC A1 Controller always contains the actual URL to the
Near |nbh| RT |nbsp| RIC, so here any of the supported API versions can be used. The controller just calls the provided
URL with the provided data.

Get Policy Type
~~~~~~~~~~~~~~~

POST
++++

Gets a policy type.

**URL path:**
  /restconf/operations/A1-ADAPTER-API:getA1PolicyType

**Parameters:**
  None.

**Body:** (*Required*)
    A JSON. ::

      {
        "input": {
          "near-rt-ric-url": "<url-to-near-rt-ric-to-get-type>"
        }
      }

**Responses:**
  200:
    A JSON where the body tag contains the JSON object of the policy type. ::

      {
        "output": {
          "http-status": "integer",
          "body": "{
            <policy-type>
          }"
        }
      }

**Examples:**
  Call: ::

    curl -X POST "http://localhost:8282/restconf/operations/A1-ADAPTER-API:getA1PolicyType"
    -H "Content-Type: application/json" -d '{
      "input": {
        "near-rt-ric-url": "http://nearRtRic-sim1:8085/a1-p/policytypes/11"
      }
    }'

  Result:
    200 ::

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

**URL path:**
  /restconf/operations/A1-ADAPTER-API:putA1Policy

**Parameters:**
  None.

**Body:** (*Required*)
    A JSON where the body tag contains the JSON object of the policy. ::

      {
        "input": {
          "near-rt-ric-url": "<url-to-near-rt-ric-to-put-policy>",
          "body": "object"
        }
      }

**Responses:**
  200:
    A JSON with the response. ::

      {
        "output": {
          "http-status": "integer"
        }
      }

**Examples:**
  Call: ::

    curl -X POST "http://localhost:8282/restconf/operations/A1-ADAPTER-API:getA1PolicyType"
    -H "Content-Type: application/json" -d '{
      "input": {
        "near-rt-ric-url": "http://nearRtRic-sim1:8085/a1-p/policytypes/11/policies/3d2157af-6a8f-4a7c-810f-38c2f824bf12",
        "body": "{
          "blocking_rate":20,
          "enforce":true,
          "trigger_threshold":10,
          "window_length":10
        }"
      }
    }'

  Result:
    200 ::

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

**URL path:**
  /restconf/operations/A1-ADAPTER-API:getA1Policy

**Parameters:**
  None.

**Body:** (*Required*)
    A JSON. ::

      {
        "input": {
          "near-rt-ric-url": "<url-to-near-rt-ric-to-get-policy>"
        }
      }

**Responses:**
  200:
    A JSON where the body tag contains the JSON object of the policy. ::

      {
        "output": {
          "http-status": "integer",
          "body": "{
            <policy>
          }"
        }
      }

**Examples:**
  Call: ::

    curl -X POST "http://localhost:8282/restconf/operations/A1-ADAPTER-API:getA1Policy"
    -H "Content-Type: application/json" -d '{
      "input": {
        "near-rt-ric-url": "http://nearRtRic-sim1:8085/a1-p/policytypes/11/policies/3d2157af-6a8f-4a7c-810f-38c2f824bf12"
      }
    }'

  Result:
    200 ::

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

**URL path:**
  /restconf/operations/A1-ADAPTER-API:deleteA1Policy

**Parameters:**
  None.

**Body:** (*Required*)
    A JSON. ::

      {
        "input": {
          "near-rt-ric-url": "<url-to-near-rt-ric-to-delete-policy>"
        }
      }

**Responses:**
  200:
    A JSON with the response. ::

      {
        "output": {
          "http-status": "integer"
        }
      }

**Examples:**
  Call: ::

    curl -X POST "http://localhost:8282/restconf/operations/A1-ADAPTER-API:deleteA1Policy"
    -H "Content-Type: application/json" -d '{
      "input": {
        "near-rt-ric-url": "http://nearRtRic-sim1:8085/a1-p/policytypes/11/policies/3d2157af-6a8f-4a7c-810f-38c2f824bf12"
      }
    }'

  Result:
    200 ::

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

**URL path:**
  /restconf/operations/A1-ADAPTER-API:getA1PolicyStatus

**Parameters:**
  None.

**Body:** (*Required*)
    A JSON. ::

      {
        "input": {
          "near-rt-ric-url": "<url-to-near-rt-ric-to-get-policy-status>"
        }
      }

**Responses:**
  200:
    A JSON where the body tag contains the JSON object with the policy status according to the API version used. ::

      {
        "output": {
          "http-status": "integer",
          "body": "{
            <policy-status-object>
          }"
        }
      }

**Examples:**
  Call: ::

    curl -X POST "http://localhost:8282/restconf/operations/A1-ADAPTER-API:getA1PolicyStatus"
    -H "Content-Type: application/json" -d '{
      "input": {
        "near-rt-ric-url": "http://nearRtRic-sim1:8085/a1-p/policytypes/11/policies/3d2157af-6a8f-4a7c-810f-38c2f824bf12/status"
      }
    }'

  Result:
    200 ::

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
