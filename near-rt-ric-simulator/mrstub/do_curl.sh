# Function to execute curl and compare + print result
# args: GET|PUT|POST|DELETE <url> <target-response-code> [<json-file>]
# All calls made to 'localhist:'<port>
# Expects env PORT set to intended PORT
# Expects env RESULT to contain the target response body. '*' means that result is not checked
# Env BODY contains the response body after the call
# Any error will stop script execution
do_curl() {
    if [ $# -lt 3 ]; then
        echo "Need 3 or more parameters, <http-operation> <url> <response-code> [file]: "$@
        echo "Exting test script....."
        exit 1
    fi
    curlstr="curl -X "$1" -sw %{http_code} localhost:$PORT$2 -H accept:*/*"
    if [ $# -gt 3 ]; then
        curlstr=$curlstr" -H Content-Type:application/json --data-binary @"$4
    fi
    echo "  CMD:"$curlstr
    res=$($curlstr)
    status=${res:${#res}-3}
    body=${res:0:${#res}-3}
    export body
    if [ $status -ne $3 ]; then
        echo "  Error status:"$status" Expected status: "$3
        echo "  Body: "$body
        echo "Exting test script....."
        exit 1
    else
        echo "  OK, code: "$status"     (Expected)"
        echo "  Body: "$body
        if [ "$RESULT" == "*" ]; then
            echo "  Body contents not checked"
        else
            body="$(echo $body | tr -d '\n' )"
            if [ "$RESULT" == "$body" ]; then
                echo "  Body as expected"
            else
                echo "  Expected body: "$RESULT
                echo "Exiting....."
                exit 1
            fi
        fi
    fi
}