#!/bin/sh
# ============LICENSE_START====================================================
#  Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
# =============================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
# ============LICENSE_END======================================================

tmout=120
cmd=
while getopts c:t: opt; do
    case "$opt" in
    c) cmd="$OPTARG" ;;
    t) tmout="$OPTARG" ;;
    esac
done
nargs=$(expr $OPTIND - 1)
shift $nargs

even_args=$(expr $# % 2)
if [ $# -lt 2 -o $even_args -ne 0 ]; then
    echo "args: [-t timeout] [-c command] hostname1 port1 hostname2 port2 ..." >&2
    exit 1
fi

while [ $# -ge 2 ]; do
    export host=$1
    export port=$2
    shift
    shift

    echo "Waiting for $host port $port..."
    timeout $tmout sh -c 'until nc -vz "$host" "$port"; do echo -n ".";
        sleep 1; done'
    rc=$?

    if [ $rc != 0 ]; then
        echo "$host port $port cannot be reached"
        exit $rc
    fi
done

$cmd

exit 0
