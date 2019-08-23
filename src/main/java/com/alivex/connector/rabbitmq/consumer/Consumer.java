package com.alivex.connector.rabbitmq.consumer;

public interface Consumer {

    public String getId();
    public boolean isChannelOpen();
    public void closeChannel();
}
