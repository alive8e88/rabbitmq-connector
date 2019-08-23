
package com.alivex.connector.rabbitmq;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;


/**
 * This class use cglib proxy for connection factory object,
 * Do not declare subclass as final class cglib cannot be proxy a final class.
 */
public class TraceableConnectionFactory implements MethodInterceptor, Serializable {

    private static final Logger LOG = Logger.getLogger(TraceableConnectionFactory.class.getName());

    private TraceableConnectionFactory() {

    }

    /**
     * Factory method, used to create proxy object of com.rabbitmq.client.ConnectionFactory.
     * This proxy object add ability to trace connection id when create connection object from this factory.
     * @return
     */
    public static ConnectionFactory newFactoryInstance() {

        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(ConnectionFactory.class);
        enhancer.setCallback(new TraceableConnectionFactory());
        return (ConnectionFactory) enhancer.create();
    }


    /**
     * Factory method, used to create proxy object of com.rabbitmq.client.ConnectionFactory.
     * This proxy object add ability to trace connection id when create connection object from this factory.
     * And set default auto-recovery.
     * @return
     */
    public static ConnectionFactory newAutomaticRecoveryFactoryInstance() {

        ConnectionFactory factory = newFactoryInstance();
        factory.setRequestedHeartbeat(3);
        factory.setConnectionTimeout(5000);
        factory.setAutomaticRecoveryEnabled(true);
        factory.setTopologyRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(3000);
        return factory;
    }


    /**
     * Interceptor method for newConnection() on com.rabbitmq.client.ConnectionFactory.
     * This method create proxy object of com.rabbitmq.client.Connection,
     * used to enhance getId() on original connection object.
     * @param o
     * @param method
     * @param args
     * @param proxy
     * @return
     * @throws Throwable
     */
    @Override
    public Object intercept(Object o, Method method, Object[] args, MethodProxy proxy) throws Throwable {

        if (!isInvokeNewConnection(method)) {
            return proxy.invokeSuper(o, args);
        }

        LOG.log(Level.FINER, "Intecept create connection from traceable connection factory.");
        Object newConnection = proxy.invokeSuper(o, args);
        //Create jdk dynamic proxy for connection interface.
        return Proxy.newProxyInstance(TraceableConnection.class.getClassLoader(),
                                      new Class[]{Connection.class},
                                      new TraceableConnection(newConnection));
    }

    private boolean isInvokeNewConnection(Method method) {
        return method.getDeclaringClass() != Object.class && method.getReturnType() == Connection.class;
    }
}
