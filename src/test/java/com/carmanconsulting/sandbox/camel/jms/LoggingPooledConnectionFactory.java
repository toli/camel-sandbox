package com.carmanconsulting.sandbox.camel.jms;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.jms.pool.ConnectionKey;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.JMSException;

public class LoggingPooledConnectionFactory extends PooledConnectionFactory {
//----------------------------------------------------------------------------------------------------------------------
// Fields
//----------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingPooledConnectionFactory.class);

//----------------------------------------------------------------------------------------------------------------------
// Constructors
//----------------------------------------------------------------------------------------------------------------------

    public LoggingPooledConnectionFactory() {
    }

    public LoggingPooledConnectionFactory(ActiveMQConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

//----------------------------------------------------------------------------------------------------------------------
// Other Methods
//----------------------------------------------------------------------------------------------------------------------

    @Override
    protected Connection createConnection(ConnectionKey key) throws JMSException {
        LOGGER.info("Creating new pooled connection...");
        Connection connection = super.createConnection(key);
        LOGGER.info("Pool size now {}", getNumConnections() + 1);
        return connection;
    }
}
