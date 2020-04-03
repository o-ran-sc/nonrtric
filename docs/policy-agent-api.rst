.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0

.. |nbsp| unicode:: 0xA0
   :trim:

.. |nbh| unicode:: 0x2011
   :trim:

.. _policy-agent-api:

================
Policy Agent API
================

This is the north bound API of the Policy Agent. By using this API, a service will get a lot of help in managing
policies.

By registering in the agent, the agent will take responsibility of synchronizing the policies created by the service in
the Near |nbh| RT |nbsp| RICs. This means that if a Near |nbh| RT |nbsp| RIC restarts, the agent will try to recreate
all the policies residing in the Near |nbh| RT |nbsp| RIC once it is started again. If this is not possible, it will
remove all policies belonging to the Near |nbh| RT |nbsp| RIC.

The agent also keeps an updated view of the policy types available, and which Near |nbh| RT |nbsp| RICs that support
which types. Also, the agent can tell if a Managed Element is managed by a certain Near |nbh| RT |nbsp| RIC.

.. contents::
   :depth: 4
   :local:

The Policy Agent has four distinguished parts, described in the sections below:

* Service Management
* Policy Management
* Near-RT RIC Repository
* Health Check

Service Management
==================

A service can register itself in the agent.

By providing a callback URL the service can get notifications from the agent.

A service can also register a "*Keep Alive Interval*", in seconds. By doing this the service promises to call the
agent's "*Keep Alive*" method, or create or delete policies, at a shorter interval than the one provided. If the
service, for some reason, is not able to do this, the agent will consider the service dead and delete all its policies,
both in the internal repository and in the Near |nbh| RT |nbsp| RICs where they are residing. **Note!** If the service
does not provide an interval, this means that the service MUST take the FULL responsibility to delete all its own
policies, otherwise they might stay in the system forever.

Service
-------

/service
~~~~~~~~

PUT
+++

  Register a service.

  **URL path:**
    /service

  **Parameters:**

    None.

  **Body:**  (*Required*)
      A JSON object (ServiceRegistrationInfo): ::

        {
          "callbackUrl": "string",         (An empty string means the service will never get any callbacks.)
          "keepAliveIntervalSeconds": 0,   (0 means the service will always be considered alive.)
          "serviceName": "string"          (Required, must be unique.)
        }

  **Responses:**

    200:
          Service updated.

    201:
          Service created.

    400:
          Something wrong with the ServiceRegistrationInfo.

  **Examples:**

    Call: ::

      curl -X PUT "http://localhost:8081/service" -H "Content-Type: application/json" -d "{
          \"callbackUrl\": \"URL\",
          \"keepAliveIntervalSeconds\": 0,
          \"serviceName\": \"existing\"
        }"

    Result:
      201: ::

         OK

    Call: ::

       curl -X PUT "http://localhost:8081/service" -H  "Content-Type: application/json" -d "{}"

    Result:
       400: ::

         Missing mandatory parameter 'serviceName'

/services
~~~~~~~~~

GET
+++

  Query service information.

  **URL path:**
    /services?name=<service-name>

  **Parameters:**

    name: (*Optional*)
      The name of the service.

  **Responses:**

    200:
          Array of JSON objects (ServiceStatus). ::

           {
               "callbackUrl": "string",             (Callback URL)
               "keepAliveIntervalSeconds": 0,       (Policy keep alive interval)
               "serviceName": "string",             (Identity of the service)
               "timeSinceLastActivitySeconds": 0    (Time since last invocation by the service)
           }

    404:
          Service is not found.

  **Examples:**

    Call: ::

      curl -X GET "http://localhost:8081/services?name=existing"

    Result:
      200: ::

         [
           {
             "serviceName":"existing",
             "keepAliveIntervalSeconds":0,
             "timeSinceLastActivitySeconds":7224,
             "callbackUrl":"URL"
           }
        ]

    Call: ::

      curl -X GET "http://localhost:8081/services?name=nonexistent"

    Result:
       404: ::

         Service not found

DELETE
++++++

  Delete a service.

  **URL path:**
    /services?name=<service-name>

  **Parameters:**

    name: (*Required*)
      The name of the service.

  **Responses:**

    204:
          OK

    404:
          Service not found.

  **Examples:**

    Call: ::

      curl -X DELETE "http://localhost:8081/services?name=existing"

    Result:
      204: ::

         OK

    Call: ::

      curl -X DELETE "http://localhost:8081/services?name=nonexistent"

    Result:
       404: ::

         Could not find service: nonexistent

/services/keepalive
~~~~~~~~~~~~~~~~~~~

POST
++++

  Heart beat from a service.

  **URL path:**
    /services/keepalive

  **Parameters:**

    name: (*Required*)
      The name of the service.

  **Responses:**

    200:
          OK

    404:
          Service is not found.

  **Examples:**

    Call: ::

      curl -X POST "http://localhost:8081/services/keepalive?name=existing"

    Result:
      200: ::

         OK

    Call: ::

      curl -X POST "http://localhost:8081/services/keepalive?name=nonexistent"

    Result:
       404: ::

         Could not find service: nonexistent

Policy Management
=================

Policies are based on types. Policy types are provided from Near |nbh| RT |nbsp| RICs. At startup, the Policy Agent
queries all Near |nbh| RT |nbsp| RICs for their supported types and stores them in its internal repository. It then
checks this at regular intervals to keep the repository of types up to date.

Policies can be queried, created, updated, and deleted. A policy is always created in a specific
Near |nbh| RT |nbsp| RIC.

When a policy is created, the Policy Agent stores information about it in its internal repository. At regular intervals,
it then checks with all Near |nbh| RT |nbsp| RICs that this repository is synchronized. If, for some reason, there is an
inconsistency, the agent will start a synchronization job and try to reflect the status of the Near |nbh| RT |nbsp| RIC.
If this fails, the agent will delete all policies for the specific Near |nbh| RT |nbsp| RIC in the internal repository
and set its state to *UNKNOWN*. This means that no interaction with the Near |nbh| RT |nbsp| RIC is possible until the
agent has been able to contact it again and re-synchronize its state in the repository.

Policy Types
------------

A policy type consists of a name and a JSON schema that defines the content of a policy based on the type.

/policy_types
~~~~~~~~~~~~~

GET
+++

  Query policy type names.

  **URL path:**
    /policy_types?ric=<name-of-ric>

  **Parameters:**

    ric: (*Optional*)
      The name of the Near |nbh| RT |nbsp| RIC to get types for.

  **Responses:**

    200:
          Array of policy type names.

    404:
          Near |nbh| RT |nbsp| RIC is not found.

  **Examples:**

    Call: ::

      curl -X GET "http://localhost:8081/policy_types"

    Result:
      200: ::

         [
           "STD_PolicyModelUnconstrained_0.2.0",
           "Example_QoETarget_1.0.0",
           "ERIC_QoSNudging_0.2.0"
        ]

    Call: ::

      curl -X GET "http://localhost:8081/policy_types?ric=nonexistent"

    Result:
       404: ::

         org.oransc.policyagent.exceptions.ServiceException: Could not find ric: nonexistent

/policy_schema
~~~~~~~~~~~~~~

GET
+++

  Returns one policy type schema definition.

  **URL path:**
    /policy_schema?id=<name-of-type>

   **Parameters:**

    id: (*Required*)
      The ID of the policy type to get the definition for.

  **Responses:**

    200:
          Policy schema as JSON schema.

    404:
          Policy type is not found.

  **Examples:**

    Call: ::

      curl -X GET "http://localhost:8081/policy_schema?id=STD_PolicyModelUnconstrained_0.2.0"

    Result:
      200: ::

        {
          "$schema": "http://json-schema.org/draft-07/schema#",
          "title": "STD_PolicyModelUnconstrained_0.2.0",
          "description": "Standard model of a policy with unconstrained scope id combinations",
          "type": "object",
          "properties": {
           "scope": {
              "type": "object",
              "properties": {
                "ueId": {"type": "string"},
                "groupId": {"type": "string"}
              },
              "minProperties": 1,
              "additionalProperties": false
            },
            "qosObjectives": {
              "type": "object",
              "properties": {
                "gfbr": {"type": "number"},
                "mfbr": {"type": "number"}
              },
              "additionalProperties": false
            },
            "resources": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "cellIdList": {
                    "type": "array",
                    "minItems": 1,
                    "uniqueItems": true,
                    "items": {
                      "type": "string"
                    }
                  },
                "additionalProperties": false,
                "required": ["cellIdList"]
              }
            }
          },
          "minProperties": 1,
          "additionalProperties": false,
          "required": ["scope"]
        }

    Call: ::

      curl -X GET "http://localhost:8081/policy_schema?id=nonexistent"

    Result:
       404: ::

         org.oransc.policyagent.exceptions.ServiceException: Could not find type: nonexistent

/policy_schemas
~~~~~~~~~~~~~~~

GET
+++

  Returns policy type schema definitions.

  **URL path:**
    /policy_schemas?ric=<name-of-ric>

   **Parameters:**

    ric: (*Optional*)
      The name of the Near |nbh| RT |nbsp| RIC to get the definitions for.

  **Responses:**

    200:
          An array of policy schemas as JSON schemas.

    404:
          Near |nbh| RT |nbsp| RIC is not found.

  **Examples:**

    Call: ::

      curl -X GET "http://localhost:8081/policy_schemas"

    Result:
      200: ::

        [{
          "$schema": "http://json-schema.org/draft-07/schema#",
          "title": "STD_PolicyModelUnconstrained_0.2.0",
          "description": "Standard model of a policy with unconstrained scope id combinations",
          "type": "object",
          "properties": {
           "scope": {
              "type": "object",
              .
              .
              .
          "additionalProperties": false,
          "required": ["scope"]
        },
         .
         .
         .
        {
          "$schema": "http://json-schema.org/draft-07/schema#",
          "title": "Example_QoETarget_1.0.0",
          "description": "Example QoE Target policy type",
          "type": "object",
          "properties": {
           "scope": {
              "type": "object",
              .
              .
              .
          "additionalProperties": false,
          "required": ["scope"]
        }]

    Call:
      curl -X GET "http://localhost:8081/policy_schemas?ric=nonexistent"

    Result:
       404: ::

         org.oransc.policyagent.exceptions.ServiceException: Could not find ric: nonexistent

Policy
------

A policy is defined by its type schema.

Once a service has created a policy, it is the service's responsibility to maintain its life cycle. Since policies are
transient, they will not survive a restart of a Near |nbh| RT |nbsp| RIC. But this is handled by the agent. When a
Near |nbh| RT |nbsp| RIC has been restarted, the agent will try to recreate the policies in the Near |nbh| RT |nbsp| RIC
that are stored in its local repository. This means that the service always must delete any policy it has created. There
are only two exceptions, see below:

- The service has registered a "*Keep Alive Interval*", then its policies will be deleted if it fails to notify the
  agent in due time.
- The agent completely fails to synchronize with a Near |nbh| RT |nbsp| RIC.

/policies
~~~~~~~~~

GET
+++

  Query policies.

  **URL path:**
    /policies?ric=<name-of-ric>&service=<name-of-service>&type=<name-of-type>

  **Parameters:**

    ric: (*Optional*)
      The name of the Near |nbh| RT |nbsp| RIC to get policies for.

    service: (*Optional*)
      The name of the service to get policies for.

    type: (*Optional*)
      The name of the policy type to get policies for.

  **Responses:**

    200:
          Array of JSON objects (PolicyInfo). ::

            {
              "id": "string",              (Identity of the policy)
              "json": "object",            (The configuration of the policy)
              "lastModified": "string",    (Timestamp, last modification time)
              "ric": "string",             (Identity of the target Near |nbh| RT |nbsp| RIC)
              "service": "string",         (The name of the service owning the policy)
              "type": "string"             (Name of the policy type)
            }

    404:
          Near |nbh| RT |nbsp| RIC or policy type not found.

  **Examples:**

    Call: ::

      curl -X GET "http://localhost:8081/policies?ric=existing"

    Result:
      200: ::

         [
           {
             "id": "Policy 1",
             "json": {
               "scope": {
                 "ueId": "UE 1",
                 "groupId": "Group 1"
               },
               "qosObjectives": {
                 "gfbr": 1,
                 "mfbr": 2
               },
               "cellId": "Cell 1"
             },
             "lastModified": "Wed, 01 Apr 2020 07:45:45 GMT",
             "ric": "existing",
             "service": "Service 1",
             "type": "STD_PolicyModelUnconstrained_0.2.0"
           },
           {
             "id": "Policy 2",
             "json": {
                 .
                 .
                 .
             },
             "lastModified": "Wed, 01 Apr 2020 07:45:45 GMT",
             "ric": "existing",
             "service": "Service 2",
             "type": "Example_QoETarget_1.0.0"
           }
        ]

    Call: ::

      curl -X GET "http://localhost:8081/policies?type=nonexistent"

    Result:
       404: ::

         Policy type not found

/policy
~~~~~~~

GET
+++

  Returns a policy configuration.

  **URL path:**
    /policy?id=<policy-id>

  **Parameters:**

    id: (*Required*)
      The ID of the policy instance.

  **Responses:**

    200:
          JSON object containing policy information. ::

            {
              "id": "string",                  (ID of policy)
              "json": "object",                (JSON with policy data speified by the type)
              "ownerServiceName": "string",    (Name of the service that created the policy)
              "ric": "string",                 (Name of the Near |nbh| RT |nbsp| RIC where the policy resides)
              "type": "string",                (Name of the policy type of the policy)
              "lastModified"                   (Timestamp, last modification time)
            }

    404:
          Policy is not found.

  **Examples:**

    Call: ::

      curl -X GET "http://localhost:8081/policy?id=Policy 1"

    Result:
      200: ::

         {
           "id": "Policy 1",
           "json", {
             "scope": {
               "ueId": "UE1 ",
               "cellId": "Cell 1"
             },
             "qosObjectives": {
               "gfbr": 319.5,
               "mfbr": 782.75,
               "priorityLevel": 268.5,
               "pdb": 44.0
             },
             "qoeObjectives": {
               "qoeScore": 329.0,
               "initialBuffering": 27.75,
               "reBuffFreq": 539.0,
               "stallRatio": 343.0
             },
             "resources": []
           },
           "ownerServiceName": "Service 1",
           "ric": "ric1",
           "type": "STD_PolicyModelUnconstrained_0.2.0",
           "lastModified": "Wed, 01 Apr 2020 07:45:45 GMT"
         }

    Call: ::

      curl -X GET "http://localhost:8081/policy?id=nonexistent"

    Result:
       404: ::

         Policy is not found

PUT
+++

  Create/Update a policy. **Note!** Calls to this method will also trigger "*Keep Alive*" for a service which has a
  "*Keep Alive Interval*" registered.

  **URL path:**
    /policy?id=<policy-id>&ric=<name-of-ric>&service=<name-of-service>&type=<name-of-policy-type>

  **Parameters:**

    id: (*Required*)
      The ID of the policy instance.

    ric: (*Required*)
      The name of the Near |nbh| RT |nbsp| RIC where the policy will be created.

    service: (*Required*)
      The name of the service creating the policy.

    type: (*Optional*)
      The name of the policy type.

  **Body:** (*Required*)
      A JSON object containing the data specified by the type.

  **Responses:**

    200:
          Policy updated.

    201:
          Policy created.

    404:
          Near |nbh| RT |nbsp| RIC or policy type is not found.

    423:
          Near |nbh| RT |nbsp| RIC is locked.

  **Examples:**

    Call: ::

      curl -X PUT "http://localhost:8081/policy?id=Policy%201&ric=ric1&service=Service%201&type=STD_PolicyModelUnconstrained_0.2.0"
        -H  "Content-Type: application/json"
        -d "{
              \"scope\": {
                \"ueId\": \"UE 1\",
                \"cellId\": \"Cell 1\"
              },
              \"qosObjectives\": {
                \"gfbr\": 319.5,
                \"mfbr\": 782.75,
                \"priorityLevel\": 268.5,
                \"pdb\": 44.0
              },
              \"qoeObjectives\": {
                \"qoeScore\": 329.0,
                \"initialBuffering\": 27.75,
                \"reBuffFreq\": 539.0,
                \"stallRatio\": 343.0
              },
              \"resources\": []
            }"

    Result:
      200

DELETE
++++++

  Deletes a policy. **Note!** Calls to this method will also trigger "*Keep Alive*" for a service which has a
  "*Keep Alive Interval*" registered.

  **URL path:**
    /policy?id=<policy-id>

  **Parameters:**

    id: (*Required*)
      The ID of the policy instance.

  **Responses:**

    204:
          Policy deleted.

    404:
          Policy is not found.

  **Examples:**

    Call: ::

      curl -X DELETE "http://localhost:8081/policy?id=Policy 1"

    Result:
      204

/policy_ids
~~~~~~~~~~~

GET
+++

  Query policy type IDs.

  **URL path:**
    /policy_ids?ric=<name-of-ric>&service=<name-of-service>&type=<name-of-policy-type>

  **Parameters:**

    ric: (*Optional*)
      The name of the Near |nbh| RT |nbsp| RIC to get policies for.

    service: (*Optional*)
      The name of the service to get policies for.

    type: (*Optional*)
      The name of the policy type to get policies for.

  **Responses:**

    200:
          Array of policy type names.

    404:
          RIC or policy type not found.

  **Examples:**

    Call: ::

      curl -X GET "http://localhost:8081/policy_ids"

    Result:
      200: ::

         [
           "Policy 1",
           "Policy 2",
           "Policy 3"
        ]

    Call: ::

      curl -X GET "http://localhost:8081/policy_ids?ric=nonexistent"

    Result:
       404: ::

         Ric not found

/policy_status
~~~~~~~~~~~~~~

GET
+++

  Returns the status of a policy.

  **URL path:**
    /policy_status?id=<policy-id>

  **Parameters:**

    id: (*Required*)
      The ID of the policy.

  **Responses:**

    200:
          JSON object with policy status.

    404:
          Policy not found.

Near-RT RIC Repository
======================

The agent keeps an updated view of the Near |nbh| RT |nbsp| RICs that are available in the system. A service can find
out which Near |nbh| RT |nbsp| RIC that manages a specific element in the network or which Near |nbh| RT |nbsp| RICs
that support a specific policy type.

Near-RT RIC
-----------

/ric
~~~~

GET
+++

  Returns the name of a Near |nbh| RT |nbsp| RIC managing a specific Mananged Element.

   **URL path:**
    /ric?managedElementId=<id-of-managed-element>

  **Parameters:**

    managedElementId: (*Optional*)
      The ID of the Managed Element.

  **Responses:**

    200:
          Name of the Near |nbh| RT |nbsp| RIC managing the Managed Element.

    404:
          No Near |nbh| RT |nbsp| RIC manages the given Managed Element.

  **Examples:**

    Call: ::

      curl -X GET "http://localhost:8081/ric?managedElementId=Node 1"

    Result:
      200: ::

        Ric 1

    Call: ::

      curl -X GET "http://localhost:8081/ric?managedElementId=notmanaged"

    Result:
       404

/rics
~~~~~

GET
+++

  Query Near |nbh| RT |nbsp| RIC information.

   **URL path:**
    /rics?policyType=<name-of-policy-type>

  **Parameters:**

    policyType: (*Optional*)
      The name of the policy type.

  **Responses:**

    200:
          Array of JSON objects containing Near |nbh| RT |nbsp| RIC information. ::

            [
              {
                "managedElementIds": [
                  "string"
                ],
                "policyTypes": [
                  "string"
                ],
                "ricName": "string"
              }
            ]

    404:
          Policy type is not found.

  **Examples:**

    Call: ::

      curl -X GET "http://localhost:8081/rics?policyType=STD_PolicyModelUnconstrained_0.2.0"

    Result:
      200: ::

        [
          {
            "managedElementIds": [
              "ME 1",
              "ME 2"
            ],
            "policyTypes": [
              "STD_PolicyModelUnconstrained_0.2.0",
              "Example_QoETarget_1.0.0",
              "ERIC_QoSNudging_0.2.0"
            ],
            "ricName": "Ric 1"
          },
            .
            .
            .
          {
            "managedElementIds": [
              "ME 3"
            ],
            "policyTypes": [
              "STD_PolicyModelUnconstrained_0.2.0"
            ],
            "ricName": "Ric X"
          }
        ]

    Call: ::

      curl -X GET "http://localhost:8081/rics?policyType=nonexistent"

    Result:
       404: ::

        Policy type not found

Health Check
============

The status of the Policy Agent.

Health Check
------------

/status
~~~~~~~

GET
+++

  Returns the status of the Policy Agent.

   **URL path:**
    /status

  **Parameters:**

    None.

  **Responses:**

    200:
          Service is living.

  **Examples:**

    Call: ::

      curl -X GET "http://localhost:8081/status"

    Result:
      200
