#!/bin/bash

cat > /tmp/daexim-export.json <<-END
{ 
   "input": { 
     "data-export-import:run-at": 5 
   } 
}
END

curl -v -H "Content-Type: application/json" -X POST -uadmin:Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U -d@/tmp/daexim-export.json http://localhost:8282/restconf/operations/data-export-import:schedule-export

