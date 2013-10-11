package com.carmanconsulting.sandbox.camel;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class JmsTopicTest extends JmsTestCase
{

    @EndpointInject(uri="mock:consumerA")
    private MockEndpoint consumerA;

    @EndpointInject(uri="mock:consumerB")
    private MockEndpoint consumerB;

    @Produce(uri="direct:event")
    private ProducerTemplate producer;

    @Test
    public void testMulticastViaTopic() throws Exception
    {
        consumerA.expectedMessageCount(1);
        consumerB.expectedMessageCount(1);
        producer.sendBody("Hello");
        assertMockEndpointsSatisfied(3, TimeUnit.SECONDS);
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception
    {
        return new RouteBuilder()
        {
            @Override
            public void configure() throws Exception
            {
                from("direct:event").to("jms:topic:event");
                from("jms:topic:event").to("mock:consumerA");
                from("jms:topic:event").to("mock:consumerB");
            }
        };
    }
}
