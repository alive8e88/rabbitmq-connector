package com.alivex.connector.rabbitmq.publisher;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.ReturnListener;
import com.rabbitmq.client.ShutdownListener;
import com.alivex.connector.rabbitmq.ConfirmResponseConfigurer;
import com.alivex.connector.rabbitmq.ConnectionIdConfigurer;
import com.alivex.connector.rabbitmq.ConnectionManager;
import com.alivex.connector.rabbitmq.DefaultConfirmListener;
import com.alivex.connector.rabbitmq.DefaultReturnListener;
import com.alivex.connector.rabbitmq.DefaultShutdownListener;
import java.io.IOException;
import java.util.UUID;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.alivex.connector.rabbitmq.OperationIdConfigurer;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

public class DefaultPublisher implements Publisher, Serializable {

    private static final Logger LOG = Logger.getLogger(DefaultPublisher.class.getName());

    private final String EXCHANGE_NAME;
    private final String ROUTING_KEY;

    private final ConnectionManager connectionManager;
    private final ShutdownListener shutdownListener;
    private final ReturnListener returnListener;
    private final ConfirmListener confirmListener;

    private String id;
    private Channel channel;
    private final BlockingQueue<Boolean> futureResult = new ArrayBlockingQueue<>(1);

    DefaultPublisher(String EXCHANGE_NAME,
                     String ROUTING_KEY,
                     ConnectionManager connectionManager,
                     ShutdownListener shutdownListener,
                     ReturnListener returnListener,
                     ConfirmListener confirmListener) {

        this.EXCHANGE_NAME = EXCHANGE_NAME;
        this.ROUTING_KEY = ROUTING_KEY;
        this.connectionManager = connectionManager;
        this.shutdownListener = shutdownListener;
        this.returnListener = returnListener;
        this.confirmListener = confirmListener;
    }

    @Override
    public boolean basicPublish(String json) {
        return publishData(json, 200);
    }
    
    @Override
    public boolean basicPublish(String json, int timeout) {
        return publishData(json, timeout);
    }

    private boolean publishData(String json, int timeout) {

        Boolean isConfirm = false;

        try {

            if (channel == null) {
                createChannel();
            }

            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Publish data:{0}", new Object[]{json});
            }

            channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, true, null, json.getBytes(StandardCharsets.UTF_8.name()));

            Boolean poll = futureResult.poll(timeout, TimeUnit.MILLISECONDS);
            if (poll != null) {
                isConfirm = poll;
            }

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to publish data to broker", e);
        }

        return isConfirm;
    }

    @Override
    public boolean isChannelOpen() {

        try {
            
            if (channel == null) {
                createChannel();
            }
            
        } catch (IOException iOException) {
            LOG.log(Level.WARNING, "Failed to open channel", iOException);
        }

        return channel != null && channel.isOpen();
    }

    @Override
    public void closeChannel() {

        closeChannel(channel);
        LOG.log(Level.INFO, "Publisher id:{0} is successfully close", id);
    }
    
    private void createChannel() throws IOException {
        channel = createChannel(futureResult, getId());
    }

    private Channel createChannel(final BlockingQueue<Boolean> futureResult, String publisherId) throws IOException {

        Channel publishChannel = connectionManager.getPublisherConnection().createChannel();

        if (shutdownListener instanceof DefaultShutdownListener) {
            setDefaultShutdownListenerToChannel(publishChannel, publisherId);
        } else {
            setUserDefineShutdownListenerToChannel(publishChannel);
        }

        if (returnListener instanceof DefaultReturnListener) {
            setDefaultReturnListenerToChannel(publishChannel, connectionManager.getPublisherConnection().getId(), publisherId);
        } else {
            setUserDefineReturnListenerToChannel(publishChannel);
        }

        if (confirmListener instanceof DefaultConfirmListener) {
            setDefaultConfirmListenerToChanel(publishChannel, futureResult, publisherId);
        } else {
            setUserDefineConfirmListenerToChanel(publishChannel);
        }

        return publishChannel;
    }

    private void setDefaultConfirmListenerToChanel(Channel channel, final BlockingQueue<Boolean> futureResult, String operatioId) {
        ((ConfirmResponseConfigurer) confirmListener).responseTo(futureResult);
        ((OperationIdConfigurer) confirmListener).setOperationId(operatioId);
        channel.addConfirmListener(confirmListener);
    }

    private void setUserDefineConfirmListenerToChanel(Channel channel) {
        channel.addConfirmListener(confirmListener);
    }

    private void setDefaultReturnListenerToChannel(Channel channel, String connectionId, String operationId) {
        ((ConnectionIdConfigurer) returnListener).setConnectionId(connectionId);
        ((OperationIdConfigurer) returnListener).setOperationId(operationId);
        channel.addReturnListener(returnListener);
    }

    private void setUserDefineReturnListenerToChannel(Channel channel) {
        channel.addReturnListener(returnListener);
    }

    private String generateRandomId() {
        return UUID.randomUUID().toString();
    }

    private void setDefaultShutdownListenerToChannel(Channel channel, String operationId) {
        ((OperationIdConfigurer) shutdownListener).setOperationId(operationId);
        channel.addShutdownListener(shutdownListener);
    }

    private void setUserDefineShutdownListenerToChannel(Channel channel) {
        channel.addShutdownListener(shutdownListener);
    }

    private void closeChannel(Channel channel) {
        
        if (!channel.isOpen()) {
            return;
        }

        try {
            channel.close();
        } catch (Exception e) {
            LOG.log(Level.INFO, "Failed to close publisher channel", e);
        }
    }

    private String getId() {

        if (id != null) {
            return id;
        }

        id = generateRandomId();
        return id;
    }
}
