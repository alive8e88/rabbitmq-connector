package com.alivex.connector.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultShutdownListener implements ShutdownListener, OperationIdConfigurer, Serializable {

    private static final Logger LOG = Logger.getLogger(DefaultShutdownListener.class.getName());

    private final int REPLY_CODE_200 = 200;
    private final String REPLY_TEXT_OK = "OK";

    private String operationId;

    /**
     * https://www.rabbitmq.com/api-guide.html#shutdown-cause
     *
     * @param e
     */
    @Override
    public void shutdownCompleted(ShutdownSignalException e) {

        if (LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, "Shutdown listener invoke.");
        }

        if (isConnectionIssue(e)) {

            //Channel error
            if (!e.isHardError()) {
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.log(Level.FINER, "Shutdown signal from channel, operationId:" + operationId, e);
                }
                return;
            }

            //Connection error
            if (!e.isInitiatedByApplication()) {
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.log(Level.FINER, "Shutdown signal from connection, operationId:" + operationId, e);
                }
            }

        } else {

            if (isChannelObject(e.getReference())) {
                String connectionId = getConnectionIdFromChannel(e);
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.log(Level.FINER, "Gracefully shutdown signal from channel operationId:{0}, connectionId:{1}", new Object[]{operationId, connectionId});
                }

                return;
            }

            if (isConnectionObject(e.getReference())) {
                String connectionId = getConnectionIdFromConnection(e);
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.log(Level.FINER, "Gracefully shutdown signal from connection, connectionId:{0}", connectionId);
                }
            }
        }
    }

    @Override
    public void setOperationId(String id) {
        this.operationId = id;
    }

    private boolean isConnectionObject(Object o) {
        return o instanceof Connection;
    }

    private boolean isChannelObject(Object o) {
        return o instanceof Channel;
    }

    private boolean isConnectionIssue(ShutdownSignalException e) {
        return !isReplyCodeContain(REPLY_CODE_200, e) || !isReplyTextContain(REPLY_TEXT_OK, e);
    }

    private boolean isReplyCodeContain(int code, ShutdownSignalException e) {
        return e.getMessage().contains("reply-code=" + code);
    }

    private boolean isReplyTextContain(String text, ShutdownSignalException e) {
        return e.getMessage().contains("reply-text=" + text);
    }

    private String getConnectionIdFromChannel(ShutdownSignalException e) {
        return ((Channel) e.getReference()).getConnection().getId();
    }

    private String getConnectionIdFromConnection(ShutdownSignalException e) {
        return ((Connection) e.getReference()).getId();
    }
}
