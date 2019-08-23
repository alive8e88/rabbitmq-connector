package com.alivex.connector.rabbitmq.publisher;


/**
 * Publish json data interface
 * @author development
 */
public interface Publisher {

    public boolean basicPublish(String json);
    public boolean basicPublish(String json, int timeout);
    public boolean isChannelOpen();
    public void closeChannel();
}
