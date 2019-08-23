





# Basic Usage

### Establish a connection
ConnectionManager is a central abstract to create a Rabbitmq connection. You need to subclass and specify the connection details you want for both publush and consumer side. Generally, you only need one instance across an application. 

You can provide connection details to the factory through setter method, see **initConnectionFactory( )** method. Server node list can provide to connection by a list of address, see **initConnection( )** method
```java
@Singleton
public class RabbitConnectionManager extends ConnectionManager {
    private static final Logger LOG = Logger.getLogger(RabbitConnectionManager.class.getName());
    private ConnectionFactory factory;
    private Connection publisherConnection;
    private Connection consumerConnection;

    @PostConstruct
    public void init() {
        try {
            initConnectionFactory();
            initPublisherConnection();
            initConsumerConnection();
            LOG.log(Level.INFO, "Connection manager successfully intialized");

        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(RabbitConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (KeyManagementException ex) {
            Logger.getLogger(RabbitConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(RabbitConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TimeoutException ex) {
            Logger.getLogger(RabbitConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @PreDestroy
    public void destroy() {
        try {
            publisherConnection.close();
            LOG.log(Level.INFO, "Successfully to close cacheable publisher connection.");
            
            consumerConnection.close();
            LOG.log(Level.INFO, "Successfully to close cacheable consumer connection.");
        } catch (IOException e) {
            LOG.log(Level.INFO, "Failed to close Rabbit MQ connection", e);
        }
    }

    private void initConnectionFactory() throws KeyManagementException, NoSuchAlgorithmException {

        factory = TraceableConnectionFactory.newAutomaticRecoveryFactoryInstance();
        factory.setPort(5671);
        factory.useSslProtocol();
        factory.setUsername("username");
        factory.setPassword("password");
    }

    private void initPublisherConnection() throws IOException, TimeoutException {
        publisherConnection = initConnection();
        LOG.log(Level.INFO, "Create cache connection for publisher, connection id:{0}", publisherConnection.getId());
    }

    private void initConsumerConnection() throws IOException, TimeoutException {
        consumerConnection = initConnection();
        LOG.log(Level.INFO, "Create cache connection for consumer, connection id:{0}", consumerConnection.getId());
    }

    private Connection initConnection() throws IOException, TimeoutException {
        return factory.newConnection(Arrays.asList(new Address("node1.domain.com"), new Address("node2.domain.com")));
    }

    @Override
    public Connection getConsumerConnection() {
        return consumerConnection;
    }

    @Override
    public Connection getPublisherConnection() {
        return publisherConnection;
    }
}
```
Or you can simplify by using an uri to create a connection.  Keep in mind the host in uri should reflect to load balance to all node in a cluster, see **initConnectionFactory( )** method.
```java
@Singleton
public class RabbitConnectionManager extends ConnectionManager {
    private static final Logger LOG = Logger.getLogger(RabbitConnectionManager.class.getName());
    private ConnectionFactory factory;
    private Connection publisherConnection;
    private Connection consumerConnection;
    
    @PostConstruct
    public void init() {

        try {
            initConnectionFactory();
            initPublisherConnection();
            initConsumerConnection();
            LOG.log(Level.INFO, "Connection manager successfully intialized");
        } catch (Exception ex) {
            Logger.getLogger(RabbitConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @PreDestroy
    public void destroy() {
        try {
            publisherConnection.close();
            LOG.log(Level.INFO, "Successfully to close cacheable publisher connection.");
            
            consumerConnection.close();
            LOG.log(Level.INFO, "Successfully to close cacheable consumer connection.");
        } catch (IOException e) {
            LOG.log(Level.INFO, "Failed to close Rabbit MQ connection", e);
        }
    }

    private void initConnectionFactory() throws KeyManagementException, NoSuchAlgorithmException, URISyntaxException {

        factory = TraceableConnectionFactory.newAutomaticRecoveryFactoryInstance();
        factory.setUri("amqp://username:password@load-balance.domain.com/vhost");
    }

    private void initPublisherConnection() throws IOException, TimeoutException {

        publisherConnection = initConnection();
        LOG.log(Level.INFO, "Create cache connection for publisher, connection id:{0}", publisherConnection.getId());
    }

    private void initConsumerConnection() throws IOException, TimeoutException {

        consumerConnection = initConnection();
        LOG.log(Level.INFO, "Create cache connection for consumer, connection id:{0}", consumerConnection.getId());
    }

    private Connection initConnection() throws IOException, TimeoutException {
        return factory.newConnection();
    }

    @Override
    public Connection getConsumerConnection() {
        return consumerConnection;
    }

    @Override
    public Connection getPublisherConnection() {
        return publisherConnection;
    }
}
```

### Create publisher
PublisherBuilder allows you to simply create basic publisher in a few lines of code. This example below show a minimum requirement to create a basic publisher.
```java
public class PublisherConfiguration {
    private static final Logger LOG = Logger.getLogger(PublisherConfiguration.class.getName());
    
    @Produces
    public Publisher createPublisher(ConnectionManager connectionManager) {
        LOG.log(Level.INFO, "Config publisher to use default listener");
        return new PublisherBuilder()
                .exchange("x.name")
                .routingKey("q.name")
                .connectionManager(connectionManager)
                .build();
    }
}
```

You can register you own **confirm**, **return** and **shutdown** listener to listening a broker event in details through a **PublisherBuilder**. see **Concurrent control in publisher** section before start to implement your own listener. This example below show a simple custom implementation.
```java
public class PublisherConfiguration {
    private static final Logger LOG = Logger.getLogger(PublisherConfiguration.class.getName());

    @Produces
    public Publisher createPublisherFullCustomize(ConnectionManager connectionManager) {
        LOG.log(Level.INFO, "Config publisher to use customize listener");
        return new PublisherBuilder()
                .exchange("x.name")
                .routingKey("q.name")
                .connectionManager(connectionManager)
                .confirmListener(new AlternativeConfirmListener())
                .returnListener(new AlternativeReturnListener())
                .shutdownListener(new AlternativeShutdownListener())
                .build();
    }

    public class AlternativeConfirmListener implements ConfirmListener {

        @Override
        public void handleAck(long arg0, boolean arg1) throws IOException {
            System.out.println("Alternative confirm listener invoke.");
        }

        @Override
        public void handleNack(long arg0, boolean arg1) throws IOException {
            System.out.println("Alternative confirm listener invoke.");
        }
    }

    public class AlternativeReturnListener implements ReturnListener {

        @Override
        public void handleReturn(int arg0, String arg1, String arg2, String arg3, AMQP.BasicProperties arg4, byte[] arg5) throws IOException {
            System.out.println("Alternative return listener invoke.");
        }
    }

    public class AlternativeShutdownListener implements ShutdownListener {

        @Override
        public void shutdownCompleted(ShutdownSignalException arg0) {
            System.out.println("Alternative shutdown listener invoke.");
        }
    }
}
```

### Process a message
To separate concern between amqp consumer logic and business logic, a **Processor** interface allows you to abstract business logic from amqp consumer logic. You can implement business logic by implement this interface. This example only prints a message body to the console log.
```java
public class PrintMessageProcessor implements Processor {

    @Override
    public void process(byte[] payload) {
        try {
            String data = new String(payload, StandardCharsets.UTF_8.name());
            System.out.println("process data: " + data);
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalArgumentException("Failed to conver message to string", ex);
        }
    }
}
```

### Create a consumer
Listen to a message from a broker you need to create a consumer. Use **ConsumerBuilder** to create a consumer for a specific queue. This example below create a consumer and wiring **PrintMessageProcessor ** to handle business logic with explicit declare a [channel prefetch](https://www.rabbitmq.com/confirms.html#channel-qos-prefetch) to 1(if you don't specify default is 1). Generally, message consumer should start when the application starts. For this reason, you can use **@Startup** to instruct an application container to create a bean at startup time.

```java
.
.
.
import com.alivex.connector.rabbitmq.consumer.Consumer;

@Startup
@Singleton
public class ConsumerConfiguration {
    private static final Logger LOG = Logger.getLogger(ConsumerConfiguration.class.getName());
    private final List<Consumer> consumers = new ArrayList<>();
    @Inject
    private ConnectionManager connectionManager;
    @Inject
    private PrintMessageProcessor printMessageProcessor;

    @PostConstruct
    public void init() {
        try {
        
            Consumer consumer = (Consumer) new ConsumerBuilder()
                    .queueName(QNAME)
                    .qos(1)
                    .consumerTag(generateRandomConsumerTag())
                    .processor(printMessageProcessor)
                    .connectionManager(connectionManager)
                    .build();

            consumers.add(consumer);
            LOG.log(Level.INFO, "Create consumer id:{0} for:{1}, processor id:{2}", new Object[]{consumer.getId(), QNAME, printMessageProcessor.getId()});

        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    @PreDestroy
    public void preDestroy() {

        if (consumers == null) {
            return;
        }

        for (Consumer consumer : consumers) {
            try {
                consumer.closeChannel();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to close consumer channel id:" + consumer.getId(), e);
            }
        }
    }
    
    private String generateRandomConsumerTag() {
        final String appName = "sample-rabbitmq";
        return appName + "-" + UUID.randomUUID().toString();
    }
}

```
### Reject and Don't Requeue
By default, when exception happens in your processor a current message will be automatic re-queue. If you don't want to re-queue a current message your processor should throw a special exception to tell a consumer to reject and don't re-queue a message. This example show a bit different of previous **PrintMessageProcessor** by throws a  **RejectAndDontRequeueException**

```java
.
.
.
import com.alivex.connector.rabbitmq.exception.RejectAndDontRequeueException;

public class PrintMessageProcessor implements Processor {

    @Override
    public void process(byte[] payload) throws RejectAndDontRequeueException {
        try {
            String data = new String(payload, StandardCharsets.UTF_8.name());
            System.out.println("process data: " + data);
        } catch (UnsupportedEncodingException ex) {
            throw new RejectAndDontRequeueException("Failed to conver message to string", ex);
        }
    }
}
```


## Advance Topic
### Concurrent control in Publisher
By default, if confirm, return and shutdown listener not specify when creating a  publisher by  **PublisherBuilder**, a builder will assign a default listener to publisher instance create by them.
A default listener is thread-safety out of the box. But in case you need to specify your own implementation, I recommend to make sure your listener is thread-safety too. Otherwise, don't reuse a **PublisherBuilder** to create a bulk of publisher to reduce a chance of thread lock.

### Create publisher pool
Sometimes you need to send a huge collection of a message. You can create a publisher pool and re-use a pool to sending a message in parallel. an example below use [fast-object-pool](https://github.com/DanielYWoo/fast-object-pool) to create a pool of publisher.
```java
//Publisher pool class
@Startup
@Singleton
public class PublisherPool {

    private static final Logger LOG = Logger.getLogger(PublisherPool .class.getName());

    @Inject
    private ConnectionManager connectionManager;
    
    private volatile ObjectPool pool;

    public PublisherPool () {
    }

    @PostConstruct
    public void initPool() {

        //Create pool config. min = 2 * 4, max = 8 * 4
        PoolConfig config = new PoolConfig();
        config.setMinSize(2);
        config.setMaxSize(8);
        config.setPartitionSize(4);
        config.setMaxIdleMilliseconds(60 * 1000 * 5);

        PublisherBuilder publisherBuilder = new PublisherBuilder()
                .exchange("x.name")
                .routingKey("q.name")
                .connectionManager(connectionManager);
        
        PublisherBuilder publisherBuilder = getPublisherBuilder();
        PublisherPoolFactory publisherFactoryAdapter = new PublisherPoolFactory (publisherBuilder);
        
        pool = new ObjectPool(config, publisherFactoryAdapter);
    }

    @PreDestroy
    public void destroy() {
        try {

            pool.shutdown();

        } catch (InterruptedException e) {
            LOG.log(Level.WARNING, "Failed to destroy rabbit mq publisher pool", e);
        }
    }

    public ObjectPool getPool() {
        return pool;
    }
}

/*
 Publisher pool factory, adapter between PublisherBuilder and ObjectFactory in fast object pool
*/
public class PublisherPoolFactory implements ObjectFactory<Publisher> {

    private static final Logger LOG = Logger.getLogger(PublisherPoolFactory.class.getName());

    private PublisherBuilder builder;

    public PublisherPoolFactory (PublisherBuilder builder) {
        this.builder = builder;
    }

    @Override
    public Publisher create() {

        LOG.log(Level.FINE, "Create publisher pool");
        return builder.build();
    }

    @Override
    public void destroy(Publisher t) {
        t.closeChannel();
    }

    @Override
    public boolean validate(Publisher t) {
        return t.isChannelOpen();
    }
}
```
## Concurrent control in Consumer
If a processor class is thread-safe, you are safe to re-use instance to handle a message concurrently. This means you are allowed to increase qos value in consumer setting without side effect. The following basic example will receive a maximum of 10 unacknowledged messages at once, and every message will use the same instance of a processor to process a different message in concurrent.
```java
@Startup
@Singleton
public class ConsumerConfiguration {
    private static final Logger LOG = Logger.getLogger(ConsumerConfiguration.class.getName());
    private final List<Consumer> consumers = new ArrayList<>();
    @Inject
    private ConnectionManager connectionManager;
    @Inject
    private PrintMessageProcessor printMessageProcessor;

    @PostConstruct
    public void init() {
        try {
        
            Consumer consumer = (Consumer) new ConsumerBuilder()
                    .queueName(QNAME)
                    .qos(10)
                    .consumerTag(generateRandomConsumerTag())
                    .processor(printMessageProcessor)
                    .connectionManager(connectionManager)
                    .build();

            consumers.add(consumer);
            LOG.log(Level.INFO, "Create consumer id:{0} for:{1}, processor id:{2}", new Object[]{consumer.getId(), QNAME, printMessageProcessor.getId()});

        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }
.
.
.
}
```
> Note:
If you cannot guarantee a thread-safety in a processor class, but you want to increase the number of processing message in concurrent. The simplest approach is a separate consumer with a separate processor instance. And setting qos value of each consumer to 1. For more details in concurrency of RabbitMQ should visit [https://www.rabbitmq.com/api-guide.html#concurrency](https://www.rabbitmq.com/api-guide.html#concurrency)


### Logging
All package in this module are cover by **com.alivex.connector.rabbitmq** allow you to easily manage your logging level. To enable tracing log of internal listener, set logging level within logging properties to **FINE** or **FINER**
```properties
com.alivex.connector.rabbitmq.level=FINER
```
Or you can enble only specific package.
```properties
com.alivex.connector.rabbitmq.level=FINER
com.alivex.connector.rabbitmq.publisher.level=INFO
com.alivex.connector.rabbitmq.consumer.level=FINER
```