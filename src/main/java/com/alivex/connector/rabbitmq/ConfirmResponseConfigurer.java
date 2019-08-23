package com.alivex.connector.rabbitmq;

import java.util.concurrent.BlockingQueue;


/**
 * Require enable confirm mode on channel.
 * @author development
 */
public interface ConfirmResponseConfigurer {

    /**
     * Confirm listener will put response result into blocking queue when confirm listener invoke in future.
     * @param confirmAsyncResult
     */
    public void responseTo(BlockingQueue<Boolean> confirmAsyncResult);
}
