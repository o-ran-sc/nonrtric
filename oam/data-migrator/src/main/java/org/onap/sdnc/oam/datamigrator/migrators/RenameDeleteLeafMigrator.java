/*
 * ============LICENSE_START=======================================================
 * ONAP : SDNC
 * ================================================================================
 * Copyright 2019 AMDOCS
 *=================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.sdnc.oam.datamigrator.migrators;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import java.util.Map;
import java.util.Set;

public abstract class RenameDeleteLeafMigrator extends Migrator {

    protected static Map<String,String> renamedFields ;
    protected static Set<String> deletedFields ;

    @Override
    protected String convertData(JsonObject sourceData) {
        JsonObject target =  convert(sourceData,"");
        return  target.toString();
    }

    protected JsonObject convert(JsonObject source,String parent) {
        JsonObject target = new JsonObject();
        for (String key : source.keySet()){
            String prefixKey = StringUtils.isNotEmpty(parent) ? parent + "."+key : key;
            if(!deletedFields.contains(prefixKey)) {
                JsonElement value = source.get(key);
                if (value.isJsonPrimitive()) {
                    target.add(renamedFields.getOrDefault(prefixKey,key), value);
                } else if(value.isJsonArray()){
                    JsonArray targetList = new JsonArray();
                    JsonArray sourceArray = value.getAsJsonArray();
                    for(JsonElement  e : sourceArray){
                         targetList.add(convert(e.getAsJsonObject(),prefixKey));
                    }
                    target.add(renamedFields.getOrDefault(prefixKey,key), targetList);
                } else{
                    target.add(renamedFields.getOrDefault(prefixKey,key), convert(value.getAsJsonObject(),prefixKey));
                }
            }
        }
        return target;
    }
}
