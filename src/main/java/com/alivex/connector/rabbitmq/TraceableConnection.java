
package com.alivex.connector.rabbitmq;

import com.rabbitmq.client.Connection;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class use JDK dynamic proxy for connection interface
 */
public class TraceableConnection implements InvocationHandler, Serializable  {

    private static final Logger LOG = Logger.getLogger(TraceableConnection.class.getName());

    private Object original;

    public TraceableConnection(Object original) {
        this.original = original;
    }

    /**
     * Interceptor for getId() on com.rabbitmq.client.Connection,
     * This method will automatic set id to connection object if current value is null.
     * Prevent null of id after auto-recovery connection feature trigger.
     * @param proxy
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        if (isInvokeGetId(method)) {

            if (LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, "Proxy invoked, method:{0}", method.getName());
            }
            
            Object result = method.invoke(original, args);
            if (result == null) {

                String id = UUID.randomUUID().toString();
                ((Connection)original).setId(id);
                return id;
            }
        }

        return method.invoke(original, args);
    }

    private boolean isInvokeGetId(Method m) {
        return m.getName().equals("getId");
    }
    
    @Override
    public String toString() {
        return "TraceableConnection";
    }
}
