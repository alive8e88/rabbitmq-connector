
package com.alivex.connector.rabbitmq.publisher;

import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.ReturnListener;
import com.rabbitmq.client.ShutdownListener;
import com.alivex.connector.rabbitmq.ConnectionManager;
import com.alivex.connector.rabbitmq.DefaultConfirmListener;
import com.alivex.connector.rabbitmq.DefaultReturnListener;
import com.alivex.connector.rabbitmq.DefaultShutdownListener;
import java.io.Serializable;


public class PublisherBuilder implements Serializable {

    private String exchange;
    private String routingKey;
    private ConfirmListener confirmListener;
    private ReturnListener returnListener;
    private ShutdownListener shutdownListener;
    private ConnectionManager connectionManager;

    public PublisherBuilder() {

    }

    public PublisherBuilder exchange(String exchange) {
        this.exchange = exchange;
        return this;
    }

    public PublisherBuilder routingKey(String key) {
        this.routingKey = key;
        return this;
    }

    /**
     * Add confirm listener to publisher,
     * If not specify default listener will be use. Default listener are thread-safety out of the box.
     * If you provide your own, a listener will be share to any publisher build from this builder.
     * Please make sure a listener is thread safety or simply don't reuse builder just create a new builder every time.
     * @param confirmListener
     * @return
     */
    public PublisherBuilder confirmListener(ConfirmListener confirmListener) {
        this.confirmListener = confirmListener;
        return this;
    }

    /**
     * Add return listener to publisher,
     * If not specify default listener will be use. Default listener are thread-safety out of the box.
     * If you provide your own, a listener will be share to any publisher build from this builder.
     * Please make sure a listener is thread safety or simply don't reuse builder just create a new builder every time.
     * @param returnListener
     * @return
     */
    public PublisherBuilder returnListener(ReturnListener returnListener) {
        this.returnListener = returnListener;
        return this;
    }

    /**
     * Add shutdown listener to publisher,
     * If not specify default listener will be use. Default listener are thread-safety out of the box.
     * If you provide your own, a listener will be share to any publisher build from this builder.
     * Please make sure a listener is thread safety or simply don't reuse builder just create a new builder every time.
     * @param shutdownListener
     * @return
     */
    public PublisherBuilder shutdownListener(ShutdownListener shutdownListener) {
        this.shutdownListener = shutdownListener;
        return this;
    }
    
    public PublisherBuilder connectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        return this;
    }
    
    public Publisher build() {

        //If not specify custom listener, default lister will be assign by create new instance every time this method invoke for thread-safety.
        ShutdownListener shutdownListenerToUse = shutdownListener == null ? new DefaultShutdownListener() : shutdownListener;
        ConfirmListener confirmListenerToUse = confirmListener == null ? new DefaultConfirmListener(): confirmListener;
        ReturnListener returnListenerToUse = returnListener == null ? new DefaultReturnListener() : returnListener;
        
        return new DefaultPublisher(exchange, 
                                    routingKey,
                                    connectionManager,
                                    shutdownListenerToUse,
                                    returnListenerToUse,
                                    confirmListenerToUse);
    }
}
