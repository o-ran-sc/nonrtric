package org.oransc.policyagent.dmaap;

import org.springframework.context.annotation.Configuration;

@Configuration("dmaap")
public class BusTopicParams {

	private int port;
	private String server;
	private String topic;
	private String consumerGroup;
	private String consumerInstance;
	private int fetchTimeout;
	private int fetchLimit;
	private String userName;
	private String password;
}