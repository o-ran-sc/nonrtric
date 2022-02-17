// -
//   ========================LICENSE_START=================================
//   O-RAN-SC
//   %%
//   Copyright (C) 2021: Nordix Foundation
//   %%
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//   ========================LICENSE_END===================================
//

package main

import (
	"encoding/csv"
	"encoding/json"
	"flag"
	"fmt"
	"math/rand"
	"net/http"
	"os"
	"strconv"
	"sync"
	"time"

	"github.com/gorilla/mux"
	"oransc.org/usecase/oduclosedloop/messages"

	log "github.com/sirupsen/logrus"
)

const THRESHOLD_TPUT int = 7000

type SliceAssuranceInformation struct {
	duId                 string
	cellId               string
	sd                   int
	sst                  int
	metricName           string
	metricValue          int
	policyRatioId        string
	policyMaxRatio       int
	policyMinRatio       int
	policyDedicatedRatio int
}

var data []*SliceAssuranceInformation
var messagesToSend []messages.Measurement

func loadData() {
	lines, err := GetCsvFromFile("test-data.csv")
	if err != nil {
		panic(err)
	}
	for _, line := range lines {
		sai := SliceAssuranceInformation{
			duId:                 line[0],
			cellId:               line[1],
			sd:                   toInt(line[2]),
			sst:                  toInt(line[3]),
			metricName:           line[4],
			metricValue:          toInt(line[5]),
			policyRatioId:        line[6],
			policyMaxRatio:       toInt(line[7]),
			policyMinRatio:       toInt(line[8]),
			policyDedicatedRatio: toInt(line[9]),
		}
		data = append(data, &sai)
	}
}

func GetCsvFromFile(name string) ([][]string, error) {
	if csvFile, err := os.Open(name); err == nil {
		defer csvFile.Close()
		reader := csv.NewReader(csvFile)
		reader.FieldsPerRecord = -1
		if csvData, err := reader.ReadAll(); err == nil {
			return csvData, nil
		} else {
			return nil, err
		}
	} else {
		return nil, err
	}
}

func toInt(num string) int {
	res, err := strconv.Atoi(num)
	if err != nil {
		return -1
	}
	return res
}

func main() {
	rand.Seed(time.Now().UnixNano())

	portSdnr := flag.Int("sdnr-port", 3904, "The port this SDNR stub will listen on")
	portDmaapMR := flag.Int("dmaap-port", 3905, "The port this Dmaap message router will listen on")
	flag.Parse()

	loadData()

	wg := new(sync.WaitGroup)
	wg.Add(2)

	go func() {

		r := mux.NewRouter()
		r.HandleFunc("/rests/data/network-topology:network-topology/topology=topology-netconf/node={NODE-ID}/yang-ext:mount/o-ran-sc-du-hello-world:network-function/distributed-unit-functions={O-DU-ID}", getSdnrResponseMessage).Methods(http.MethodGet)
		r.HandleFunc("/rests/data/network-topology:network-topology/topology=topology-netconf/node={NODE-ID}/yang-ext:mount/o-ran-sc-du-hello-world:network-function/distributed-unit-functions={O-DU-ID}/radio-resource-management-policy-ratio={POLICY-ID}", updateRRMPolicyDedicatedRatio).Methods(http.MethodPut)

		fmt.Println("Starting SDNR stub on port: ", *portSdnr)

		log.Fatal(http.ListenAndServe(fmt.Sprintf(":%v", *portSdnr), r))
		wg.Done()
	}()

	go func() {

		r := mux.NewRouter()
		r.HandleFunc("/events/unauthenticated.VES_O_RAN_SC_HELLO_WORLD_PM_STREAMING_OUTPUT/myG/C1", sendDmaapMRMessages).Methods(http.MethodGet)

		fmt.Println("Starting DmaapMR stub on port: ", *portDmaapMR)

		log.Fatal(http.ListenAndServe(fmt.Sprintf(":%v", *portDmaapMR), r))
		wg.Done()
	}()

	wg.Wait()
}

func getSdnrResponseMessage(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	log.Info("Get messages for RRM Policy Ratio information for O-Du ID ", vars["O-DU-ID"])

	distUnitFunctions := getDistributedUnitFunctionMessage(vars["O-DU-ID"])

	respondWithJSON(w, http.StatusOK, distUnitFunctions)
}

func getDistributedUnitFunctionMessage(oduId string) messages.ORanDuRestConf {

	var policies []messages.RRMPolicyRatio
	keys := make(map[string]bool)
	for _, entry := range data {
		if _, value := keys[entry.policyRatioId]; !value {
			keys[entry.policyRatioId] = true
			message := messages.RRMPolicyRatio{

				Id:                      entry.policyRatioId,
				AdmState:                "locked",
				UserLabel:               entry.policyRatioId,
				RRMPolicyMaxRatio:       entry.policyMaxRatio,
				RRMPolicyMinRatio:       entry.policyMinRatio,
				RRMPolicyDedicatedRatio: entry.policyDedicatedRatio,
				ResourceType:            "prb",
				RRMPolicyMembers: []messages.RRMPolicyMember{
					{
						MobileCountryCode:   "310",
						MobileNetworkCode:   "150",
						SliceDifferentiator: entry.sd,
						SliceServiceType:    entry.sst,
					},
				},
			}
			policies = append(policies, message)
		}
	}

	var publicLandMobileNetworks []messages.PublicLandMobileNetworks
	for _, entry := range data {
		publicLandMobileNetwork := messages.PublicLandMobileNetworks{
			MobileCountryCode:   "310",
			MobileNetworkCode:   "150",
			SliceDifferentiator: entry.sd,
			SliceServiceType:    entry.sst,
		}
		publicLandMobileNetworks = append(publicLandMobileNetworks, publicLandMobileNetwork)
	}

	var supportedSnssaiSubcounterInstances []messages.SupportedSnssaiSubcounterInstances
	for _, entry := range data {
		supportedSnssaiSubcounterInstance := messages.SupportedSnssaiSubcounterInstances{
			SliceDifferentiator: entry.sd,
			SliceServiceType:    entry.sst,
		}
		supportedSnssaiSubcounterInstances = append(supportedSnssaiSubcounterInstances, supportedSnssaiSubcounterInstance)
	}

	cell := messages.Cell{
		Id:             "cell-1",
		LocalId:        1,
		PhysicalCellId: 1,
		BaseStationChannelBandwidth: messages.BaseStationChannelBandwidth{
			Uplink:              83000,
			Downlink:            80000,
			SupplementaryUplink: 84000,
		},
		OperationalState:         "enabled",
		TrackingAreaCode:         10,
		AdmState:                 "unlocked",
		PublicLandMobileNetworks: publicLandMobileNetworks,
		SupportedMeasurements: []messages.SupportedMeasurements{
			{
				PerformanceMeasurementType:         "o-ran-sc-du-hello-world:user-equipment-average-throughput-uplink",
				SupportedSnssaiSubcounterInstances: supportedSnssaiSubcounterInstances,
			},
			{
				PerformanceMeasurementType:         "o-ran-sc-du-hello-world:user-equipment-average-throughput-downlink",
				SupportedSnssaiSubcounterInstances: supportedSnssaiSubcounterInstances,
			},
		},
		TrafficState: "active",
		AbsoluteRadioFrequencyChannelNumber: messages.AbsoluteRadioFrequencyChannelNumber{
			Uplink:              14000,
			Downlink:            15000,
			SupplementaryUplink: 14500,
		},
		UserLabel: "cell-1",
		SynchronizationSignalBlock: messages.SynchronizationSignalBlock{
			Duration:               2,
			FrequencyChannelNumber: 12,
			Periodicity:            10,
			SubcarrierSpacing:      30,
			Offset:                 3,
		},
	}

	distUnitFunction := messages.DistributedUnitFunction{
		Id:               oduId,
		OperationalState: "enabled",
		AdmState:         "unlocked",
		UserLabel:        oduId,
		Cell: []messages.Cell{
			cell,
		},
		RRMPolicyRatio: policies,
	}

	duRRMPolicyRatio := messages.ORanDuRestConf{
		DistributedUnitFunction: []messages.DistributedUnitFunction{
			distUnitFunction,
		},
	}

	return duRRMPolicyRatio
}

func updateRRMPolicyDedicatedRatio(w http.ResponseWriter, r *http.Request) {
	var policies struct {
		RRMPolicies []messages.RRMPolicyRatio `json:"radio-resource-management-policy-ratio"`
	}
	decoder := json.NewDecoder(r.Body)

	if err := decoder.Decode(&policies); err != nil {
		respondWithError(w, http.StatusBadRequest, "Invalid request payload")
		return
	}
	defer r.Body.Close()

	prMessages := policies.RRMPolicies
	log.Infof("Post request to update RRMPolicyDedicatedRatio %+v", prMessages)
	findAndUpdatePolicy(prMessages)
	respondWithJSON(w, http.StatusOK, map[string]string{"status": "200"})
}

func findAndUpdatePolicy(rRMPolicyRatio []messages.RRMPolicyRatio) {
	for _, policy := range rRMPolicyRatio {
		for _, entry := range data {
			if entry.policyRatioId == policy.Id {
				log.Infof("update Policy Dedicated Ratio: value for policy %+v\n Old value: %v New value: %v ", policy, entry.policyDedicatedRatio, policy.RRMPolicyDedicatedRatio)
				entry.policyDedicatedRatio = policy.RRMPolicyDedicatedRatio
				if entry.metricValue > THRESHOLD_TPUT {
					entry.metricValue = rand.Intn(THRESHOLD_TPUT)
				}
				messagesToSend = append(messagesToSend, generateMeasurementEntry(entry))
			}
		}
	}
}

func respondWithError(w http.ResponseWriter, code int, message string) {
	respondWithJSON(w, code, map[string]string{"error": message})
}

func respondWithJSON(w http.ResponseWriter, code int, payload interface{}) {
	response, _ := json.Marshal(payload)

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)
	w.Write(response)
}

func sendDmaapMRMessages(w http.ResponseWriter, r *http.Request) {
	log.Info("Send Dmaap messages")
	entry := data[rand.Intn(5)]

	maxTput := THRESHOLD_TPUT + 100
	randomTput := rand.Intn(maxTput-THRESHOLD_TPUT+1) + THRESHOLD_TPUT
	if randomTput%3 == 0 {
		log.Info("Using tput value higher than THRESHOLD_TPUT ", randomTput)
		entry.metricValue = randomTput
	}
	randomEventId := rand.Intn(10000)
	messagesToSend = append(messagesToSend, generateMeasurementEntry(entry))

	message := messages.StdDefinedMessage{
		Event: messages.Event{
			CommonEventHeader: messages.CommonEventHeader{
				Domain:                  "stndDefined",
				EventId:                 "pm-1_16442" + strconv.Itoa(randomEventId),
				EventName:               "stndDefined_performanceMeasurementStreaming",
				EventType:               "performanceMeasurementStreaming",
				Sequence:                825,
				Priority:                "Low",
				ReportingEntityId:       "",
				ReportingEntityName:     "O-DU-1122",
				SourceId:                "",
				SourceName:              "O-DU-1122",
				StartEpochMicrosec:      1644252450000000,
				LastEpochMicrosec:       1644252480000000,
				NfNamingCode:            "SIM-O-DU",
				NfVendorName:            "O-RAN-SC SIM Project",
				StndDefinedNamespace:    "o-ran-sc-du-hello-world-pm-streaming-oas3",
				TimeZoneOffset:          "+00:00",
				Version:                 "4.1",
				VesEventListenerVersion: "7.2.1",
			},
			StndDefinedFields: messages.StndDefinedFields{
				StndDefinedFieldsVersion: "1.0",
				SchemaReference:          "https://gerrit.o-ran-sc.org/r/gitweb?p=scp/oam/modeling.git;a=blob_plain;f=data-model/oas3/experimental/o-ran-sc-du-hello-world-oas3.json;hb=refs/heads/master",
				Data: messages.Data{
					DataId:              "pm-1_1644252450",
					StartTime:           "2022-02-07T16:47:30.0Z",
					AdministrativeState: "unlocked",
					OperationalState:    "enabled",
					UserLabel:           "pm",
					JobTag:              "my-job-tag",
					GranularityPeriod:   30,
					Measurements:        messagesToSend,
				},
			},
		},
	}

	fmt.Printf("Sending Dmaap message:\n %+v\n", message)

	messageAsByteArray, _ := json.Marshal(message)
	response := [1]string{string(messageAsByteArray)}

	time.Sleep(time.Duration(rand.Intn(3)) * time.Second)
	respondWithJSON(w, http.StatusOK, response)

	messagesToSend = nil
}

func generateMeasurementEntry(entry *SliceAssuranceInformation) messages.Measurement {

	measurementTypeInstanceReference := "/o-ran-sc-du-hello-world:network-function/distributed-unit-functions[id='" + entry.duId + "']/cell[id='" + entry.cellId + "']/supported-measurements[performance-measurement-type='(urn:o-ran-sc:yang:o-ran-sc-du-hello-world?revision=2021-11-23)" + entry.metricName + "']/supported-snssai-subcounter-instances[slice-differentiator='" + strconv.Itoa(entry.sd) + "'][slice-service-type='" + strconv.Itoa(entry.sst) + "']"
	meas := messages.Measurement{

		MeasurementTypeInstanceReference: measurementTypeInstanceReference,
		Value:                            entry.metricValue,
		Unit:                             "kbit/s",
	}
	return meas
}
