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

package kafkaconsumer

import (
	"time"

	"github.com/confluentinc/confluent-kafka-go/kafka"
)

type KafkaFactory interface {
	NewKafkaConsumer(topicID string) (KafkaConsumer, error)
}

type KafkaFactoryImpl struct {
	BootstrapServer string
}

func (kf KafkaFactoryImpl) NewKafkaConsumer(topicID string) (KafkaConsumer, error) {
	c, err := kafka.NewConsumer(&kafka.ConfigMap{
		"bootstrap.servers": kf.BootstrapServer,
		"group.id":          "dmaap-mediator-producer",
		"auto.offset.reset": "earliest",
	})
	if err != nil {
		return KafkaConsumerImpl{}, err
	}
	return KafkaConsumerImpl{
		consumer: c,
	}, nil
}

type KafkaConsumer interface {
	Commit() ([]kafka.TopicPartition, error)
	Subscribe(topic string) (err error)
	ReadMessage(timeout time.Duration) (*kafka.Message, error)
}

type KafkaConsumerImpl struct {
	consumer *kafka.Consumer
}

func (kc KafkaConsumerImpl) Commit() ([]kafka.TopicPartition, error) {
	return kc.consumer.Commit()
}

func (kc KafkaConsumerImpl) Subscribe(topic string) error {
	return kc.consumer.Subscribe(topic, nil)
}

func (kc KafkaConsumerImpl) ReadMessage(timeout time.Duration) (*kafka.Message, error) {
	return kc.consumer.ReadMessage(timeout)
}
