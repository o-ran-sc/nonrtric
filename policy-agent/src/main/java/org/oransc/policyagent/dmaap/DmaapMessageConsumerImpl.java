package org.oransc.policyagent.dmaap;

import java.util.Properties;

import org.onap.dmaap.mr.client.impl.MRConsumerImpl;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DmaapMessageConsumerImpl implements DmaapMessageConsumer {

    private final ApplicationConfig applicationConfig;

    protected MRConsumerImpl consumer;

    @Autowired
    public DmaapMessageConsumerImpl(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub

    }

    @Override
    public void init(Properties baseProperties) {
        Properties dmaapConsumerConfig = applicationConfig.getDmaapConsumerConfig();
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
