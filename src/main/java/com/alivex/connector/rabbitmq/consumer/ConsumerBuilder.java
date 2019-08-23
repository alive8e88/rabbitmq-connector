
package com.alivex.connector.rabbitmq.consumer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.alivex.connector.rabbitmq.ConnectionManager;
import java.io.IOException;
import java.io.Serializable;

public class ConsumerBuilder implements Serializable {

    private final boolean AUTO_ACK = false;
    private String queueName;
    private int qos = 1;
    private Processor processor;
    private ConnectionManager connectionManager;
    private String consumerTag;

    public ConsumerBuilder queueName(String name) {
        this.queueName = name;
        return this;
    }

    public ConsumerBuilder qos(int qos) {
        this.qos = qos;
        return this;
    }

    public ConsumerBuilder processor(Processor p) {
        this.processor = p;
        return this;
    }
    
    public ConsumerBuilder connectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        return this;
    }

    public ConsumerBuilder consumerTag(String tag) {
        this.consumerTag = tag;
        return this;
    }

    public Consumer build() throws IOException {

        Channel channel = initChannel();
        Consumer consumer = new DefaultConsumer(channel, processor);

        if (isConsumerTagAvailable()) {
            channel.basicConsume(queueName, AUTO_ACK, consumerTag, consumer);
        } else {
            channel.basicConsume(queueName, AUTO_ACK, consumer);
        }

        return consumer;
    }

    private Channel initChannel() throws IOException{

        Connection connection = connectionManager.getConsumerConnection();
        Channel channel = connection.createChannel();
        channel.basicQos(qos);

        //Invoke connection proxy object to init connection id,
        //fix connection id null when reference from default consumer.
        connection.getId();

        return channel;
    }

    private boolean isConsumerTagAvailable() {
        return consumerTag != null;
    }
}
