
package com.alivex.connector.rabbitmq;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ReturnListener;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * require mandatory flag
 * @author development
 */
public class DefaultReturnListener implements ReturnListener, ConnectionIdConfigurer, OperationIdConfigurer, Serializable {

    private static final Logger LOG = Logger.getLogger(DefaultReturnListener.class.getName());
    private String connectionId;
    private String operationId;

    public DefaultReturnListener() {
    }
    
    /**
     * Handler method for return operation.
     * @param arg0 = reply code
     * @param arg1 = reply text
     * @param arg2 = exchange name
     * @param arg3 = routing key
     * @param arg4 = message properties
     * @param arg5 = message payload
     * @throws IOException
     */
    @Override
    public void handleReturn(int arg0, String arg1, String arg2, String arg3, AMQP.BasicProperties arg4, byte[] arg5) throws IOException {

        if (LOG.isLoggable(Level.FINE)) {

            LOG.log(Level.FINE, "Return listerner invoke.");
            LOG.log(Level.FINE, "ConnectionId:{0}", connectionId);
            LOG.log(Level.FINE, "OperationId:{0}", operationId);
            LOG.log(Level.FINE, "ReplyCode:{0}", arg0);
            LOG.log(Level.FINE, "ReplyText:{0}", arg1);
            LOG.log(Level.FINE, "ExchangeName:{0}", arg2);
            LOG.log(Level.FINE, "RountingKey:{0}", arg3);
            LOG.log(Level.FINE, "Properties:{0}", arg4);
            LOG.log(Level.FINE, "Payload:{0}", new String(arg5, StandardCharsets.UTF_8.name()));
        }
    }


    @Override
    public void setConnectionId(String id) {
        this.connectionId = id;
    }

    @Override
    public void setOperationId(String id) {
        this.operationId = id;
    }
}
