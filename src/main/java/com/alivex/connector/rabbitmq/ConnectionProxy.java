
package com.alivex.connector.rabbitmq;

import com.rabbitmq.client.Connection;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ConnectionProxy implements InvocationHandler {

    private static final Logger LOG = Logger.getLogger(ConnectionProxy.class.getName());

    private Object original;

    public ConnectionProxy(Object original) {
        this.original = original;
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        if (isInvokeGetId(method)) {

            LOG.log(Level.FINER, "Proxy invoked, method:{0}", method.getName());
            Object result = method.invoke(original, args);
            if (result == null) {

                String id = UUID.randomUUID().toString();
                ((Connection) original).setId(id);
                return id;
            }
        }

        return method.invoke(original, args);
    }

    @Override
    public String toString() {
        return "ConnectionProxy";
    }

    private boolean isInvokeGetId(Method m) {
        return m.getName().equals("getId");
    }
}
