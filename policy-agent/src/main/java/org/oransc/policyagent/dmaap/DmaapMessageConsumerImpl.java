package org.oransc.policyagent.dmaap;

import java.util.Properties;

import org.onap.dmaap.mr.client.impl.MRConsumerImpl;

public class DmaapMessageConsumerImpl implements DmaapMessageConsumer {

	protected MRConsumerImpl consumer;
	
	public DmaapMessageConsumerImpl() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(Properties baseProperties) {
		// Initialize the DMAAP with the properties
		// TODO Auto-generated method stub

	}

	@Override
	public void processMsg(String msg) throws Exception {
		// Call the Controller once you get the message from DMAAP
		// Call the concurrent Task executor to handle the incoming request
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isReady() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRunning() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void stopConsumer() {
		// TODO Auto-generated method stub

	}

}
