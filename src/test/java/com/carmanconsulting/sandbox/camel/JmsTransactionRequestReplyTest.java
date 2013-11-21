package com.carmanconsulting.sandbox.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.SimpleRegistry;
import org.junit.Test;
import org.springframework.jms.connection.JmsTransactionManager;

public class JmsTransactionRequestReplyTest extends JmsTestCase
{
    @Produce(uri="direct:hello")
    private ProducerTemplate producerTemplate;

    @Override
    protected void doBindings(SimpleRegistry registry)
    {
        registry.put("transactionManager", new JmsTransactionManager(getConnectionFactory()));
    }

    @Test
    public void testRequestReply()
    {
        assertEquals("Hello, Camel!", producerTemplate.requestBody("Camel"));
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception
    {
        return new RouteBuilder()
        {
            @Override
            public void configure() throws Exception
            {
                from("direct:hello").to("jms:queue:foo?transactedInOut=true");
                from("jms:queue:foo?transactedInOut=true").process(new Processor()
                {
                    @Override
                    public void process(Exchange exchange) throws Exception
                    {
                        exchange.getOut().setBody("Hello, "+ exchange.getIn().getBody(String.class) + "!");
                    }
                });
            }
        };
    }
}
