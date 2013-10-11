package com.carmanconsulting.sandbox.camel;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.component.jms.JmsComponent;

import javax.jms.ConnectionFactory;

public abstract class JmsTestCase extends CamelTestCase
{
//----------------------------------------------------------------------------------------------------------------------
// Other Methods
//----------------------------------------------------------------------------------------------------------------------

    protected ConnectionFactory createConnectionFactory()
    {
        return new ActiveMQConnectionFactory(String.format("vm://%s", getClass().getSimpleName()));
    }

    @Override
    protected void initializeCamelContext(CamelContext context)
    {
        JmsComponent jms = new JmsComponent(context);
        jms.setConnectionFactory(createConnectionFactory());
        context.addComponent("jms", jms);
    }
}
