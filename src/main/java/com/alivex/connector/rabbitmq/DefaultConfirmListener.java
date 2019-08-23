
package com.alivex.connector.rabbitmq;

import com.rabbitmq.client.ConfirmListener;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Require enable confirm mode on channel.
 */
public class DefaultConfirmListener implements ConfirmListener, ConfirmResponseConfigurer, OperationIdConfigurer, Serializable {

    private static final Logger LOG = Logger.getLogger(DefaultConfirmListener.class.getName());
    private BlockingQueue<Boolean> confirmAsyncResult;
    private String operatioId;

    @Override
    public void handleAck(long arg0, boolean arg1) throws IOException {
        try {

            if (LOG.isLoggable(Level.FINER)) {
                LOG.log(Level.FINER, "Confirm listener invoke.");
                LOG.log(Level.FINER, "Successfully to publish data, operationId:{0}", operatioId);
            }

            confirmAsyncResult.add(true);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to confirm publish", e);
            throw e;
        }
    }

    @Override
    public void handleNack(long arg0, boolean arg1) throws IOException {
        try {

            LOG.log(Level.FINER, "Confirm listener invoke.");
            LOG.log(Level.FINER, "Failed to publish, operationId:{0}", operatioId);
            confirmAsyncResult.add(false);

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to nack confirm publish", e);
            throw e;
        }
    }

    @Override
    public void responseTo(BlockingQueue<Boolean> confirmAsyncResult) {
        this.confirmAsyncResult = confirmAsyncResult;
    }

    @Override
    public void setOperationId(String id) {
        this.operatioId = id;
    }
}
