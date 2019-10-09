#!/bin/bash
cat > /tmp/daexim-import.json <<-END
{ 
   "input" : { 
     "check-models" : true, 
     "clear-stores" : "all" 
   } 
}
END

curl -v -H "Content-Type: application/json" -X POST -uadmin:Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U -d@/tmp/daexim-import.json http://localhost:8080/restconf/operations/data-export-import:immediate-import
