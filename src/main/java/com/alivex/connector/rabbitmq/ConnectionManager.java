package com.alivex.connector.rabbitmq;

import com.rabbitmq.client.Connection;
import java.io.Serializable;


/**
 * Reusable connection class.
 * @author development
 */
public abstract class ConnectionManager implements Serializable {

    public abstract Connection getPublisherConnection();
    public abstract Connection getConsumerConnection();
}
