package org.oransc.policyagent.dmaap;

import java.util.Properties;

/**
 * The Dmaap consumer which has the base methods to be implemented by any class
 * which implements this interface
 *
 */
public interface DmaapMessageConsumer extends Runnable {

    public void init(Properties baseProperties);

    public abstract void processMsg(String msg) throws Exception;

    public boolean isReady();

    public boolean isRunning();

    public void stopConsumer();

}
