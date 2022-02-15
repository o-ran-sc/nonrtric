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

package messages

type ORanDuRestConf struct {
	DistributedUnitFunction []DistributedUnitFunction `json:"o-ran-sc-du-hello-world:distributed-unit-functions"`
}

type DistributedUnitFunction struct {
	Id               string           `json:"id"`
	OperationalState string           `json:"operational-state"`
	AdmState         string           `json:"administrative-state"`
	UserLabel        string           `json:"user-label"`
	RRMPolicyRatio   []RRMPolicyRatio `json:"radio-resource-management-policy-ratio"`
	Cell             []Cell           `json:"cell"`
}

type RRMPolicyRatio struct {
	Id                      string            `json:"id"`
	AdmState                string            `json:"administrative-state"`
	UserLabel               string            `json:"user-label"`
	RRMPolicyMaxRatio       int               `json:"radio-resource-management-policy-max-ratio"`
	RRMPolicyMinRatio       int               `json:"radio-resource-management-policy-min-ratio"`
	RRMPolicyDedicatedRatio int               `json:"radio-resource-management-policy-dedicated-ratio"`
	ResourceType            string            `json:"resource-type"`
	RRMPolicyMembers        []RRMPolicyMember `json:"radio-resource-management-policy-members"`
}

type RRMPolicyMember struct {
	MobileCountryCode   string `json:"mobile-country-code"`
	MobileNetworkCode   string `json:"mobile-network-code"`
	SliceDifferentiator int    `json:"slice-differentiator"`
	SliceServiceType    int    `json:"slice-service-type"`
}

type Cell struct {
	Id                                  string                              `json:"id"`
	LocalId                             int                                 `json:"local-id"`
	PhysicalCellId                      int                                 `json:"physical-cell-id"`
	BaseStationChannelBandwidth         BaseStationChannelBandwidth         `json:"base-station-channel-bandwidth"`
	OperationalState                    string                              `json:"operational-state"`
	TrackingAreaCode                    int                                 `json:"tracking-area-code"`
	AdmState                            string                              `json:"administrative-state"`
	PublicLandMobileNetworks            []PublicLandMobileNetworks          `json:"public-land-mobile-networks"`
	SupportedMeasurements               []SupportedMeasurements             `json:"supported-measurements"`
	TrafficState                        string                              `json:"traffic-state"`
	AbsoluteRadioFrequencyChannelNumber AbsoluteRadioFrequencyChannelNumber `json:"absolute-radio-frequency-channel-number"`
	UserLabel                           string                              `json:"user-label"`
	SynchronizationSignalBlock          SynchronizationSignalBlock          `json:"synchronization-signal-block"`
}

type BaseStationChannelBandwidth struct {
	Uplink              int `json:"uplink"`
	Downlink            int `json:"downlink"`
	SupplementaryUplink int `json:"supplementary-uplink"`
}

type PublicLandMobileNetworks struct {
	SliceDifferentiator int    `json:"slice-differentiator"`
	SliceServiceType    int    `json:"slice-service-type"`
	MobileCountryCode   string `json:"mobile-country-code"`
	MobileNetworkCode   string `json:"mobile-network-code"`
}

type SupportedMeasurements struct {
	PerformanceMeasurementType         string                               `json:"performance-measurement-type"`
	SupportedSnssaiSubcounterInstances []SupportedSnssaiSubcounterInstances `json:"supported-snssai-subcounter-instances"`
}

type SupportedSnssaiSubcounterInstances struct {
	SliceDifferentiator int `json:"slice-differentiator"`
	SliceServiceType    int `json:"slice-service-type"`
}

type AbsoluteRadioFrequencyChannelNumber struct {
	Uplink              int `json:"uplink"`
	Downlink            int `json:"downlink"`
	SupplementaryUplink int `json:"supplementary-uplink"`
}

type SynchronizationSignalBlock struct {
	Duration               int `json:"duration"`
	FrequencyChannelNumber int `json:"frequency-channel-number"`
	Periodicity            int `json:"periodicity"`
	SubcarrierSpacing      int `json:"subcarrier-spacing"`
	Offset                 int `json:"offset"`
}
