
package com.alivex.connector.rabbitmq.consumer;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.alivex.connector.rabbitmq.exception.RejectAndDontRequeueException;
import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


public class DefaultConsumer extends com.rabbitmq.client.DefaultConsumer implements Consumer, Serializable {

    private static final Logger LOG = Logger.getLogger(DefaultConsumer.class.getName());
    private final String id;
    private final Channel channel;
    private final Processor processor;
    
    public DefaultConsumer(Channel channel, Processor processor) {
        super(channel);
        this.channel = channel;
        this.processor = processor;
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {

        try {

            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Consumer received message, connection:{0}, channel:{1}", new Object[]{getConnectionId(), getChannelId()});
            }

            processor.process(body);
            this.channel.basicAck(envelope.getDeliveryTag(), false);

        } catch (RejectAndDontRequeueException e) {

            this.channel.basicReject(envelope.getDeliveryTag(), false);
            LOG.log(Level.SEVERE, "Failed to consume message.", e);
            
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to consume message.", e);
        }
    }

    public String getChannelId() {
        return String.valueOf(channel.hashCode());
    }

    public String getConnectionId() {
        return channel.getConnection().getId();
    }

    @Override
    public boolean isChannelOpen() {
        return channel != null && channel.isOpen();
    }

    @Override
    public void closeChannel() {

        closeChannel(channel);
        LOG.log(Level.INFO, "Consumer id:{0} is successfully close", id);
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

    @Override
    public String getId() {
        return id;
    }
}
