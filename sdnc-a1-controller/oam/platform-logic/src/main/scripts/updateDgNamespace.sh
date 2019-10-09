#! /bin/bash

updateFile() {
sed  -i .orig -e '
s/openecomp.org/onap.org/g
' $1
}

for file in $@
do
	updateFile $file
done
