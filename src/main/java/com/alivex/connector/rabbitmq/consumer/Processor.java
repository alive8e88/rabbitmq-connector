package com.alivex.connector.rabbitmq.consumer;


public interface Processor {

    /**
     * Process array of byte received from consumer.
     * Throw any runtime exception will result in automatic nack and close channel.
     * @param data
     */
    public void process(byte[] data) throws RuntimeException;
}
