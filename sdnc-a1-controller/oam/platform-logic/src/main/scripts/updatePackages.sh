#! /bin/bash

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
