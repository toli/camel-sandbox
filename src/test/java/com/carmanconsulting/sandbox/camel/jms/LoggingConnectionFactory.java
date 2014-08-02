package com.carmanconsulting.sandbox.camel.jms;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import java.util.concurrent.atomic.AtomicInteger;

public class LoggingConnectionFactory extends ActiveMQConnectionFactory {
//----------------------------------------------------------------------------------------------------------------------
// Fields
//----------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingConnectionFactory.class);

    private AtomicInteger count = new AtomicInteger();

//----------------------------------------------------------------------------------------------------------------------
// Constructors
//----------------------------------------------------------------------------------------------------------------------

    public LoggingConnectionFactory(String brokerURL) {
        super(brokerURL);
    }

//----------------------------------------------------------------------------------------------------------------------
// ConnectionFactory Implementation
//----------------------------------------------------------------------------------------------------------------------

    @Override
    public Connection createConnection() throws JMSException {
        LOGGER.info("createConnection()");
        final Connection connection = super.createConnection();
        LOGGER.info("Returning connection {}.", count.incrementAndGet());
        return connection;
    }

    @Override
    public Connection createConnection(String userName, String password) throws JMSException {
        LOGGER.info("createConnection({}, {})", userName, password);
        final Connection connection = super.createConnection(userName, password);
        LOGGER.info("Returning connection {}.", count.incrementAndGet());
        return connection;
    }
}
