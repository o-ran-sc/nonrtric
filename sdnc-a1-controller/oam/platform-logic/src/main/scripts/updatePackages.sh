#! /bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2020 Nordix Foundation. All rights reserved.
#  ========================================================================
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#  ============LICENSE_END=================================================
#

updateFile() {
sed  -i .orig -e '
s/\(plugin=.\)org.openecomp.sdnc.\(prop\)/\org.onap.ccsdk.sli.plugins.\2/g
s/\(plugin=.\)org.openecomp.sdnc.\(ra\)/\1org.onap.ccsdk.sli.adaptors.\2/g
s/\(plugin=.\)org.openecomp.sdnc.\(restapicall\)/\1org.onap.ccsdk.sli.plugins.\2/g
s/\(plugin=.\)org.openecomp.sdnc.sli.\(aai\)/\1org.onap.ccsdk.sli.adaptors.\2/g
s/\(plugin=.\)org.openecomp.sdnc.sli.common/\1org.onap.ccsdk.sli.core.sli/g
s/\(plugin=.\)org.openecomp.sdnc.\(sli.provider\)/\1org.onap.ccsdk.sli.core.\2/g
s/\(plugin=\\\{0,1\}.\)com.att.sdnctl.\(sli.recording\)/\1org.openecomp.sdnc.\2/g
s/\(plugin=.\)org.openecomp.sdnc.sli.\(resource.mdsal\)/\1org.onap.ccsdk.sli.adaptors.\2/g
s/\(plugin=.\)org.openecomp.sdnc.sli\(resource.sql\)/\org.onap.ccsdk.sli.adaptors.\2/g
s/\(plugin=.\)org.openecomp.sdnc.sli.SliPluginUtils/\1org.onap.ccsdk.sli.core.slipluginutils/g
' $1
}

for file in $@
do
	updateFile $file
done
