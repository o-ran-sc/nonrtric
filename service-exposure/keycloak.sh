#!/bin/bash

if [ -z "$1" ]
  then
    echo "No argument supplied"
    exit 1
fi

OPERATION=$1

if [ "$OPERATION" == "deploy" ]; then
        echo "Deploying applications..."
        echo "-------------------------"
        istioctl kube-inject -f postgres.yaml | kubectl apply -f -
	sleep 10
        istioctl kube-inject -f keycloak.yaml | kubectl apply -f -
        echo ""
        echo "Waiting for pods to start..."
        echo "----------------------------"
        kubectl wait deployment -n default postgres --for=condition=available --timeout=90s
        kubectl wait deployment -n default keycloak --for=condition=available --timeout=300s
        echo ""
        echo "Checking pod status..."
        echo "----------------------"
        kubectl get pods -n default
elif [ "$OPERATION" == "undeploy" ]; then
        echo "Undeploying applications..."
        echo "---------------------------"
	kubectl delete -f keycloak.yaml
	kubectl delete -f postgres.yaml
else
	echo "Unrecogized operation ${OPERATION}"
	exit 1
fi

exit 0
