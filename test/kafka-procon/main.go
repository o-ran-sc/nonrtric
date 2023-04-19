// -
//   ========================LICENSE_START=================================
//   O-RAN-SC
//   %%
//   Copyright (C) 2020-2022: Nordix Foundation
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

// Writing a basic HTTP server is easy using the
// `net/http` package.
package main

import (
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"strconv"
	"sync/atomic"
	"time"

	"github.com/confluentinc/confluent-kafka-go/kafka"
	"github.com/enriquebris/goconcurrentqueue"
	"github.com/gorilla/mux"
)

// Note: consumer 'group' and consumer 'user' both uses hardcoded values specific to this interface
//    globalCounters      var holding the "global counters"
//      received          number of received messages from all topics                             (int)
//      sent              number of sent messages to all topics                                   (int)
//    topics              var holding all topic related info
//      <topic-name>      name of a topic (present after topic is created)
//        content-type    data type of the topic                                                  (string)
//        counters
//          received      number of received messages from the topic                              (int)
//          sent          number of sent messages to the topic                                    (int)
//        messages
//          send          messages waiting to be sent (set when sending is started)               (fifo)
//          received      received messages waiting to be fetched (set when reception is started) (fifo)

type counter struct {
	c uint64
}

func (c *counter) step() {
	atomic.AddUint64(&c.c, 1)
}

func (c counter) get() uint64 {
	return atomic.LoadUint64(&c.c)
}

type counters struct {
	received counter
	sent     counter
}

func newCounters() counters {
	return counters{
		received: counter{},
		sent:     counter{},
	}
}

type messages struct {
	send     *goconcurrentqueue.FIFO
	received *goconcurrentqueue.FIFO
}

func (m *messages) startSend() bool {
	if m.send == nil {
		m.send = goconcurrentqueue.NewFIFO()
		return true
	}
	return false
}

func (m *messages) stopSend() {
	m.send = nil
}

func (m *messages) addToSend(msg string) error {
	if m.send == nil {
		return fmt.Errorf("sending not started")
	}
	m.send.Lock()
	defer m.send.Unlock()
	return m.send.Enqueue(msg)
}

func (m *messages) getToSend() (interface{}, error) {
	if m.send == nil {
		return "", fmt.Errorf("sending not started")
	}
	m.send.Lock()
	defer m.send.Unlock()
	return m.send.Dequeue()
}

func (m *messages) startReceive() bool {
	if m.received == nil {
		m.received = goconcurrentqueue.NewFIFO()
		return true
	}
	return false
}

func (m *messages) stopReceive() {
	m.send = nil
}

type topic struct {
	contentType string
	counters    counters
	messages    messages
}

func newTopic(ct string) *topic {
	return &topic{
		contentType: ct,
		counters:    counters{},
		messages:    messages{},
	}
}

var globalCounters counters
var topics map[string]*topic = make(map[string]*topic)

var bootstrapserver = ""

func initApp() {
	bootstrapserver = os.Getenv("KAFKA_BOOTSTRAP_SERVER")
	if len(bootstrapserver) == 0 {
		fmt.Println("Fatal error: env var KAFKA_BOOTSTRAP_SERVER not set")
		fmt.Println("Exiting... ")
		os.Exit(1)
	}
	fmt.Println("Using KAFKA_BOOTSTRAP_SERVER=" + bootstrapserver)
}

//Helper function to get a created topic, if it exists
func getTopicFromRequest(w http.ResponseWriter, req *http.Request) (*topic, string, bool) {
	topicId := mux.Vars(req)["topic"]
	t, exist := topics[topicId]
	if exist == false {
		w.WriteHeader(http.StatusNotFound)
		fmt.Fprintf(w, "Topic %v does not exist", topicId)
		return nil, "", false
	}
	return t, topicId, true
}

// Alive check
// GET on /
func healthCheck(w http.ResponseWriter, req *http.Request) {
	fmt.Fprintf(w, "OK")
}

// Deep reset of this interface stub - no removal of msgs or topics in kafka
// POST on /reset
func allreset(w http.ResponseWriter, req *http.Request) {
	for _, v := range topics {
		v.messages.stopSend()
		v.messages.stopReceive()
	}
	time.Sleep(5 * time.Second) //Allow producers/consumers to shut down
	globalCounters = newCounters()
	topics = make(map[string]*topic)
	fmt.Fprintf(w, "OK")
}

// Get topics, return json array of strings of topics created by this interface stub
// Returns json array of strings, array is empty if no topics exist
// GET on /topics

func getTopics(w http.ResponseWriter, req *http.Request) {
	topicKeys := make([]string, 0, len(topics))
	fmt.Printf("len topics: %v\n", len(topics))
	for k := range topics {
		topicKeys = append(topicKeys, k)
	}
	var j, err = json.Marshal(topicKeys)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Cannot convert list of topics to json, error details: %v", err)
		return
	} else {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		w.Write(j)
	}
}

func writeOkRepsonse(w http.ResponseWriter, httpStatus int, msg string) {
	w.WriteHeader(httpStatus)
	w.Header().Set("Content-Type", "text/plain")
	fmt.Fprintf(w, msg)
}

// Get a counter value
// GET /topics/counters/{counter}
func getCounter(w http.ResponseWriter, req *http.Request) {
	ctr := mux.Vars(req)["counter"]
	var ctrvalue = -1
	if ctr == "received" {
		ctrvalue = int(globalCounters.received.get())
	} else if ctr == "sent" {
		ctrvalue = int(globalCounters.sent.get())
	}

	if ctrvalue == -1 {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Counter %v does not exist", ctr)
		return
	}
	writeOkRepsonse(w, http.StatusOK, strconv.Itoa(ctrvalue))
	return

}

// Create a topic
// PUT on /topics/<topic>?type=<type>    type shall be 'application/json' or 'text/plain'
func createTopic(w http.ResponseWriter, req *http.Request) {
	topicId := mux.Vars(req)["topic"]
	topicType := mux.Vars(req)["type"]

	fmt.Printf("Creating topic: %v, content type: %v\n", topicId, topicType)

	if len(topicType) == 0 {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Type not specified")
		return
	}

	tid, exist := topics[topicId]
	if exist == true {
		if tid.contentType != topicType {
			w.WriteHeader(http.StatusBadRequest)
			fmt.Fprintf(w, "Topic type exist but type is different, queue content type: %v, requested content type: %v", tid.contentType, topicType)
			return
		}
		writeOkRepsonse(w, http.StatusOK, "Topic exist")
		return
	}

	t := newTopic(topicType)

	a, err := kafka.NewAdminClient(&kafka.ConfigMap{"bootstrap.servers": bootstrapserver})
	defer func() { a.Close() }()
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Cannot create client to bootstrap server: "+bootstrapserver+", error details: %v", err)
		return
	}

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	maxDur := 10 * time.Second

	_, err = a.CreateTopics(
		ctx,
		[]kafka.TopicSpecification{{
			Topic:             topicId,
			NumPartitions:     1,
			ReplicationFactor: 1}},
		kafka.SetAdminOperationTimeout(maxDur))

	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "Failed to create topic: %v, error details: %v", topicId, err)
		return
	}
	topics[topicId] = t
	w.WriteHeader(http.StatusCreated)
	fmt.Fprintf(w, "Topic created")
}

// Get topic type
// GET on /topic/<topic>
func getTopic(w http.ResponseWriter, req *http.Request) {
	t, _, exist := getTopicFromRequest(w, req)
	if !exist {
		return
	}
	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, t.contentType)
}

// Get a topics counter value
// GET /topics/{topic}/counters/{counter}
func getTopicCounter(w http.ResponseWriter, req *http.Request) {
	t, _, exist := getTopicFromRequest(w, req)
	if !exist {
		return
	}
	ctr := mux.Vars(req)["counter"]

	var ctrvalue = -1
	if ctr == "received" {
		ctrvalue = int(t.counters.received.get())
	} else if ctr == "sent" {
		ctrvalue = int(t.counters.sent.get())
	}

	if ctrvalue == -1 {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Counter %v does not exist", ctr)
		return
	}
	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, strconv.Itoa(ctrvalue))
	return
}

func startToSend(w http.ResponseWriter, req *http.Request) {
	t, topicId, exist := getTopicFromRequest(w, req)
	fmt.Printf("Start to send to topic: %v\n", topicId)
	if !exist {
		return
	}

	if !t.messages.startSend() {
		w.WriteHeader(http.StatusOK)
		fmt.Fprintf(w, "Already started sending")
		return
	}
	go func() {
		p, err := kafka.NewProducer(&kafka.ConfigMap{"bootstrap.servers": bootstrapserver})
		if err != nil {
			fmt.Printf("Cannot create producer for topic: %v, error details: %v\n", topicId, err)
			return
		}
		defer func() {
			fmt.Printf("Closing producer for topic: %v\n", topicId)
			p.Close()
		}()
		for {
			q := t.messages.send
			if q == nil {
				return
			}
			m, err := q.Get(0)
			if err == nil {
				err = p.Produce(&kafka.Message{
					TopicPartition: kafka.TopicPartition{Topic: &topicId, Partition: kafka.PartitionAny},
					Value:          []byte(fmt.Sprintf("%v", m)),
				}, nil)
				if err == nil {
					q.Remove(0)
					t.counters.sent.step()
					globalCounters.sent.step()
					msg := fmt.Sprintf("%v", m)
					if len(msg) < 500 {
						fmt.Printf("Message sent on topic: %v, len: %v, msg: %v\n", topicId, len(msg), msg)
					} else {
						fmt.Printf("Message sent on topic: %v, len: %v, is larger than 500...not printed\n", topicId, len(msg))
					}
				} else {
					fmt.Printf("Failed to send message on topic: %v. Discarded. Error details: %v\n", topicId, err)
					q.Remove(0)
				}
			} else {
				time.Sleep(10 * time.Millisecond)
			}
		}
	}()

	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "Sending started")
}

func startToReceive(w http.ResponseWriter, req *http.Request) {
	t, topicId, exist := getTopicFromRequest(w, req)
	if !exist {
		return
	}

	if !t.messages.startReceive() {
		w.WriteHeader(http.StatusOK)
		fmt.Fprintf(w, "Already started receiving")
		return
	}

	go func() {

		defer func() { t.messages.stopReceive() }()

		groudId := "kafkaprocon"

		c, err := kafka.NewConsumer(&kafka.ConfigMap{
			"bootstrap.servers":       bootstrapserver,
			"group.id":                groudId,
			"auto.offset.reset":       "earliest",
			"enable.auto.commit":      true,
			"auto.commit.interval.ms": 5000,
		})
		if err != nil {
			fmt.Printf("Cannot create consumer for topic: %v, error details: %v\n", topicId, err)
			t.messages.stopReceive()
			return
		}
		c.Commit()
		defer func() { c.Close() }()
		for {
			que := t.messages.received
			if que == nil {
				fmt.Printf("Cannot start receiving on topic: %v, queue does not exist\n", topicId)
				return
			}
			fmt.Printf("Start subscribing on topic: %v\n", topicId)
			err = c.SubscribeTopics([]string{topicId}, nil)
			if err != nil {
				fmt.Printf("Cannot start subscribing on topic: %v, error details: %v\n", topicId, err)
				return
			}
			maxDur := 1 * time.Second
			for {
				msg, err := c.ReadMessage(maxDur)
				if err == nil {
					if len(msg.Value) < 500 {
						fmt.Printf("Message received on topic: %v, partion: %v, len: %v, msg: %v\n", topicId, msg.TopicPartition, len(msg.Value), string(msg.Value))
					} else {
						fmt.Printf("Message received on topic: %v, partion: %v, len: %v is larger than 500...not printed\n", topicId, msg.TopicPartition, len(msg.Value))
					}
					err = t.messages.received.Enqueue(string(msg.Value))
					if err != nil {
						w.WriteHeader(http.StatusInternalServerError)
						fmt.Fprintf(w, "Received message topic: %v, cannot be put in queue, %v", topicId, err)
						return
					}
					t.counters.received.step()
					globalCounters.received.step()
				} else {
					fmt.Printf("Nothing to consume on topic: %v, reason: %v\n", topicId, err)
				}
			}
		}
	}()

	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "Receiving started")
}

// Post a message to a topic
// POST /send    content type is specified in content type
func sendToTopic(w http.ResponseWriter, req *http.Request) {

	t, topicId, exist := getTopicFromRequest(w, req)
	fmt.Printf("Send to topic: %v\n", topicId)
	if !exist {
		return
	}
	q := t.messages.send
	if q == nil {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Sending not initiated on topic: %v", topicId)
		return
	}
	ct := req.Header.Get("Content-Type")
	if ct != t.contentType {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Message to send content type: %v on topic: %v does not match queue content type: %v", ct, topicId, t.contentType)
		return
	}

	if ct == "application/json" {
		// decoder := json.NewDecoder(req.Body)
		// var j :=
		// err := decoder.Decode(&j)
		// if err != nil {
		// 	w.WriteHeader(http.StatusBadRequest)
		// 	w.Header().Set("Content-Type", "text/plain")
		// 	fmt.Fprintf(w, "Json payload cannot be decoded, error details %v\n", err)
		// 	return
		// }
		//m = mux.Vars(req)[""]
		if err := req.ParseForm(); err != nil {
			w.WriteHeader(http.StatusBadRequest)
			fmt.Fprintf(w, "Json payload cannot be decoded on topic: %v, error details %v", topicId, err)
			return
		}
		b, err := ioutil.ReadAll(req.Body)
		if err == nil {
			if len(b) < 500 {
				fmt.Printf("Json payload to send on topic: %v, msg: %v\n", topicId, string(b))
			} else {
				fmt.Printf("Json payload to send on topic: %v larger than 500 bytes, not printed...\n", topicId)
			}
		} else {
			fmt.Printf("Json payload to send on topic: %v cannnot be decoded, err: %v\n", topicId, err)
		}
		err = q.Enqueue(string(b))
		if err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			fmt.Fprintf(w, "Json message to send cannot be put in queue")
			return
		}
	} else if ct == "text/plain" {
		if err := req.ParseForm(); err != nil {
			w.WriteHeader(http.StatusBadRequest)
			fmt.Fprintf(w, "Text payload to send on topic: %v cannot be decoded, error details %v\n", topicId, err)
			return
		}
		b, err := ioutil.ReadAll(req.Body)
		if err == nil {
			if len(b) < 500 {
				fmt.Printf("Text payload to send on topic: %v, msg: %v\n", topicId, string(b))
			} else {
				fmt.Printf("Text payload to send on topic: %v larger than 500 bytes, not printed...\n", topicId)
			}
		} else {
			fmt.Printf("Text payload to send on topic: %v cannnot be decoded, err: %v\n", topicId, err)
		}
		err = q.Enqueue(string(b))
		if err != nil {
			w.WriteHeader(http.StatusInternalServerError)
			fmt.Fprintf(w, "Text message to send cannot be put in queue")
			return
		}
	} else {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Message to send, unknown content type %v", ct)
		return
	}

	w.WriteHeader(http.StatusOK)
	w.Header().Set("Content-Type", "text/plain")
	fmt.Fprintf(w, "Message to send put in queue")
}

// Get zero or one message from a topic
// GET /receive
func receiveFromTopic(w http.ResponseWriter, req *http.Request) {
	t, topicId, exist := getTopicFromRequest(w, req)
	if !exist {
		return
	}
	if t.messages.received == nil {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Receiving not initiated on topic %v", topicId)
		return
	}

	m, err := t.messages.received.Dequeue()
	if err != nil {
		w.WriteHeader(http.StatusNoContent)
		return
	}

	w.Header().Set("Content-Type", t.contentType)
	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "%v", m)
}

// Remove the send queue to stop sending
func stopToSend(w http.ResponseWriter, req *http.Request) {
	fmt.Printf("Stop sending\n")
	t, _, exist := getTopicFromRequest(w, req)
	if !exist {
		return
	}
	t.messages.stopSend()
	w.WriteHeader(http.StatusNoContent)
}

// Remove the receive queue to stop receiving
func stopToReceive(w http.ResponseWriter, req *http.Request) {
	fmt.Printf("Stop receiving\n")
	t, _, exist := getTopicFromRequest(w, req)
	if !exist {
		return
	}
	t.messages.stopReceive()
	w.WriteHeader(http.StatusNoContent)
}

func HelloServer(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "Hello, %s!", r.URL.Path[1:])
}

func main() {

	initApp()

	r := mux.NewRouter()

	r.HandleFunc("/", healthCheck).Methods("GET")
	r.HandleFunc("/reset", allreset).Methods("POST")
	r.HandleFunc("/counters/{counter}", getCounter).Methods("GET")
	r.HandleFunc("/topics", getTopics).Methods("GET")
	r.HandleFunc("/topics/{topic}/counters/{counter}", getTopicCounter).Methods("GET")
	r.HandleFunc("/topics/{topic}", createTopic).Methods("PUT").Queries("type", "{type}")
	r.HandleFunc("/topics/{topic}", getTopic).Methods("GET")
	r.HandleFunc("/topics/{topic}/startsend", startToSend).Methods("POST")
	r.HandleFunc("/topics/{topic}/startreceive", startToReceive).Methods("POST")
	r.HandleFunc("/topics/{topic}/stopsend", stopToSend).Methods("POST")
	r.HandleFunc("/topics/{topic}/stopreceive", stopToReceive).Methods("POST")
	r.HandleFunc("/topics/{topic}/msg", sendToTopic).Methods("POST")
	r.HandleFunc("/topics/{topic}/msg", receiveFromTopic).Methods("GET")

	port := "8090"
	srv := &http.Server{
		Handler:      r,
		Addr:         ":" + port,
		WriteTimeout: 15 * time.Second,
		ReadTimeout:  15 * time.Second,
	}
	fmt.Println("Running on port: " + port)
	fmt.Printf(srv.ListenAndServe().Error())
}
