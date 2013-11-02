package com.carmanconsulting.sandbox.camel;

import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Test;

/**
 * @see <a href="https://issues.apache.org/jira/browse/CAMEL-6927">CAMEL-6927</a>
 */
public class BeanTransformTest extends JmsTestCase
{

    @Produce(uri="direct:input")
    private ProducerTemplate producerTemplate;

    @Test
    public void testAppending() throws Exception
    {
        MockEndpoint output = getMockEndpoint("mock:output");
        output.expectedBodiesReceived("test-foo");
        producerTemplate.sendBody("test");
        output.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception
    {
        return new RouteBuilder()
        {
            @Override
            public void configure() throws Exception
            {
                from("direct:input").bean(FooAppender.class, "appendTimestamp").to("mock:output");
            }
        };
    }

    public static class FooAppender
    {
        public String appendTimestamp(String body)
        {
            return body + "-foo";
        }
    }
}
