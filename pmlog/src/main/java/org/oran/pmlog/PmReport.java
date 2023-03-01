/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2023 Nordix Foundation
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================LICENSE_END===================================
 */

package org.oran.pmlog;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.Collection;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder(toBuilder = true)
public class PmReport {

    private static com.google.gson.Gson gson = new com.google.gson.GsonBuilder() //
            .disableHtmlEscaping() //
            .create();

    public static PmReport parse(DataFromKafkaTopic data) {
        return gson.fromJson(data.valueAsString(), PmReport.class);
    }

    public long lastTimeEpochMili() {
        return event.commonEventHeader.lastEpochMicrosec / 1000;
    }

    public String fullDistinguishedName(PmReport.MeasValuesList measValueList) {
        return event.getPerf3gppFields().getMeasDataCollection().getMeasuredEntityDn() + ","
                + measValueList.getMeasObjInstId();
    }

    @Expose
    public Event event;

    public static class CommonEventHeader {
        @Expose
        private String domain;

        @Expose
        private String eventId;

        @Expose
        private String eventName;

        @Expose
        @Getter
        private String sourceName;

        @Expose
        private String reportingEntityName;

        @Expose
        private long startEpochMicrosec;

        @Expose
        @Setter
        private long lastEpochMicrosec;

        @Expose
        private String timeZoneOffset;

        /* Not reported elements */
        int sequence;
        String priority;
        String version;
        String vesEventListenerVersion;
    }

    public static class MeasInfoId {
        @Expose
        private String sMeasInfoId = "";
    }

    public static class MeasTypes {
        public String getMeasType(int pValue) {
            if (pValue > sMeasTypesList.size()) {
                return "MeasTypeIndexOutOfBounds:" + pValue;
            }
            return sMeasTypesList.get(pValue - 1);
        }

        @Expose
        protected ArrayList<String> sMeasTypesList = new ArrayList<>();
    }

    @Getter
    @Builder(toBuilder = true)
    public static class MeasResult {
        @Expose
        @Setter
        private int p;

        @Expose
        @Setter
        private String sValue;
    }

    @Builder(toBuilder = true)
    public static class MeasValuesList {
        @Expose
        @Getter
        private String measObjInstId;

        @Expose
        @Getter
        private String suspectFlag;

        @Expose
        @Getter
        private Collection<MeasResult> measResults;

        public boolean isEmpty() {
            return this.measResults.isEmpty();
        }

        static MeasValuesList emptyList = MeasValuesList.builder().measResults(new ArrayList<>()).build();

        public static MeasValuesList empty() {
            return emptyList;
        }
    }

    @Getter
    @Builder(toBuilder = true)
    public static class MeasInfoList {
        @Expose
        private MeasInfoId measInfoId;

        @Expose
        private MeasTypes measTypes;

        @Expose
        private Collection<MeasValuesList> measValuesList;

    }

    @Builder(toBuilder = true)
    @Getter
    public static class MeasDataCollection {
        @Expose
        private int granularityPeriod;

        @Expose
        private String measuredEntityUserName;

        @Expose
        private String measuredEntityDn;

        @Expose
        private String measuredEntitySoftwareVersion;

        @Expose
        private Collection<MeasInfoList> measInfoList;
    }

    @Builder(toBuilder = true)
    @Getter
    public static class Perf3gppFields {
        @Expose
        private String perf3gppFieldsVersion;

        @Expose
        private MeasDataCollection measDataCollection;
    }

    @Getter
    @Builder(toBuilder = true)
    public static class Event {
        @Expose
        private CommonEventHeader commonEventHeader;

        @Expose
        private Perf3gppFields perf3gppFields;
    }

}
